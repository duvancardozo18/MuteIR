package com.muteir.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.ConsumerIrManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    /** Pausa entre un código y el siguiente, además de la duración de la señal. */
    private static final int PAUSE_MS = 25;
    private static final int DEFAULT_INTERVAL_S = 30;

    private ConsumerIrManager ir;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private volatile boolean running = false;

    private TextView status;
    private TextView progress;
    private Button btnAll;
    private Button btnBrand;
    private Button btnCustom;
    private Spinner spCategory;
    private Spinner spBrand;
    private Spinner spProto;
    private CheckBox cbRepeat;
    private CheckBox cbOnlyOn;
    private EditText etInterval;
    private EditText etD;
    private EditText etS;
    private EditText etF;

    private static final String[] CATEGORIES = {
            "Todo", "Solo TV", "Solo audio (equipos, barras, parlantes)", "Solo audio de carro",
            "Solo otros / sin clasificar"
    };

    private static final String[] PROTOS = {
            "NEC", "NEC corto (Samsung)", "Pioneer (NEC 40 kHz)", "RC5 (Philips)", "RC6 (Philips)",
            "Sony 12 bits", "Sony 15 bits", "JVC", "Denon", "Sharp", "Aiwa", "RCA"
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
        root.addView(label("Qué silenciar"));
        spCategory = new Spinner(this);
        spCategory.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, CATEGORIES));
        root.addView(spCategory);

        btnAll = new Button(this);
        btnAll.setText("MUTEAR TODO");
        btnAll.setTextSize(20);
        btnAll.setPadding(dp(8), dp(24), dp(8), dp(24));
        btnAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int c = spCategory.getSelectedItemPosition();
                boolean all = c == 0;
                List<CodeBook.Entry> list = CodeBook.select(
                        all || c == 1, all || c == 2, all || c == 3, all || c == 4, null);
                runSequence(list, btnAll, "MUTEAR TODO");
            }
        });
        root.addView(btnAll, matchWrap(dp(8), 0));

        progress = new TextView(this);
        progress.setTextSize(13);
        progress.setPadding(0, dp(6), 0, dp(6));
        root.addView(progress);

        // --- Repetición
        cbRepeat = new CheckBox(this);
        cbRepeat.setText("Repetir hasta que lo detenga");
        root.addView(cbRepeat);

        LinearLayout intervalRow = new LinearLayout(this);
        intervalRow.setOrientation(LinearLayout.HORIZONTAL);
        intervalRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView intervalLabel = new TextView(this);
        intervalLabel.setText("Esperar entre repeticiones (segundos): ");
        intervalLabel.setTextSize(14);
        etInterval = new EditText(this);
        etInterval.setInputType(InputType.TYPE_CLASS_NUMBER);
        etInterval.setText(String.valueOf(DEFAULT_INTERVAL_S));
        etInterval.setGravity(Gravity.CENTER);
        etInterval.setEms(4);
        intervalRow.addView(intervalLabel);
        intervalRow.addView(etInterval);
        root.addView(intervalRow);

        cbOnlyOn = new CheckBox(this);
        cbOnlyOn.setText("En las repeticiones enviar solo códigos \"mute ON\"");
        cbOnlyOn.setChecked(true);
        root.addView(cbOnlyOn);

        root.addView(hint(
                "Apunta el borde superior del teléfono hacia los aparatos (hasta ~8 m, "
                        + "sin obstáculos). En la mayoría de equipos mute es un interruptor: "
                        + "si se envía de nuevo, el sonido vuelve. Por eso, al repetir, lo "
                        + "recomendado es dejar marcada la opción \"solo mute ON\": las "
                        + "repeticiones solo envían códigos que silencian sin reactivar el "
                        + "sonido (son pocos equipos). Si la desmarcas, cada repetición vuelve "
                        + "a alternar el sonido de los demás. La app debe quedar abierta y "
                        + "en pantalla mientras repite."));

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
                runSequence(CodeBook.select(true, true, true, true, brand), btnBrand,
                        "Mutear esta marca");
            }
        });
        root.addView(btnBrand);
        root.addView(hint("La repetición también aplica a este botón."));

        // --- Código propio
        root.addView(section("Código propio"));
        root.addView(hint("Útil para marcas que no están en la lista. Valores en decimal o con 0x. "
                + "S solo se usa en NEC, Pioneer y Aiwa; vacío = valor habitual."));
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

    private TextView label(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(14);
        t.setTextColor(Color.DKGRAY);
        return t;
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

    private int readInterval() {
        try {
            int v = Integer.parseInt(etInterval.getText().toString().trim());
            return Math.max(1, Math.min(v, 24 * 3600));
        } catch (NumberFormatException e) {
            etInterval.setText(String.valueOf(DEFAULT_INTERVAL_S));
            return DEFAULT_INTERVAL_S;
        }
    }

    private void showProgress(final String text) {
        ui.post(new Runnable() {
            @Override
            public void run() {
                progress.setText(text);
            }
        });
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

        final boolean repeat = cbRepeat.isChecked();
        final int intervalS = readInterval();
        final List<CodeBook.Entry> repeatList;
        if (repeat && cbOnlyOn.isChecked()) {
            repeatList = new ArrayList<CodeBook.Entry>();
            for (CodeBook.Entry e : list) if (e.discrete) repeatList.add(e);
            if (repeatList.isEmpty()) {
                Toast.makeText(this, "Esta selección no tiene códigos \"mute ON\"; "
                        + "se repetirán todos los códigos.", Toast.LENGTH_LONG).show();
                repeatList.addAll(list);
            }
        } else {
            repeatList = list;
        }

        running = true;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setBusy(source, true, idleText);

        new Thread(new Runnable() {
            @Override
            public void run() {
                int cycle = 0;
                long sentTotal = 0;
                String error = null;

                while (running && error == null) {
                    cycle++;
                    List<CodeBook.Entry> current = cycle == 1 ? list : repeatList;
                    final int total = current.size();
                    String prefix = repeat ? "Ciclo " + cycle + " · " : "";
                    for (int i = 0; i < total && running; i++) {
                        CodeBook.Entry e = current.get(i);
                        showProgress(prefix + "Enviando " + (i + 1) + "/" + total + ": " + e.brandLabel());
                        IrCodec.Signal s = e.signal();
                        error = send(s);
                        if (error != null) break;
                        sentTotal++;
                        if (!sleepWhileRunning(s.totalMicros() / 1000 + PAUSE_MS)) break;
                    }
                    if (!repeat || error != null || !running) break;

                    // Espera entre repeticiones, con cuenta regresiva.
                    for (int left = intervalS; left > 0 && running; left--) {
                        showProgress("Ciclo " + cycle + " terminado. Próxima repetición en "
                                + left + " s (toca DETENER para parar).");
                        if (!sleepWhileRunning(1000)) break;
                    }
                }

                final boolean stoppedByUser = !running;
                final long sentFinal = sentTotal;
                final int cycles = cycle;
                final String err = error;
                running = false;
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        setBusy(source, false, idleText);
                        if (err != null) {
                            progress.setText("Error al transmitir: " + err);
                        } else if (repeat) {
                            progress.setText("Detenido: " + sentFinal + " códigos enviados en "
                                    + cycles + (cycles == 1 ? " ciclo." : " ciclos."));
                        } else if (stoppedByUser && sentFinal < list.size()) {
                            progress.setText("Detenido tras " + sentFinal + " de " + list.size() + " códigos.");
                        } else {
                            progress.setText("Listo: " + sentFinal + " códigos enviados.");
                        }
                    }
                });
            }
        }).start();
    }

    /** Duerme en tramos cortos para que DETENER responda enseguida. */
    private boolean sleepWhileRunning(long ms) {
        long end = System.currentTimeMillis() + ms;
        try {
            while (running) {
                long left = end - System.currentTimeMillis();
                if (left <= 0) return true;
                Thread.sleep(Math.min(left, 100));
            }
        } catch (InterruptedException ie) {
            return false;
        }
        return false;
    }

    private void setBusy(Button source, boolean busy, String idleText) {
        btnAll.setEnabled(!busy || source == btnAll);
        btnBrand.setEnabled(!busy || source == btnBrand);
        btnCustom.setEnabled(!busy);
        cbRepeat.setEnabled(!busy);
        cbOnlyOn.setEnabled(!busy);
        etInterval.setEnabled(!busy);
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                IrCodec.Signal sig;
                switch (proto) {
                    case 0: sig = IrCodec.nec(d, s < 0 ? (~d & 0xFF) : s, f); break;
                    case 1: sig = IrCodec.necx(d, s < 0 ? d : s, f); break;
                    case 2: sig = IrCodec.pioneer(d, s < 0 ? (~d & 0xFF) : s, f); break;
                    case 3: sig = IrCodec.rc5(d, f, false); break;
                    case 4: sig = IrCodec.rc6(d, f); break;
                    case 5: sig = IrCodec.sony(d, f, 12, 3); break;
                    case 6: sig = IrCodec.sony(d, f, 15, 3); break;
                    case 7: sig = IrCodec.jvc(d, f); break;
                    case 8: sig = IrCodec.denon(d, f); break;
                    case 9: sig = IrCodec.sharp(d, f); break;
                    case 10: sig = IrCodec.aiwa(d, s < 0 ? 0 : s, f); break;
                    default: sig = IrCodec.rca(d, f, 58000); break;
                }
                final String err = send(sig);
                showProgress(err == null ? "Código enviado." : "Error al transmitir: " + err);
            }
        }).start();
    }

    private int parse(EditText e, int emptyValue) {
        String t = e.getText().toString().trim();
        if (t.isEmpty()) return emptyValue;
        return Integer.decode(t);
    }
}
