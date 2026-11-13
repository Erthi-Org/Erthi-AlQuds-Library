package org.alquds.library;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Offline personal PDF shelf; external providers are accessed only after explicit selection. */
public final class LocalPdfActivity extends Activity {
    private static final int PICK_PDF = 41;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private LocalPdfStore store;
    private LinearLayout content;
    private TextView status;
    private ImageView image;
    private Bitmap displayed;
    private String documentId;
    private int page, pages, generation;
    private boolean busy, dark;
    private int ink, background;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        dark = getSharedPreferences("library", MODE_PRIVATE).getBoolean("dark", false);
        ink = Color.parseColor(dark ? "#F4F0E6" : "#102E35");
        background = Color.parseColor(dark ? "#0A0A0A" : "#FAF7F0");
        store = new LocalPdfStore(this);
        documentId = state == null ? null : state.getString("document");
        if (documentId == null) showShelf(""); else open(documentId, state.getInt("page", 0));
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private TextView text(String value, int size) {
        TextView label = new TextView(this);
        label.setText(value); label.setTextSize(size); label.setTextColor(ink);
        label.setPadding(dp(4), dp(10), dp(4), dp(10));
        return label;
    }

    private Button button(String label, Runnable action) {
        Button button = new Button(this);
        button.setText(label); button.setAllCaps(false); button.setMinHeight(dp(48));
        button.setOnClickListener(view -> { if (!busy) action.run(); });
        content.addView(button, new LinearLayout.LayoutParams(-1, -2));
        return button;
    }

    private void screen(String title) {
        if (image != null) image.setImageDrawable(null);
        if (displayed != null) { displayed.recycle(); displayed = null; }
        image = null;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackgroundColor(background);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
                view.setPadding(dp(16) + bars.left, bars.top, dp(16) + bars.right, bars.bottom);
            } else {
                view.setPadding(dp(16) + insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                        dp(16) + insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content); root.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
        setContentView(root); root.requestApplyInsets();
        TextView heading = text(title, 24);
        if (android.os.Build.VERSION.SDK_INT >= 28) heading.setAccessibilityHeading(true);
        content.addView(heading);
        status = text("", 16); status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        content.addView(status);
    }

    private void showShelf(String message) {
        int request = ++generation;
        documentId = null; busy = false;
        screen("كتبي على هذا الجهاز");
        button("الرجوع إلى الفهرس", this::finish);
        content.addView(text("اختر ملف PDF تملكه أو يحق لك استخدامه. يُحفظ على هذا الجهاز فقط، دون رفعه إلى خادم. الحد 64 ميبيبايت للملف و512 ميبيبايت للمكتبة المحلية.", 15));
        button("إضافة ملف PDF من الهاتف", () -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/pdf")
                    .addCategory(Intent.CATEGORY_OPENABLE);
            try { startActivityForResult(intent, PICK_PDF); }
            catch (android.content.ActivityNotFoundException error) { status.setText("لا يوجد منتقي ملفات متاح على الجهاز."); }
        });
        status.setText(message.isEmpty() ? "جارٍ تحميل كتبك…" : message);
        worker.execute(() -> {
            try {
                List<LocalPdfStore.Document> documents = store.list();
                handler.post(() -> {
                    if (isDestroyed() || request != generation) return;
                    status.setText(message.isEmpty() ? documents.isEmpty() ? "لم تضف كتبًا بعد." : "الكتب المحلية: " + documents.size() : message);
                    for (LocalPdfStore.Document document : documents) {
                        button(document.title + " — " + document.pages + " صفحة", () -> open(document.id, -1));
                        button("حذف النسخة المحلية: " + document.title, () -> new AlertDialog.Builder(this)
                                .setTitle("حذف النسخة المحلية؟")
                                .setMessage("سيُحذف الملف وموضع القراءة من التطبيق. يبقى ملفك الأصلي لدى مصدره.")
                                .setNegativeButton("إلغاء", null)
                                .setPositiveButton("حذف", (dialog, which) -> delete(document.id)).show());
                    }
                });
            } catch (RuntimeException error) { failure(request, "تعذر تحميل كتبك. أعد فتح الشاشة."); }
        });
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_PDF || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (!"content".equals(uri.getScheme())) { status.setText("اختر ملفًا من منتقي الملفات."); return; }
        busy = true; int request = ++generation;
        status.setText("جارٍ نسخ الملف والتحقق منه…");
        worker.execute(() -> {
            try {
                String title = "كتاب PDF";
                try (Cursor cursor = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) title = cursor.getString(0);
                }
                LocalPdfStore.Document document;
                try (InputStream input = getContentResolver().openInputStream(uri)) {
                    if (input == null) throw new IOException("No file stream");
                    document = store.importPdf(input, title);
                }
                handler.post(() -> {
                    if (!isDestroyed() && request == generation) { busy = false; open(document.id, 0); }
                });
            } catch (IOException | RuntimeException error) {
                failure(request, "تعذر استيراد الملف. اختر PDF سليمًا غير محمي، ضمن حد المساحة، ثم أعد المحاولة.");
            }
        });
    }

    private void open(String id, int requestedPage) {
        int request = ++generation; busy = true; documentId = id;
        if (requestedPage >= 0) page = requestedPage;
        screen("قارئ PDF المحلي");
        status.setText("جارٍ فتح الكتاب…");
        worker.execute(() -> {
            try {
                LocalPdfStore.Document document = store.get(id);
                int index = requestedPage < 0 ? document.lastPage : Math.min(Math.max(0, requestedPage), document.pages - 1);
                handler.post(() -> {
                    if (isDestroyed() || request != generation) return;
                    page = index; pages = document.pages; busy = false;
                    content.addView(text(document.title, 20));
                    button("الرجوع إلى كتبي", () -> showShelf(""));
                    button("الصفحة السابقة", () -> { if (page > 0) render(page - 1); }).setEnabled(page > 0);
                    button("الصفحة التالية", () -> { if (page + 1 < pages) render(page + 1); }).setEnabled(page + 1 < pages);
                    button("انتقل إلى صفحة", this::jump);
                    image = new ImageView(this); image.setAdjustViewBounds(true);
                    content.addView(image, new LinearLayout.LayoutParams(-1, -2));
                    content.addView(text("الصفحة صورة؛ لا تتوفر قراءة نص PDF بقارئ الشاشة في هذا الإصدار.", 14));
                    render(index);
                });
            } catch (IOException | RuntimeException error) {
                handler.post(() -> { if (!isDestroyed() && request == generation) showShelf("تعذر فتح الكتاب. لم يُحذف من قائمتك."); });
            }
        });
    }

    private void render(int index) {
        // Rebuild page controls after navigation, preserving the requested page through rotation.
        if (index != page) { open(documentId, index); return; }
        int request = generation; String id = documentId; busy = true;
        status.setText("جارٍ عرض الصفحة…");
        worker.execute(() -> {
            Bitmap bitmap = null;
            try (ParcelFileDescriptor descriptor = ParcelFileDescriptor.open(store.file(id), ParcelFileDescriptor.MODE_READ_ONLY);
                 PdfRenderer renderer = new PdfRenderer(descriptor);
                 PdfRenderer.Page pdfPage = renderer.openPage(index)) {
                float scale = Math.min(1600f / pdfPage.getWidth(), 2400f / pdfPage.getHeight());
                int width = Math.max(1, Math.round(pdfPage.getWidth() * scale));
                int height = Math.max(1, Math.round(pdfPage.getHeight() * scale));
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                bitmap.eraseColor(Color.WHITE);
                pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                store.savePage(id, index);
                Bitmap result = bitmap; bitmap = null;
                handler.post(() -> {
                    if (isDestroyed() || request != generation) { result.recycle(); return; }
                    image.setImageBitmap(result); displayed = result;
                    image.setContentDescription("صورة الصفحة " + (index + 1) + " من " + pages);
                    status.setText("الصفحة " + (index + 1) + " من " + pages);
                    busy = false;
                });
            } catch (IOException | RuntimeException error) {
                if (bitmap != null) bitmap.recycle();
                failure(request, "تعذر عرض الصفحة أو حفظ موضعها. ارجع إلى كتبك وأعد المحاولة.");
            }
        });
    }

    private void jump() {
        EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint("من 1 إلى " + pages);
        input.setContentDescription("رقم الصفحة");
        new AlertDialog.Builder(this).setTitle("انتقل إلى صفحة").setView(input)
                .setNegativeButton("إلغاء", null).setPositiveButton("انتقال", (dialog, which) -> {
                    try {
                        int target = Integer.parseInt(input.getText().toString());
                        if (target < 1 || target > pages) throw new NumberFormatException();
                        open(documentId, target - 1);
                    } catch (NumberFormatException error) { status.setText("أدخل رقم صفحة من 1 إلى " + pages + "."); }
                }).show();
    }

    private void delete(String id) {
        busy = true; int request = ++generation;
        worker.execute(() -> {
            try {
                store.delete(id);
                handler.post(() -> { if (!isDestroyed() && request == generation) showShelf("حُذفت النسخة من التطبيق."); });
            } catch (IOException | RuntimeException error) { failure(request, "تعذر حذف النسخة المحلية. أعد المحاولة."); }
        });
    }

    private void failure(int request, String message) {
        handler.post(() -> { if (!isDestroyed() && request == generation) { busy = false; status.setText(message); } });
    }

    @SuppressWarnings("deprecation")
    @Override public void onBackPressed() {
        if (documentId != null) showShelf(""); else super.onBackPressed();
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        out.putString("document", documentId); out.putInt("page", page);
        super.onSaveInstanceState(out);
    }

    @Override protected void onDestroy() {
        generation++;
        if (image != null) image.setImageDrawable(null);
        if (displayed != null) displayed.recycle();
        worker.execute(() -> store.close()); worker.shutdown();
        super.onDestroy();
    }
}
