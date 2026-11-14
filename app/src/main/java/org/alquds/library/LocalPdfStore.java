package org.alquds.library;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Private user documents, independent of the existing catalog database and its migrations. */
final class LocalPdfStore extends SQLiteOpenHelper {
    static final long MAX_FILE_BYTES = 64L * 1024 * 1024;
    private static final long MAX_LIBRARY_BYTES = 512L * 1024 * 1024;
    private static final Object FILE_LOCK = new Object();
    private final File directory;

    static final class Document {
        final String id, title;
        final int pages, lastPage;
        Document(String id, String title, int pages, int lastPage) {
            this.id = id; this.title = title; this.pages = pages; this.lastPage = lastPage;
        }
    }

    LocalPdfStore(Context context) { this(context, "local-pdfs.db", "local-pdfs"); }

    LocalPdfStore(Context context, String databaseName, String folder) {
        super(context, databaseName, null, 1);
        directory = new File(context.getNoBackupFilesDir(), folder);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE documents(id TEXT PRIMARY KEY,title TEXT NOT NULL,"
                + "pages INTEGER NOT NULL CHECK(pages>0),last_page INTEGER NOT NULL DEFAULT 0 "
                + "CHECK(last_page>=0 AND last_page<pages),created_at INTEGER NOT NULL)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int from, int to) {
        throw new IllegalStateException("Unsupported document database upgrade");
    }

    File file(String id) throws IOException {
        if (id == null || !id.matches("[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}")) {
            throw new IOException("Invalid document identifier");
        }
        return new File(directory, id + ".pdf");
    }

    List<Document> list() {
        List<Document> result = new ArrayList<>();
        try (Cursor rows = getReadableDatabase().rawQuery(
                "SELECT id,title,pages,last_page FROM documents ORDER BY created_at DESC,id", null)) {
            while (rows.moveToNext()) result.add(new Document(rows.getString(0), rows.getString(1),
                    rows.getInt(2), rows.getInt(3)));
        }
        return result;
    }

    Document get(String id) throws IOException {
        file(id); // Validate even when the identifier comes from an internal Intent.
        try (Cursor rows = getReadableDatabase().rawQuery(
                "SELECT id,title,pages,last_page FROM documents WHERE id=?", new String[]{id})) {
            if (!rows.moveToFirst()) throw new IOException("Document missing");
            return new Document(rows.getString(0), rows.getString(1), rows.getInt(2), rows.getInt(3));
        }
    }

    void savePage(String id, int page) throws IOException {
        Document document = get(id);
        if (page < 0 || page >= document.pages) throw new IOException("Invalid page");
        ContentValues values = new ContentValues();
        values.put("last_page", page);
        if (getWritableDatabase().update("documents", values, "id=?", new String[]{id}) != 1) {
            throw new IOException("Could not save reading position");
        }
    }

    /** Caller owns input; neither a provider filename nor an external URI becomes a local path. */
    Document importPdf(InputStream input, String displayName) throws IOException {
        synchronized (FILE_LOCK) {
            if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Storage unavailable");
            long used = 0;
            File[] existing = directory.listFiles();
            if (existing == null) throw new IOException("Storage unavailable");
            for (File file : existing) used += file.length();
            long available = Math.min(MAX_FILE_BYTES, Math.min(MAX_LIBRARY_BYTES - used,
                    directory.getUsableSpace() - 16L * 1024 * 1024));
            if (available <= 0) throw new IOException("Storage full");
            String id = UUID.randomUUID().toString();
            File pending = new File(directory, id + ".part");
            File target = file(id);
            boolean committed = false;
            try {
                copyBounded(input, pending, available);
                int pages;
                try (ParcelFileDescriptor descriptor = ParcelFileDescriptor.open(pending, ParcelFileDescriptor.MODE_READ_ONLY);
                     PdfRenderer renderer = new PdfRenderer(descriptor)) {
                    pages = renderer.getPageCount();
                    if (pages < 1 || pages > 10000) throw new IOException("Unsupported page count");
                    try (PdfRenderer.Page ignored = renderer.openPage(0)) { /* Validate a readable first page. */ }
                } catch (SecurityException | IllegalArgumentException error) {
                    throw new IOException("Unreadable or protected PDF", error);
                }
                if (!pending.renameTo(target)) throw new IOException("Could not save document");
                String title = displayName == null ? "كتاب PDF" : displayName
                        .replaceAll("[\\p{Cntrl}\\u202A-\\u202E\\u2066-\\u2069]", " ").trim();
                if (title.isEmpty()) title = "كتاب PDF";
                if (title.length() > 160) title = title.substring(0, 160);
                ContentValues values = new ContentValues();
                values.put("id", id); values.put("title", title); values.put("pages", pages);
                values.put("created_at", System.currentTimeMillis());
                getWritableDatabase().insertOrThrow("documents", null, values);
                committed = true;
                return new Document(id, title, pages, 0);
            } finally {
                if (pending.exists() && !pending.delete()) pending.deleteOnExit();
                if (!committed && target.exists() && !target.delete()) target.deleteOnExit();
            }
        }
    }

    static void copyBounded(InputStream input, File target, long available) throws IOException {
        if (available <= 0 || available > MAX_FILE_BYTES) throw new IOException("Invalid storage limit");
        try (FileOutputStream output = new FileOutputStream(target)) {
            byte[] buffer = new byte[16384];
            byte[] signature = new byte[5];
            long count = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                if (Thread.currentThread().isInterrupted()) throw new IOException("Import interrupted");
                if (read == 0) continue;
                if (count + read > available) throw new IOException("PDF exceeds storage limit");
                for (int i = 0; i < read && count + i < signature.length; i++) signature[(int) count + i] = buffer[i];
                output.write(buffer, 0, read);
                count += read;
            }
            if (count < 5 || !"%PDF-".equals(new String(signature, StandardCharsets.US_ASCII))) {
                throw new IOException("Not a PDF");
            }
        }
    }

    void delete(String id) throws IOException {
        synchronized (FILE_LOCK) {
            get(id);
            File target = file(id);
            File tombstone = new File(directory, id + ".trash");
            boolean moved = target.exists();
            if (moved && !target.renameTo(tombstone)) throw new IOException("Could not remove local file");
            try {
                if (getWritableDatabase().delete("documents", "id=?", new String[]{id}) != 1) {
                    throw new IOException("Could not delete document metadata");
                }
            } catch (IOException | RuntimeException error) {
                if (moved && !tombstone.renameTo(target)) throw new IOException("Could not restore local file", error);
                throw error;
            }
            if (moved && !tombstone.delete()) tombstone.deleteOnExit();
        }
    }
}
