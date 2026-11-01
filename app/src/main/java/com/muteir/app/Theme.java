package com.muteir.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.TextView;

/** Colores y pequeñas fábricas de vistas para un estilo oscuro y redondeado. */
public final class Theme {

    private Theme() {}

    public static final int BG = Color.rgb(14, 17, 22);
    public static final int SURFACE = Color.rgb(24, 28, 35);
    public static final int SURFACE_2 = Color.rgb(34, 39, 48);
    public static final int TRACK = Color.rgb(42, 48, 59);
    public static final int STROKE = Color.rgb(48, 55, 67);
    public static final int TEXT = Color.rgb(242, 244, 247);
    public static final int MUTED = Color.rgb(154, 163, 175);
    public static final int ACCENT = Color.rgb(255, 77, 94);
    public static final int ACCENT_PRESSED = Color.rgb(224, 58, 76);
    public static final int ACCENT_GLOW = Color.argb(46, 255, 77, 94);
    public static final int ACCENT_SOFT = Color.argb(38, 255, 77, 94);
    public static final int ON_ACCENT = Color.WHITE;
    public static final int OK = Color.rgb(61, 220, 151);
    public static final int OK_SOFT = Color.argb(36, 61, 220, 151);
    public static final int WARN = Color.rgb(245, 165, 36);
    public static final int WARN_SOFT = Color.argb(36, 245, 165, 36);

    public static GradientDrawable rounded(int color, float radiusPx) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radiusPx);
        return d;
    }

    public static GradientDrawable outlined(int fillColor, int strokeColor, int strokePx, float radiusPx) {
        GradientDrawable d = rounded(fillColor, radiusPx);
        d.setStroke(strokePx, strokeColor);
        return d;
    }

    public static TextView text(Context c, String s, float sp, int color, boolean medium) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (medium) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return t;
    }

    /** Chip seleccionable: fondo sólido si está activo, contorno si no. */
    public static void styleChip(TextView chip, boolean selected, float density) {
        float r = 18 * density;
        if (selected) {
            chip.setBackground(rounded(ACCENT, r));
            chip.setTextColor(ON_ACCENT);
        } else {
            chip.setBackground(outlined(SURFACE_2, STROKE, Math.round(density), r));
            chip.setTextColor(TEXT);
        }
        chip.setGravity(Gravity.CENTER);
    }
}
