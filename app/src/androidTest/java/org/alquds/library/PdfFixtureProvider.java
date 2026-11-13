package org.alquds.library;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;

/** Read-only fixture in the test APK; never packaged in the user application. */
public final class PdfFixtureProvider extends ContentProvider {
    static final Uri URI = Uri.parse("content://org.alquds.library.test.pdf-fixture/book");
    private boolean removed;
    @Override public boolean onCreate() { return true; }
    @Override public String getType(Uri uri) { return "application/pdf"; }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String order) {
        MatrixCursor rows = new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME});
        rows.addRow(new Object[]{"اختبار قارئ محلي.pdf"}); return rows;
    }
    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!URI.equals(uri) || !"r".equals(mode) || removed) throw new FileNotFoundException();
        File fixture = new File(getContext().getCacheDir(), "pdf-fixture.pdf");
        try (java.io.FileOutputStream output = new java.io.FileOutputStream(fixture)) { output.write(PdfFixtures.twoPages()); }
        catch (java.io.IOException error) { throw new FileNotFoundException(error.getMessage()); }
        return ParcelFileDescriptor.open(fixture, ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String selection, String[] args) {
        if (!URI.equals(uri)) return 0;
        removed = true;
        return new File(getContext().getCacheDir(), "pdf-fixture.pdf").delete() ? 1 : 0;
    }
}
