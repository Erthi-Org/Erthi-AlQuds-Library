package org.alquds.library;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.widget.TextView;
import java.util.HashMap;
import java.util.Map;

/** Bundled original-color Fluent 3D PNGs; no font glyphs or tinting. */
final class UiIcons {
    private final Context context;
    private final Map<Integer, Bitmap> cache = new HashMap<>();
    UiIcons(Context context) { this.context = context; }
    void draw(Canvas canvas, int resource, float x, float y, float size) {
        Bitmap bitmap = cache.get(resource);
        if (bitmap == null) {
            BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=2;
            bitmap = BitmapFactory.decodeResource(context.getResources(), resource,options);
            cache.put(resource, bitmap);
        }
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(bitmap, null, new RectF(x-size/2, y-size/2, x+size/2, y+size/2), paint);
    }
    void close() { for (Bitmap bitmap : cache.values()) bitmap.recycle(); cache.clear(); }
    static void apply(TextView view, int resource) {
        Drawable icon = view.getContext().getDrawable(resource);
        int size = Math.round(28 * view.getResources().getDisplayMetrics().density);
        icon.setBounds(0, 0, size, size);
        view.setCompoundDrawablesRelative(icon, null, null, null);
        view.setCompoundDrawablePadding(Math.round(10 * view.getResources().getDisplayMetrics().density));
    }
    static int label(String label) {
        if (label.contains("الخروج") || label.equals("خروج")) return R.drawable.ic_ui_exit;
        if (label.contains("إلغاء") || label.contains("إغلاق")) return R.drawable.ic_ui_close;
        if (label.contains("الرجوع") || label.equals("رجوع") || label.contains("السابقة") || label.equals("السابق")) return R.drawable.ic_ui_back;
        if (label.contains("التالية") || label.equals("التالي")) return R.drawable.ic_ui_next;
        if (label.equals("فتح")) return R.drawable.ic_ui_folder;
        if (label.contains("تصفية")) return R.drawable.ic_ui_filter;
        if (label.equals("100%")) return R.drawable.ic_search;
        if (label.contains("معاينة")) return R.drawable.ic_ui_page;
        if (label.contains("حذف")) return R.drawable.ic_ui_delete;
        if (label.contains("حفظ") || label.contains("تعديل")) return R.drawable.ic_ui_save;
        if (label.contains("إضافة ملف")) return R.drawable.ic_ui_folder;
        if (label.contains("انتقل") || label.contains("انتقال")) return R.drawable.ic_ui_bookmark;
        if (label.contains("المفضلة")) return R.drawable.ic_favorite;
        if (label.contains("البحث") || label.equals("بحث")) return R.drawable.ic_search;
        if (label.contains("المصادر") || label.equals("الموقع") || label.contains("المصدر")) return R.drawable.ic_ui_sources;
        if (label.contains("إرثي")) return R.drawable.ic_ui_info;
        if (label.contains("مشاركة")) return R.drawable.ic_ui_share;
        if (label.contains("تحديث") || label.contains("محاولة")) return R.drawable.ic_ui_refresh;
        if (label.contains("استمع")) return R.drawable.ic_ui_headphones;
        if (label.contains("القرآن") || label.startsWith("سورة")) return R.drawable.ic_ui_quran;
        if (label.contains("الليلي")) return R.drawable.ic_moon;
        if (label.contains("النهاري")) return R.drawable.ic_sun;
        return R.drawable.ic_books;
    }
    static int category(String name) {
        if (name.contains("القرآن")) return R.drawable.ic_ui_quran;
        if (name.contains("الحديث")) return R.drawable.ic_ui_hadith;
        if (name.contains("الفقه")) return R.drawable.ic_ui_law;
        if (name.contains("السيرة")) return R.drawable.ic_ui_people;
        if (name.contains("اللغة")) return R.drawable.ic_ui_language;
        if (name.contains("التاريخ")) return R.drawable.ic_ui_history;
        if (name.contains("العلوم")) return R.drawable.ic_ui_science;
        return R.drawable.ic_ui_geography;
    }
    static boolean inline(String value) {
        return value.equals("السابق") || value.equals("التالي") || value.equals("فتح") || value.equals("عرض الكل")
                || value.equals("استمع الآن") || value.equals("تحديث القراء") || value.equals("تصفية")
                || value.equals("مشاركة") || value.equals("المصدر الأصلي") || value.equals("100%");
    }
    static int glyph(String value) {
        switch (value) {
            case "⌕": return R.drawable.ic_search;
            case "☰": return R.drawable.ic_ui_menu;
            case "‹": return R.drawable.ic_ui_back;
            case "›": return R.drawable.ic_ui_next;
            case "♥": case "♡": return R.drawable.ic_favorite;
            case "▶": return R.drawable.ic_ui_play;
            case "Ⅱ": return R.drawable.ic_ui_pause;
            case "☑": return R.drawable.ic_ui_checked;
            case "□": return R.drawable.ic_ui_unchecked;
            case "◈": return R.drawable.ic_ui_quran;
            default: return 0;
        }
    }
}
