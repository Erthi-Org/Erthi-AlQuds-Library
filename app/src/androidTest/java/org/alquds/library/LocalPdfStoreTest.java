package org.alquds.library;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class LocalPdfStoreTest {
    private Context context;
    private String name;
    private LocalPdfStore store;
    private File directory;

    @Before public void createIsolatedStore() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        name = "pdf-test-" + UUID.randomUUID();
        directory = new File(context.getNoBackupFilesDir(), name);
        store = new LocalPdfStore(context, name + ".db", name);
    }

    @After public void cleanup() {
        store.close(); context.deleteDatabase(name + ".db");
        File[] files = directory.listFiles();
        if (files != null) for (File file : files) assertTrue(file.delete());
        if (directory.exists()) assertTrue(directory.delete());
    }

    @Test public void importValidatesPdfAndPreservesReadingPositionAfterReopen() throws Exception {
        LocalPdfStore.Document document = store.importPdf(new ByteArrayInputStream(PdfFixtures.twoPages()), "كتاب شخصي.pdf");
        assertEquals(2, document.pages); assertEquals("كتاب شخصي.pdf", document.title);
        assertTrue(store.file(document.id).isFile());
        store.savePage(document.id, 1); store.close();
        store = new LocalPdfStore(context, name + ".db", name);
        assertEquals(1, store.get(document.id).lastPage);
        assertEquals(1, store.list().size());
        try { store.savePage(document.id, 2); fail("Out-of-range page accepted"); }
        catch (IOException expected) { assertEquals(1, store.get(document.id).lastPage); }
    }

    @Test public void rejectsInvalidAndTruncatedFilesWithoutAddingRecords() throws Exception {
        for (byte[] bytes : new byte[][]{new byte[0], "not a PDF".getBytes(java.nio.charset.StandardCharsets.UTF_8), "%PDF-broken".getBytes(java.nio.charset.StandardCharsets.UTF_8)}) {
            try { store.importPdf(new ByteArrayInputStream(bytes), "document.pdf"); fail("Invalid PDF accepted"); }
            catch (IOException | RuntimeException expected) { assertTrue(store.list().isEmpty()); }
            assertEquals(0, directory.listFiles().length);
        }
    }

    @Test public void streamSizeLimitDoesNotTrustProviderMetadata() throws Exception {
        InputStream endless = new InputStream() {
            @Override public int read() { return 'x'; }
            @Override public int read(byte[] bytes, int offset, int length) {
                java.util.Arrays.fill(bytes, offset, offset + length, (byte) 'x'); return length;
            }
        };
        try { store.importPdf(endless, "oversized.pdf"); fail("Oversized input accepted"); }
        catch (IOException expected) { assertTrue(store.list().isEmpty()); }
        assertEquals(0, directory.listFiles().length);
    }

    @Test public void identifiersCannotEscapePrivateStorage() throws Exception {
        for (String id : new String[]{"../other", "/sdcard/book.pdf", "", "not-a-uuid"}) {
            try { store.file(id); fail("Unsafe identifier accepted"); }
            catch (IOException expected) { /* Rejected before resolving a path. */ }
        }
    }

    @Test public void deleteRemovesOnlyTheImportedCopy() throws Exception {
        byte[] original = PdfFixtures.twoPages();
        File source = new File(context.getCacheDir(), name + ".pdf");
        try {
            try (java.io.FileOutputStream output = new java.io.FileOutputStream(source)) { output.write(original); }
            LocalPdfStore.Document document;
            try (InputStream input = new java.io.FileInputStream(source)) { document = store.importPdf(input, "../../original.pdf"); }
            assertEquals(1, directory.listFiles().length);
            store.delete(document.id);
            assertTrue(store.list().isEmpty()); assertFalse(store.file(document.id).exists());
            assertEquals(original.length, source.length());
        } finally { assertTrue(source.delete()); }
    }
}
