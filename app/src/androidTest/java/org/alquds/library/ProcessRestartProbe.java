package org.alquds.library;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Dedicated emulator only: run via tests/run_process_restart.py, never as a normal suite. */
@RunWith(AndroidJUnit4.class)
public class ProcessRestartProbe {
    private Context context() { return InstrumentationRegistry.getInstrumentation().getTargetContext(); }

    @Test public void prepare() throws Exception {
        SharedPreferences settings = context().getSharedPreferences("library", Context.MODE_PRIVATE);
        SharedPreferences marker = context().getSharedPreferences("process-restart-probe", Context.MODE_PRIVATE);
        assertFalse("Unfinished probe; use a clean test emulator", marker.getBoolean("ready", false));
        long bookId;
        boolean originalFavorite;
        try (CatalogDatabase database = new CatalogDatabase(context())) {
            Book book = database.search("").get(0);
            bookId = book.id;
            originalFavorite = book.favorite;
        }
        assertTrue(marker.edit().putBoolean("hadTheme", settings.contains("dark"))
                .putBoolean("theme", settings.getBoolean("dark", false))
                .putBoolean("favorite", originalFavorite)
                .putLong("bookId", bookId).putInt("pid", android.os.Process.myPid())
                .putBoolean("ready", true).commit());
        assertTrue(settings.edit().putBoolean("dark", true).commit());
        try (CatalogDatabase database = new CatalogDatabase(context())) { database.setFavorite(bookId, true); }
        try (LocalPdfStore store = new LocalPdfStore(context())) {
            LocalPdfStore.Document document = store.importPdf(new java.io.ByteArrayInputStream(PdfFixtures.twoPages()), "اختبار استمرار الكتاب.pdf");
            store.savePage(document.id, 1);
            assertTrue(marker.edit().putString("pdf", document.id).commit());
        }
    }

    @Test public void verify() throws Exception {
        SharedPreferences settings = context().getSharedPreferences("library", Context.MODE_PRIVATE);
        SharedPreferences marker = context().getSharedPreferences("process-restart-probe", Context.MODE_PRIVATE);
        assertTrue("Prepare must run in a previous process", marker.getBoolean("ready", false));
        try {
            assertNotEquals("Must use a new Android process", marker.getInt("pid", -1), android.os.Process.myPid());
            assertTrue(settings.getBoolean("dark", false));
            try (LocalPdfStore store = new LocalPdfStore(context())) {
                LocalPdfStore.Document document = store.get(marker.getString("pdf", null));
                assertEquals(2, document.pages); assertEquals(1, document.lastPage);
                try (android.os.ParcelFileDescriptor descriptor = android.os.ParcelFileDescriptor.open(store.file(document.id), android.os.ParcelFileDescriptor.MODE_READ_ONLY);
                     android.graphics.pdf.PdfRenderer renderer = new android.graphics.pdf.PdfRenderer(descriptor)) {
                    assertEquals(2, renderer.getPageCount());
                }
            }
            try (CatalogDatabase database = new CatalogDatabase(context())) {
                assertEquals(marker.getLong("bookId", -1), database.search("", true).get(0).id);
                assertTrue(database.search("", true).get(0).favorite);
            }
            try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
                scenario.onActivity(activity -> assertTrue(containsText(
                        activity.findViewById(android.R.id.content), "تفعيل الوضع النهاري")));
            }
        } finally {
            SharedPreferences.Editor restore = settings.edit();
            if (marker.getBoolean("hadTheme", false)) restore.putBoolean("dark", marker.getBoolean("theme", false));
            else restore.remove("dark");
            assertTrue(restore.commit());
            try (CatalogDatabase database = new CatalogDatabase(context())) {
                database.setFavorite(marker.getLong("bookId", -1), marker.getBoolean("favorite", false));
            }
            try (LocalPdfStore store = new LocalPdfStore(context())) { store.delete(marker.getString("pdf", null)); }
            assertTrue(marker.edit().clear().commit());
        }
    }

    private static boolean containsText(View view, String text) {
        if (view instanceof TextView && ((TextView) view).getText().toString().equals(text)) return true;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) if (containsText(group.getChildAt(i), text)) return true;
        }
        return false;
    }
}
