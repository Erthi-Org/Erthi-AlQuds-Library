package org.alquds.library;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.widget.EditText;

final class HeritagePalette {
    static final int EMERALD = 0xff062622;
    static final int DEEP = 0xff031a18;
    static final int PANEL = 0xff0b342d;
    static final int SELECTED = 0xff184139;
    static final int GOLD = 0xffdfb56b;
    static final int GILT = 0xfff3d79a;
    static final int LINE = 0xffab8045;
    static final int IVORY = 0xfff7ead3;
    static final int INK = 0xff18271f;
    static final int MUTED = 0xffcbbd9f;
    static final int PAPER = 0xfff8efd9;
    static final int PAPER_PANEL = 0xfffff7e7;
    static final int PAPER_MUTED = 0xff525544;

    private HeritagePalette() {}

    static boolean isDark(Context context) {
        return context.getSharedPreferences("library", Context.MODE_PRIVATE).getBoolean("dark", true);
    }

    static RippleDrawable button(boolean dark, float density) {
        return new RippleDrawable(ColorStateList.valueOf(dark ? 0x55dfb56b : 0x44745125),
                new AndalusianFrame(dark, density), null);
    }

    static void styleInput(EditText input, boolean dark) {
        input.setTextColor(dark ? IVORY : INK);
        input.setHintTextColor(dark ? MUTED : PAPER_MUTED);
        input.setBackgroundTintList(ColorStateList.valueOf(dark ? GILT : 0xff745125));
    }

    static Drawable backdrop(Context context, boolean dark) {
        Bitmap tile = BitmapFactory.decodeResource(context.getResources(), R.drawable.heritage_zellige);
        float density = context.getResources().getDisplayMetrics().density;
        return new Drawable() {
            private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
            private final RectF strip = new RectF();

            @Override public void draw(Canvas canvas) {
                canvas.drawColor(dark ? EMERALD : PAPER);
                paint.setAlpha(dark ? 235 : 175);
                strip.set(getBounds().left, getBounds().top, getBounds().right, getBounds().top + 9 * density);
                canvas.drawBitmap(tile, null, strip, paint);
                strip.offset(0, getBounds().height() - strip.height());
                canvas.drawBitmap(tile, null, strip, paint);
            }

            @Override public void setAlpha(int alpha) {}
            @Override public void setColorFilter(ColorFilter filter) {}
            @Override public int getOpacity() { return PixelFormat.OPAQUE; }
        };
    }
}
