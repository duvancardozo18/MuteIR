package com.muteir.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

/**
 * Botón circular grande: en reposo muestra un parlante tachado; mientras envía,
 * un anillo de progreso con el porcentaje; entre repeticiones, la cuenta regresiva.
 */
public class MuteButton extends View {

    public static final int IDLE = 0;
    public static final int RUNNING = 1;
    public static final int WAITING = 2;
    public static final int DISABLED = 3;

    private int state = IDLE;
    private float progress = 0f;
    private String bigText = "";
    private String smallText = "";

    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint halo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint icon = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint big = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint small = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private final Path path = new Path();
    private final float density;

    public MuteButton(Context context) {
        super(context);
        density = getResources().getDisplayMetrics().density;

        track.setStyle(Paint.Style.STROKE);
        track.setStrokeWidth(8 * density);
        track.setColor(Theme.TRACK);

        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeWidth(8 * density);
        arc.setStrokeCap(Paint.Cap.ROUND);
        arc.setColor(Theme.ACCENT);

        fill.setStyle(Paint.Style.FILL);
        halo.setStyle(Paint.Style.FILL);

        icon.setStyle(Paint.Style.FILL);
        icon.setColor(Theme.ON_ACCENT);
        iconStroke.setStyle(Paint.Style.STROKE);
        iconStroke.setStrokeCap(Paint.Cap.ROUND);
        iconStroke.setStrokeWidth(5 * density);
        iconStroke.setColor(Theme.ON_ACCENT);

        big.setTextAlign(Paint.Align.CENTER);
        big.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        big.setColor(Theme.TEXT);
        small.setTextAlign(Paint.Align.CENTER);
        small.setColor(Theme.MUTED);
        small.setTextSize(13 * density);

        setClickable(true);
        setFocusable(true);
        setContentDescription("Silenciar");
    }

    public void setState(int state, float progress, String bigText, String smallText) {
        this.state = state;
        this.progress = Math.max(0f, Math.min(1f, progress));
        this.bigText = bigText == null ? "" : bigText;
        this.smallText = smallText == null ? "" : smallText;
        setContentDescription(state == IDLE ? "Silenciar" : "Detener");
        invalidate();
    }

    public int getState() {
        return state;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = Math.round(232 * density);
        int w = resolveSize(size, widthMeasureSpec);
        setMeasuredDimension(w, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float r = Math.min(getWidth(), getHeight()) / 2f - 10 * density;

        // Anillo exterior
        oval.set(cx - r, cy - r, cx + r, cy + r);
        canvas.drawCircle(cx, cy, r, track);
        if (state == RUNNING || state == WAITING) {
            arc.setColor(state == WAITING ? Theme.OK : Theme.ACCENT);
            canvas.drawArc(oval, -90f, 360f * progress, false, arc);
        }

        // Disco interior
        float inner = r - 18 * density;
        if (state == IDLE) {
            halo.setColor(Theme.ACCENT_GLOW);
            canvas.drawCircle(cx, cy, inner + 8 * density, halo);
            fill.setColor(isPressed() ? Theme.ACCENT_PRESSED : Theme.ACCENT);
        } else if (state == DISABLED) {
            fill.setColor(Theme.SURFACE_2);
        } else {
            fill.setColor(Theme.SURFACE);
        }
        canvas.drawCircle(cx, cy, inner, fill);

        if (state == IDLE || state == DISABLED) {
            int c = state == IDLE ? Theme.ON_ACCENT : Theme.MUTED;
            icon.setColor(c);
            iconStroke.setColor(c);
            drawSpeakerX(canvas, cx, cy - 14 * density, 30 * density);
            big.setTextSize(17 * density);
            big.setColor(c);
            big.setLetterSpacing(0.12f);
            canvas.drawText(state == IDLE ? "SILENCIAR" : "SIN EMISOR IR", cx, cy + 46 * density, big);
            big.setLetterSpacing(0f);
        } else {
            big.setTextSize(44 * density);
            big.setColor(Theme.TEXT);
            canvas.drawText(bigText, cx, cy + 10 * density, big);
            canvas.drawText(smallText, cx, cy + 40 * density, small);
            small.setColor(Theme.MUTED);
            canvas.drawText("Toca para detener", cx, cy + 62 * density, small);
        }
    }

    /** Parlante con una X, dibujado con trazos simples. */
    private void drawSpeakerX(Canvas c, float cx, float cy, float s) {
        float left = cx - s;
        path.reset();
        path.moveTo(left, cy - s * 0.32f);
        path.lineTo(left + s * 0.42f, cy - s * 0.32f);
        path.lineTo(left + s * 0.95f, cy - s * 0.8f);
        path.lineTo(left + s * 0.95f, cy + s * 0.8f);
        path.lineTo(left + s * 0.42f, cy + s * 0.32f);
        path.lineTo(left, cy + s * 0.32f);
        path.close();
        c.drawPath(path, icon);
        float x0 = cx + s * 0.25f;
        float x1 = cx + s * 0.95f;
        float h = s * 0.35f;
        c.drawLine(x0, cy - h, x1, cy + h, iconStroke);
        c.drawLine(x0, cy + h, x1, cy - h, iconStroke);
    }

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);
        animate().scaleX(pressed ? 0.96f : 1f).scaleY(pressed ? 0.96f : 1f).setDuration(120).start();
        invalidate();
    }
}
