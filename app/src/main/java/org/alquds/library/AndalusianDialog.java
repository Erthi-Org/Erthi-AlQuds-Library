package org.alquds.library;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/** Native dialog interaction retained, with consistent frames and labeled color icons. */
final class AndalusianDialog extends AlertDialog.Builder {
    private final boolean dark;
    AndalusianDialog(Context context) {
        super(context, isDark(context) ? android.R.style.Theme_Material_Dialog_Alert
                : android.R.style.Theme_Material_Light_Dialog_Alert);
        dark=isDark(context);
    }
    private static boolean isDark(Context context) {
        return context instanceof HeritageHomeActivity || HeritagePalette.isDark(context);
    }
    @Override public AlertDialog.Builder setItems(CharSequence[] items, DialogInterface.OnClickListener listener) {
        ArrayAdapter<CharSequence> adapter=new ArrayAdapter<CharSequence>(getContext(),android.R.layout.simple_list_item_1,items) {
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                TextView row=(TextView)super.getView(position,convertView,parent);
                float density=row.getResources().getDisplayMetrics().density;
                row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); row.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
                row.setMinHeight(Math.round(56*density)); row.setTextSize(17);
                row.setPadding(Math.round(20*density),Math.round(10*density),Math.round(20*density),Math.round(10*density));
                row.setTextColor(dark ? HeritagePalette.IVORY : HeritagePalette.INK);
                UiIcons.apply(row,UiIcons.label(items[position].toString()));
                return row;
            }
        };
        return super.setAdapter(adapter,listener);
    }
    @Override public AlertDialog create() {
        AlertDialog dialog=super.create();
        dialog.setOnShowListener(ignored->{
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new AndalusianFrame(dark,
                        getContext().getResources().getDisplayMetrics().density));
                dialog.getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            }
            for(int which:new int[]{DialogInterface.BUTTON_POSITIVE,DialogInterface.BUTTON_NEGATIVE,DialogInterface.BUTTON_NEUTRAL}) {
                android.widget.Button button=dialog.getButton(which);
                if(button!=null) {
                    UiIcons.apply(button,UiIcons.label(button.getText().toString()));
                    button.setTextColor(dark ? HeritagePalette.GILT : 0xff684719);
                    button.setMinHeight(Math.round(48*getContext().getResources().getDisplayMetrics().density));
                }
            }
        });
        return dialog;
    }
}
