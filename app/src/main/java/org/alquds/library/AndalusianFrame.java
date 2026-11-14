package org.alquds.library;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/** Restrained geometric corner knots and double borders; decoration is not an action icon. */
final class AndalusianFrame extends Drawable {
    private final boolean dark;
    private final float density;
    AndalusianFrame(boolean dark, float density) { this.dark=dark; this.density=density; }
    @Override public void draw(Canvas canvas) {
        RectF bounds = new RectF(getBounds()); bounds.inset(density, density);
        draw(canvas, bounds, dark ? HeritagePalette.PANEL : HeritagePalette.PAPER_PANEL,
                HeritagePalette.LINE, 12*density, density);
    }
    static void draw(Canvas canvas, RectF r, int fill, int border, float radius, float density) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(fill); canvas.drawRoundRect(r,radius,radius,paint);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(density); paint.setColor(border);
        canvas.drawRoundRect(r,radius,radius,paint);
        if(r.width()<65*density || r.height()<40*density) return;
        RectF inner=new RectF(r); inner.inset(3*density,3*density);
        paint.setAlpha(155); paint.setStrokeWidth(.6f*density);
        canvas.drawRoundRect(inner,Math.max(0,radius-2*density),Math.max(0,radius-2*density),paint);
        paint.setColor(HeritagePalette.GOLD); paint.setStrokeWidth(.8f*density);
        float size=6*density;
        for(int corner=0;corner<4;corner++) {
            float x=(corner%2==0?r.left+8*density:r.right-8*density);
            float y=(corner<2?r.top+8*density:r.bottom-8*density);
            Path knot=new Path();
            for(int i=0;i<16;i++) {
                double angle=-Math.PI/2+i*Math.PI/8;
                float radial=(i%2==0?size:size*.48f);
                float px=x+(float)Math.cos(angle)*radial, py=y+(float)Math.sin(angle)*radial;
                if(i==0) knot.moveTo(px,py); else knot.lineTo(px,py);
            }
            knot.close(); canvas.drawPath(knot,paint);
        }
    }
    @Override public void setAlpha(int alpha) { }
    @Override public void setColorFilter(ColorFilter filter) { }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
