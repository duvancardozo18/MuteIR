package com.muteir.app;

import android.app.Activity;
import android.content.Context;
import android.hardware.ConsumerIrManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends Activity {

    private ConsumerIrManager ir;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private volatile boolean running = false;

    private TextView status;
    private TextView progress;
    private Button btnAll;
    private Button btnBrand;
    private Button btnCustom;
    private RadioButton rbAll;
    private RadioButton rbTv;
    private RadioButton rbAudio;
    private Spinner spBrand;
    private Spinner spProto;
    private EditText etD;
    private EditText etS;
    private EditText etF;

    private static final String[] PROTOS = {
            "NEC", "NEC corto (Samsung)", "RC5 (Philips)", "Sony 12 bits", "Sony 15 bits", "JVC"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ir = (ConsumerIrManager) getSystemService(Context.CONSUMER_IR_SERVICE);
        setContentView(buildUi());
        refreshEmitterState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        running = false;   // si la app pasa a segundo plano, se detiene el envío
    }

    // ------------------------------------------------------------------ UI

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(24));
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("Mute IR");
        title.setTextSize(28);
        title.setTextColor(Color.BLACK);
        root.addView(title);

        status = new TextView(this);
        status.setTextSize(14);
        status.setPadding(0, dp(4), 0, dp(16));
        root.addView(status);

        // --- Mutear todo
        RadioGroup group = new RadioGroup(this);
        group.setOrientation(RadioGroup.HORIZONTAL);
        rbAll = radio("Todo");
        rbTv = radio("Solo TV");
        rbAudio = radio("Solo audio");
        group.addView(rbAll);
        group.addView(rbTv);
        group.addView(rbAudio);
        rbAll.setChecked(true);
        root.addView(group);

        btnAll = new Button(this);
        btnAll.setText("MUTEAR TODO");
        btnAll.setTextSize(20);
        btnAll.setPadding(dp(8), dp(24), dp(8), dp(24));
        btnAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean tv = rbAll.isChecked() || rbTv.isChecked();
                boolean audio = rbAll.isChecked() || rbAudio.isChecked();
                runSequence(CodeBook.select(tv, audio, null), btnAll, "MUTEAR TODO");
            }
        });
        root.addView(btnAll, matchWrap(dp(8), 0));

        progress = new TextView(this);
        progress.setTextSize(13);
        progress.setPadding(0, dp(6), 0, dp(6));
        root.addView(progress);

        root.addView(hint(
                "Apunta el borde superior del teléfono hacia los aparatos (hasta ~8 m, "
                        + "sin obstáculos). Mute es un interruptor: si un aparato ya estaba "
                        + "en silencio, vuelve el sonido; en ese caso pulsa otra vez."));

        // --- Por marca
        root.addView(section("Una sola marca"));
        spBrand = new Spinner(this);
        List<String> brands = CodeBook.brands();
        spBrand.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, brands));
        root.addView(spBrand);
        btnBrand = new Button(this);
        btnBrand.setText("Mutear esta marca");
        btnBrand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String brand = (String) spBrand.getSelectedItem();
                runSequence(CodeBook.select(true, true, brand), btnBrand, "Mutear esta marca");
            }
        });
        root.addView(btnBrand);

        // --- Código propio
        root.addView(section("Código propio"));
        root.addView(hint("Útil para marcas que no están en la lista. Valores en decimal o con 0x. "
                + "Para RC5, Sony y JVC se ignora el campo S."));
        spProto = new Spinner(this);
        spProto.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, PROTOS));
        root.addView(spProto);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        etD = number("D");
        etS = number("S");
        etF = number("F");
        row.addView(etD, weight());
        row.addView(etS, weight());
        row.addView(etF, weight());
        root.addView(row);

        btnCustom = new Button(this);
        btnCustom.setText("Enviar código");
        btnCustom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCustom();
            }
        });
        root.addView(btnCustom);

        root.addView(hint("Úsalo en tus propios aparatos o con permiso de quien los usa."));
        return scroll;
    }

    private RadioButton radio(String text) {
        RadioButton rb = new RadioButton(this);
        rb.setText(text);
        return rb;
    }

    private TextView section(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(18);
        t.setTextColor(Color.BLACK);
        t.setPadding(0, dp(24), 0, dp(6));
        return t;
    }

    private TextView hint(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(13);
        t.setTextColor(Color.DKGRAY);
        t.setPadding(0, dp(4), 0, dp(8));
        return t;
    }

    private EditText number(String hintText) {
        EditText e = new EditText(this);
        e.setHint(hintText);
        e.setGravity(Gravity.CENTER);
        e.setSingleLine(true);
        return e;
    }

    private LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
    }

    private LinearLayout.LayoutParams matchWrap(int top, int bottom) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = top;
        lp.bottomMargin = bottom;
        return lp;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    // ------------------------------------------------------------------ IR

    private boolean hasEmitter() {
        return ir != null && ir.hasIrEmitter();
    }

    private void refreshEmitterState() {
        boolean ok = hasEmitter();
        if (ok) {
            status.setText("Emisor infrarrojo detectado. " + CodeBook.all().size()
                    + " códigos de " + CodeBook.brands().size() + " marcas.");
            status.setTextColor(Color.rgb(0, 120, 60));
        } else {
            status.setText("Este teléfono no tiene emisor infrarrojo; la app no puede enviar señales.");
            status.setTextColor(Color.rgb(180, 0, 0));
        }
        btnAll.setEnabled(ok);
        btnBrand.setEnabled(ok);
        btnCustom.setEnabled(ok);
    }

    private int pickCarrier(int wanted) {
        ConsumerIrManager.CarrierFrequencyRange[] ranges = ir.getCarrierFrequencies();
        if (ranges == null || ranges.length == 0) return wanted;
        int best = wanted;
        int bestDiff = Integer.MAX_VALUE;
        for (ConsumerIrManager.CarrierFrequencyRange r : ranges) {
            int c = Math.max(r.getMinFrequency(), Math.min(wanted, r.getMaxFrequency()));
            int diff = Math.abs(c - wanted);
            if (diff < bestDiff) {
                bestDiff = diff;
                best = c;
            }
        }
        return best;
    }

    /** Envía una señal; devuelve null si todo fue bien o el mensaje de error. */
    private String send(IrCodec.Signal s) {
        try {
            ir.transmit(pickCarrier(s.carrierHz), s.pattern);
            return null;
        } catch (Exception e) {
            return e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    private void runSequence(final List<CodeBook.Entry> list, final Button source, final String idleText) {
        if (running) {          // un segundo toque detiene el envío
            running = false;
            return;
        }
        if (!hasEmitter()) {
            refreshEmitterState();
            return;
        }
        if (list.isEmpty()) {
            Toast.makeText(this, "No hay códigos para esa selección", Toast.LENGTH_SHORT).show();
            return;
        }
        running = true;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setBusy(source, true, idleText);

        new Thread(new Runnable() {
            @Override
            public void run() {
                final int total = list.size();
                int sent = 0;
                String error = null;
                for (int i = 0; i < total && running; i++) {
                    final CodeBook.Entry e = list.get(i);
                    final int n = i + 1;
                    ui.post(new Runnable() {
                        @Override
                        public void run() {
                            progress.setText("Enviando " + n + "/" + total + ": " + e.brandLabel());
                        }
                    });
                    IrCodec.Signal s = e.signal();
                    error = send(s);
                    if (error != null) break;
                    sent++;
                    try {
                        Thread.sleep(s.totalMicros() / 1000 + 60);
                    } catch (InterruptedException ie) {
                        break;
                    }
                }
                final boolean stopped = !running;
                final int sentFinal = sent;
                final String err = error;
                running = false;
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        setBusy(source, false, idleText);
                        if (err != null) {
                            progress.setText("Error al transmitir: " + err);
                        } else if (stopped && sentFinal < total) {
                            progress.setText("Detenido tras " + sentFinal + " de " + total + " códigos.");
                        } else {
                            progress.setText("Listo: " + sentFinal + " códigos enviados.");
                        }
                    }
                });
            }
        }).start();
    }

    private void setBusy(Button source, boolean busy, String idleText) {
        btnAll.setEnabled(!busy || source == btnAll);
        btnBrand.setEnabled(!busy || source == btnBrand);
        btnCustom.setEnabled(!busy);
        source.setText(busy ? "DETENER" : idleText);
    }

    private void sendCustom() {
        final int d, s, f;
        try {
            d = parse(etD, 0);
            s = parse(etS, -1);
            f = parse(etF, 0);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Valores inválidos", Toast.LENGTH_SHORT).show();
            return;
        }
        final int proto = spProto.getSelectedItemPosition();
        final int sub = s < 0 ? (~d & 0xFF) : s;   // S vacío en NEC = complemento de D
        new Thread(new Runnable() {
            @Override
            public void run() {
                IrCodec.Signal sig;
                switch (proto) {
                    case 0: sig = IrCodec.nec(d, sub, f); break;
                    case 1: sig = IrCodec.necx(d, sub, f); break;
                    case 2: sig = IrCodec.rc5(d, f, false); break;
                    case 3: sig = IrCodec.sony(d, f, 12, 3); break;
                    case 4: sig = IrCodec.sony(d, f, 15, 3); break;
                    default: sig = IrCodec.jvc(d, f); break;
                }
                final String err = send(sig);
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        progress.setText(err == null ? "Código enviado." : "Error al transmitir: " + err);
                    }
                });
            }
        }).start();
    }

    private int parse(EditText e, int emptyValue) {
        String t = e.getText().toString().trim();
        if (t.isEmpty()) return emptyValue;
        return Integer.decode(t);
    }
}
