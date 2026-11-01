package com.muteir.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.hardware.ConsumerIrManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    /** Pausa entre un código y el siguiente, además de la duración de la señal. */
    private static final int PAUSE_MS = 25;
    private static final int DEFAULT_INTERVAL_S = 30;
    private static final int MAX_INTERVAL_S = 24 * 3600;

    private static final String[] CATEGORY_NAMES = {"Todo", "TV", "Audio", "Carro", "Otros"};
    private static final String[] CATEGORY_HINTS = {
            "Televisores, equipos de sonido, carro y otros",
            "Televisores y pantallas",
            "Equipos de sonido, barras, parlantes y receptores",
            "Radios y pantallas de carro",
            "Equipos sin clasificar (reproductores, proyectores…)"
    };
    private static final String[] POPULAR_BRANDS = {
            "LG", "Samsung", "Sony", "JBL", "Philips", "Panasonic", "TCL", "Hisense", "Bose", "Yamaha"
    };
    private static final int[] INTERVAL_PRESETS = {10, 30, 60, 300, 900};
    private static final String[] PROTOS = {
            "NEC", "NEC corto (Samsung)", "Pioneer (NEC 40 kHz)", "RC5 (Philips)", "RC6 (Philips)",
            "Sony 12 bits", "Sony 15 bits", "JVC", "Denon", "Sharp", "Aiwa", "RCA"
    };

    private ConsumerIrManager ir;
    private SharedPreferences prefs;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private volatile boolean running = false;
    private float density;

    // Estado de la interfaz
    private int category = 0;
    private int intervalS = DEFAULT_INTERVAL_S;
    private final int[] categoryCounts = new int[5];
    private final long[] categoryMillis = new long[5];
    private final Map<String, String> brandLookup = new HashMap<String, String>();

    // Vistas
    private MuteButton muteButton;
    private TextView statusChip;
    private TextView categoryInfo;
    private TextView nowSending;
    private final TextView[] categoryChips = new TextView[5];
    private Switch swRepeat;
    private Switch swOnlyOn;
    private LinearLayout repeatOptions;
    private TextView intervalValue;
    private final List<TextView> presetChips = new ArrayList<TextView>();
    private AutoCompleteTextView brandInput;
    private TextView brandInfo;
    private TextView btnBrand;
    private LinearLayout advancedBody;
    private TextView advancedHeader;
    private Spinner spProto;
    private EditText etD;
    private EditText etS;
    private EditText etF;
    private TextView btnCustom;

    // ------------------------------------------------------------------ ciclo de vida

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        density = getResources().getDisplayMetrics().density;
        ir = (ConsumerIrManager) getSystemService(Context.CONSUMER_IR_SERVICE);
        prefs = getSharedPreferences("muteir", MODE_PRIVATE);
        category = clamp(prefs.getInt("category", 0), 0, 4);
        intervalS = clamp(prefs.getInt("interval", DEFAULT_INTERVAL_S), 1, MAX_INTERVAL_S);

        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(Theme.BG);
            getWindow().setNavigationBarColor(Theme.BG);
        }
        for (String b : CodeBook.brands()) brandLookup.put(b.toLowerCase(Locale.ROOT), b);

        setContentView(buildUi());
        refreshEmitterState();
        computeCategoryStats();
    }

    @Override
    protected void onPause() {
        super.onPause();
        running = false;   // si la app pasa a segundo plano, se detiene el envío
    }

    // ------------------------------------------------------------------ construcción de la interfaz

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Theme.BG);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(32));
        scroll.addView(root);

        // Encabezado
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.addView(Theme.text(this, "Mute IR", 26, Theme.TEXT, true));
        titles.addView(Theme.text(this, "Silencia TV y equipos de sonido", 14, Theme.MUTED, false));
        header.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        statusChip = Theme.text(this, "", 12, Theme.OK, true);
        statusChip.setPadding(dp(12), dp(6), dp(12), dp(6));
        header.addView(statusChip);
        root.addView(header);

        // Botón principal
        muteButton = new MuteButton(this);
        muteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                if (running) {
                    running = false;
                    return;
                }
                startCategory();
            }
        });
        root.addView(muteButton, lp(ViewGroup.LayoutParams.MATCH_PARENT, dp(24), dp(8)));

        nowSending = Theme.text(this, "", 13, Theme.MUTED, false);
        nowSending.setGravity(Gravity.CENTER);
        nowSending.setMinLines(2);
        root.addView(nowSending, lp(ViewGroup.LayoutParams.MATCH_PARENT, 0, dp(8)));

        // Categorías
        root.addView(sectionLabel("Qué silenciar"));
        HorizontalScrollView chipScroll = new HorizontalScrollView(this);
        chipScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout chipRow = new LinearLayout(this);
        chipRow.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < CATEGORY_NAMES.length; i++) {
            final int idx = i;
            TextView chip = chip(CATEGORY_NAMES[i]);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (running) return;
                    setCategory(idx);
                }
            });
            categoryChips[i] = chip;
            chipRow.addView(chip, chipLp());
        }
        chipScroll.addView(chipRow);
        root.addView(chipScroll);
        categoryInfo = Theme.text(this, "", 13, Theme.MUTED, false);
        categoryInfo.setPadding(dp(2), dp(10), 0, 0);
        root.addView(categoryInfo);

        // Tarjeta: repetir
        LinearLayout repeatCard = card();
        swRepeat = toggleRow(repeatCard, "Repetir automáticamente",
                "Vuelve a enviar los códigos hasta que lo detengas");
        swRepeat.setChecked(prefs.getBoolean("repeat", false));

        repeatOptions = new LinearLayout(this);
        repeatOptions.setOrientation(LinearLayout.VERTICAL);
        repeatOptions.setPadding(0, dp(12), 0, 0);

        repeatOptions.addView(Theme.text(this, "Cada cuánto", 13, Theme.MUTED, false));
        LinearLayout stepper = new LinearLayout(this);
        stepper.setOrientation(LinearLayout.HORIZONTAL);
        stepper.setGravity(Gravity.CENTER_VERTICAL);
        stepper.setPadding(0, dp(8), 0, dp(8));
        TextView minus = roundIconButton("−");
        TextView plus = roundIconButton("+");
        intervalValue = Theme.text(this, "", 22, Theme.TEXT, true);
        intervalValue.setGravity(Gravity.CENTER);
        stepper.addView(minus, new LinearLayout.LayoutParams(dp(44), dp(44)));
        stepper.addView(intervalValue, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        stepper.addView(plus, new LinearLayout.LayoutParams(dp(44), dp(44)));
        minus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setInterval(stepInterval(intervalS, false));
            }
        });
        plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setInterval(stepInterval(intervalS, true));
            }
        });
        intervalValue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                askInterval();
            }
        });
        repeatOptions.addView(stepper);

        HorizontalScrollView presetScroll = new HorizontalScrollView(this);
        presetScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout presetRow = new LinearLayout(this);
        presetRow.setOrientation(LinearLayout.HORIZONTAL);
        for (final int p : INTERVAL_PRESETS) {
            TextView c = chip(formatDuration(p));
            c.setTag(p);
            c.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setInterval(p);
                }
            });
            presetChips.add(c);
            presetRow.addView(c, chipLp());
        }
        presetScroll.addView(presetRow);
        repeatOptions.addView(presetScroll);

        View divider = new View(this);
        divider.setBackgroundColor(Theme.STROKE);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(1)));
        dlp.topMargin = dp(16);
        dlp.bottomMargin = dp(12);
        repeatOptions.addView(divider, dlp);

        swOnlyOn = toggleRow(repeatOptions, "Repetir sin reactivar el sonido",
                "En la mayoría de equipos, enviar mute otra vez devuelve el sonido. "
                        + "Con esto activado, las repeticiones solo usan códigos que silencian "
                        + "sin alternar (los cubren menos equipos).");
        swOnlyOn.setChecked(prefs.getBoolean("onlyOn", true));
        repeatCard.addView(repeatOptions);
        root.addView(repeatCard, cardLp());

        swRepeat.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton b, boolean checked) {
                prefs.edit().putBoolean("repeat", checked).apply();
                repeatOptions.setVisibility(checked ? View.VISIBLE : View.GONE);
            }
        });
        swOnlyOn.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton b, boolean checked) {
                prefs.edit().putBoolean("onlyOn", checked).apply();
            }
        });
        repeatOptions.setVisibility(swRepeat.isChecked() ? View.VISIBLE : View.GONE);

        // Tarjeta: mi equipo (marca)
        LinearLayout brandCard = card();
        brandCard.addView(Theme.text(this, "Mi equipo", 17, Theme.TEXT, true));
        TextView brandSub = Theme.text(this, "Envía solo los códigos de una marca: es más rápido", 13, Theme.MUTED, false);
        brandSub.setPadding(0, dp(2), 0, dp(12));
        brandCard.addView(brandSub);

        brandInput = new AutoCompleteTextView(this);
        brandInput.setHint("Busca tu marca, p. ej. LG");
        brandInput.setHintTextColor(Theme.MUTED);
        brandInput.setTextColor(Theme.TEXT);
        brandInput.setSingleLine(true);
        brandInput.setThreshold(1);
        brandInput.setBackground(Theme.outlined(Theme.SURFACE_2, Theme.STROKE, Math.max(1, dp(1)), dp(12)));
        brandInput.setPadding(dp(14), dp(12), dp(14), dp(12));
        brandInput.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, CodeBook.brands()));
        brandInput.setText(prefs.getString("brand", ""));
        brandInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                refreshBrand();
            }
        });
        brandInput.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                hideKeyboard();
                refreshBrand();
            }
        });
        brandCard.addView(brandInput, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        HorizontalScrollView popScroll = new HorizontalScrollView(this);
        popScroll.setHorizontalScrollBarEnabled(false);
        popScroll.setPadding(0, dp(10), 0, 0);
        LinearLayout popRow = new LinearLayout(this);
        popRow.setOrientation(LinearLayout.HORIZONTAL);
        for (final String b : POPULAR_BRANDS) {
            final String real = brandLookup.get(b.toLowerCase(Locale.ROOT));
            if (real == null) continue;
            TextView c = chip(real);
            c.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    brandInput.setText(real);
                    brandInput.setSelection(real.length());
                    brandInput.dismissDropDown();
                    hideKeyboard();
                }
            });
            popRow.addView(c, chipLp());
        }
        popScroll.addView(popRow);
        brandCard.addView(popScroll);

        brandInfo = Theme.text(this, "", 13, Theme.MUTED, false);
        brandInfo.setPadding(0, dp(10), 0, dp(10));
        brandCard.addView(brandInfo);

        btnBrand = primaryButton("Silenciar marca");
        btnBrand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                if (running) {
                    running = false;
                    return;
                }
                String brand = resolvedBrand();
                if (brand == null) return;
                hideKeyboard();
                runSequence(CodeBook.select(true, true, true, true, brand), brand);
            }
        });
        brandCard.addView(btnBrand, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        root.addView(brandCard, cardLp());

        // Tarjeta: código manual (plegable)
        LinearLayout advCard = card();
        advancedHeader = Theme.text(this, "", 17, Theme.TEXT, true);
        advancedHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean show = advancedBody.getVisibility() != View.VISIBLE;
                advancedBody.setVisibility(show ? View.VISIBLE : View.GONE);
                updateAdvancedHeader();
            }
        });
        advCard.addView(advancedHeader);

        advancedBody = new LinearLayout(this);
        advancedBody.setOrientation(LinearLayout.VERTICAL);
        advancedBody.setVisibility(View.GONE);
        TextView advHint = Theme.text(this, "Para marcas que no están en la lista. Valores en decimal o con 0x. "
                + "S solo se usa en NEC, Pioneer y Aiwa; si lo dejas vacío se usa el valor habitual.", 13, Theme.MUTED, false);
        advHint.setPadding(0, dp(8), 0, dp(12));
        advancedBody.addView(advHint);
        spProto = new Spinner(this);
        spProto.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, PROTOS));
        spProto.setBackground(Theme.outlined(Theme.SURFACE_2, Theme.STROKE, Math.max(1, dp(1)), dp(12)));
        advancedBody.addView(spProto, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(10), 0, dp(10));
        etD = numberField("D");
        etS = numberField("S");
        etF = numberField("F");
        row.addView(etD, fieldLp(0));
        row.addView(etS, fieldLp(dp(8)));
        row.addView(etF, fieldLp(dp(8)));
        advancedBody.addView(row);
        btnCustom = secondaryButton("Enviar código");
        btnCustom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                sendCustom();
            }
        });
        advancedBody.addView(btnCustom, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        advCard.addView(advancedBody);
        updateAdvancedHeader();
        root.addView(advCard, cardLp());

        // Consejos
        TextView tips = Theme.text(this,
                "Apunta el borde superior del teléfono hacia los aparatos, a menos de 8 m y sin "
                        + "obstáculos. Mantén la app abierta mientras envía.\n\n"
                        + "Úsala en tus propios aparatos o con permiso de quien los usa.",
                12, Theme.MUTED, false);
        tips.setPadding(dp(4), dp(20), dp(4), 0);
        root.addView(tips);

        setCategory(category);
        setInterval(intervalS);
        refreshBrand();
        return scroll;
    }

    // ------------------------------------------------------------------ fábricas de vistas

    private TextView sectionLabel(String s) {
        TextView t = Theme.text(this, s.toUpperCase(Locale.ROOT), 12, Theme.MUTED, true);
        t.setLetterSpacing(0.08f);
        t.setPadding(dp(2), dp(16), 0, dp(10));
        return t;
    }

    private TextView chip(String s) {
        TextView c = Theme.text(this, s, 14, Theme.TEXT, true);
        c.setPadding(dp(16), dp(8), dp(16), dp(8));
        c.setMinHeight(dp(36));
        Theme.styleChip(c, false, density);
        return c;
    }

    private LinearLayout.LayoutParams chipLp() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.rightMargin = dp(8);
        return p;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(18), dp(16), dp(18), dp(18));
        c.setBackground(Theme.rounded(Theme.SURFACE, dp(20)));
        return c;
    }

    private LinearLayout.LayoutParams cardLp() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(16);
        return p;
    }

    private LinearLayout.LayoutParams lp(int width, int top, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = top;
        p.bottomMargin = bottom;
        return p;
    }

    private LinearLayout.LayoutParams fieldLp(int left) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(48), 1f);
        p.leftMargin = left;
        return p;
    }

    /** Fila con título, descripción y un interruptor a la derecha; toda la fila es pulsable. */
    private Switch toggleRow(LinearLayout parent, String title, String subtitle) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(Theme.text(this, title, 16, Theme.TEXT, true));
        TextView sub = Theme.text(this, subtitle, 13, Theme.MUTED, false);
        sub.setPadding(0, dp(2), dp(12), 0);
        texts.addView(sub);
        row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final Switch sw = new Switch(this);
        if (Build.VERSION.SDK_INT >= 23) {
            int[][] states = {{android.R.attr.state_checked}, {}};
            sw.setThumbTintList(new ColorStateList(states, new int[]{Theme.ACCENT, Color.rgb(200, 205, 212)}));
            sw.setTrackTintList(new ColorStateList(states, new int[]{Theme.ACCENT_SOFT, Theme.TRACK}));
        }
        row.addView(sw);
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sw.isEnabled()) sw.toggle();
            }
        });
        parent.addView(row);
        return sw;
    }

    private TextView roundIconButton(String s) {
        TextView b = Theme.text(this, s, 22, Theme.TEXT, true);
        b.setGravity(Gravity.CENTER);
        b.setBackground(Theme.rounded(Theme.SURFACE_2, dp(22)));
        return b;
    }

    private TextView primaryButton(String s) {
        TextView b = Theme.text(this, s, 16, Theme.ON_ACCENT, true);
        b.setGravity(Gravity.CENTER);
        b.setBackground(Theme.rounded(Theme.ACCENT, dp(14)));
        return b;
    }

    private TextView secondaryButton(String s) {
        TextView b = Theme.text(this, s, 15, Theme.TEXT, true);
        b.setGravity(Gravity.CENTER);
        b.setBackground(Theme.outlined(Theme.SURFACE_2, Theme.STROKE, Math.max(1, dp(1)), dp(14)));
        return b;
    }

    private EditText numberField(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(Theme.MUTED);
        e.setTextColor(Theme.TEXT);
        e.setGravity(Gravity.CENTER);
        e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        e.setBackground(Theme.outlined(Theme.SURFACE_2, Theme.STROKE, Math.max(1, dp(1)), dp(12)));
        return e;
    }

    private void setPrimaryEnabled(TextView b, boolean enabled) {
        b.setEnabled(enabled);
        b.setAlpha(enabled ? 1f : 0.4f);
    }

    private int dp(int v) {
        return Math.round(v * density);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && brandInput != null) imm.hideSoftInputFromWindow(brandInput.getWindowToken(), 0);
    }

    // ------------------------------------------------------------------ estado de la interfaz

    private void setCategory(int idx) {
        category = idx;
        prefs.edit().putInt("category", idx).apply();
        for (int i = 0; i < categoryChips.length; i++) Theme.styleChip(categoryChips[i], i == idx, density);
        updateCategoryInfo();
    }

    private void updateCategoryInfo() {
        String hint = CATEGORY_HINTS[category];
        if (categoryCounts[category] > 0) {
            categoryInfo.setText(hint + "\n" + categoryCounts[category] + " códigos · "
                    + formatDuration((int) Math.max(1, categoryMillis[category] / 1000)) + " aprox.");
        } else {
            categoryInfo.setText(hint);
        }
    }

    /** Cuenta códigos y estima la duración de cada categoría en segundo plano. */
    private void computeCategoryStats() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 5; i++) {
                    List<CodeBook.Entry> l = selectCategory(i);
                    long ms = 0;
                    for (CodeBook.Entry e : l) ms += e.signal().totalMicros() / 1000 + PAUSE_MS;
                    categoryCounts[i] = l.size();
                    categoryMillis[i] = ms;
                }
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        updateCategoryInfo();
                    }
                });
            }
        }).start();
    }

    private List<CodeBook.Entry> selectCategory(int c) {
        boolean all = c == 0;
        return CodeBook.select(all || c == 1, all || c == 2, all || c == 3, all || c == 4, null);
    }

    private void setInterval(int seconds) {
        intervalS = clamp(seconds, 1, MAX_INTERVAL_S);
        prefs.edit().putInt("interval", intervalS).apply();
        intervalValue.setText(formatDuration(intervalS));
        for (TextView c : presetChips) Theme.styleChip(c, ((Integer) c.getTag()) == intervalS, density);
    }

    private static int stepInterval(int v, boolean up) {
        int step;
        if (up) {
            step = v < 5 ? 1 : v < 60 ? 5 : v < 600 ? 30 : v < 3600 ? 60 : 600;
        } else {
            step = v <= 5 ? 1 : v <= 60 ? 5 : v <= 600 ? 30 : v <= 3600 ? 60 : 600;
        }
        return clamp(up ? v + step : v - step, 1, MAX_INTERVAL_S);
    }

    /** Permite escribir un número exacto de segundos tocando el valor. */
    private void askInterval() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(intervalS));
        input.setSelectAllOnFocus(true);
        new android.app.AlertDialog.Builder(this)
                .setTitle("Segundos entre repeticiones")
                .setView(input)
                .setPositiveButton("Listo", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int which) {
                        try {
                            setInterval(Integer.parseInt(input.getText().toString().trim()));
                        } catch (NumberFormatException e) {
                            Toast.makeText(MainActivity.this, "Número inválido", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    static String formatDuration(int s) {
        if (s < 60) return s + " s";
        if (s < 3600) {
            int m = s / 60, r = s % 60;
            return r == 0 ? m + " min" : m + " min " + r + " s";
        }
        int h = s / 3600, m = (s % 3600) / 60;
        return m == 0 ? h + " h" : h + " h " + m + " min";
    }

    private String resolvedBrand() {
        String t = brandInput.getText().toString().trim();
        if (t.isEmpty()) return null;
        return brandLookup.get(t.toLowerCase(Locale.ROOT));
    }

    private void refreshBrand() {
        String brand = resolvedBrand();
        String typed = brandInput.getText().toString().trim();
        if (brand != null) {
            int n = CodeBook.select(true, true, true, true, brand).size();
            brandInfo.setText(n + (n == 1 ? " código" : " códigos") + " para " + brand);
            brandInfo.setTextColor(Theme.MUTED);
            btnBrand.setText(running ? "Detener" : "Silenciar " + brand);
            prefs.edit().putString("brand", brand).apply();
        } else if (typed.isEmpty()) {
            brandInfo.setText("Elige una marca de la lista o un atajo");
            brandInfo.setTextColor(Theme.MUTED);
            btnBrand.setText(running ? "Detener" : "Silenciar marca");
        } else {
            brandInfo.setText("No hay códigos para \"" + typed + "\". Prueba otra forma del nombre "
                    + "o usa el código manual.");
            brandInfo.setTextColor(Theme.WARN);
            btnBrand.setText(running ? "Detener" : "Silenciar marca");
        }
        setPrimaryEnabled(btnBrand, hasEmitter() && (running || brand != null));
    }

    private void updateAdvancedHeader() {
        boolean open = advancedBody.getVisibility() == View.VISIBLE;
        advancedHeader.setText((open ? "▾  " : "▸  ") + "Código manual");
    }

    // ------------------------------------------------------------------ IR

    private boolean hasEmitter() {
        return ir != null && ir.hasIrEmitter();
    }

    private void refreshEmitterState() {
        boolean ok = hasEmitter();
        statusChip.setText(ok ? "● IR listo" : "● Sin IR");
        statusChip.setTextColor(ok ? Theme.OK : Theme.WARN);
        statusChip.setBackground(Theme.rounded(ok ? Theme.OK_SOFT : Theme.WARN_SOFT, dp(14)));
        muteButton.setState(ok ? MuteButton.IDLE : MuteButton.DISABLED, 0, null, null);
        muteButton.setEnabled(ok);
        nowSending.setText(ok ? CodeBook.all().size() + " códigos de " + CodeBook.brands().size() + " marcas"
                : "Este teléfono no tiene emisor infrarrojo, así que no puede enviar señales.");
        setPrimaryEnabled(btnCustom, ok);
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

    private void startCategory() {
        runSequence(selectCategory(category), CATEGORY_NAMES[category]);
    }

    private void post(Runnable r) {
        ui.post(r);
    }

    private void showSending(final int state, final float progress, final String big,
                             final String small, final String line) {
        post(new Runnable() {
            @Override
            public void run() {
                muteButton.setState(state, progress, big, small);
                nowSending.setText(line);
            }
        });
    }

    private void setBusy(boolean busy) {
        for (TextView c : categoryChips) {
            c.setEnabled(!busy);
            c.setAlpha(busy ? 0.5f : 1f);
        }
        swRepeat.setEnabled(!busy);
        swOnlyOn.setEnabled(!busy);
        brandInput.setEnabled(!busy);
        setPrimaryEnabled(btnCustom, !busy && hasEmitter());
        refreshBrand();
    }

    private void runSequence(final List<CodeBook.Entry> list, final String label) {
        if (running) return;
        if (!hasEmitter()) {
            refreshEmitterState();
            return;
        }
        if (list.isEmpty()) {
            Toast.makeText(this, "No hay códigos para esa selección", Toast.LENGTH_SHORT).show();
            return;
        }

        final boolean repeat = swRepeat.isChecked();
        final int waitS = intervalS;
        final List<CodeBook.Entry> repeatList;
        if (repeat && swOnlyOn.isChecked()) {
            repeatList = new ArrayList<CodeBook.Entry>();
            for (CodeBook.Entry e : list) if (e.discrete) repeatList.add(e);
            if (repeatList.isEmpty()) {
                Toast.makeText(this, "Esta selección no tiene códigos que silencien sin alternar; "
                        + "las repeticiones enviarán todos los códigos.", Toast.LENGTH_LONG).show();
                repeatList.addAll(list);
            }
        } else {
            repeatList = list;
        }

        running = true;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setBusy(true);
        muteButton.setState(MuteButton.RUNNING, 0, "0%", label);
        nowSending.setText("Preparando…");

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
                    String cycleText = repeat ? "Ciclo " + cycle : label;
                    for (int i = 0; i < total && running; i++) {
                        CodeBook.Entry e = current.get(i);
                        int pct = Math.round(100f * i / total);
                        showSending(MuteButton.RUNNING, (float) i / total, pct + "%", cycleText,
                                (i + 1) + " de " + total + " · " + e.brandLabel());
                        IrCodec.Signal s = e.signal();
                        error = send(s);
                        if (error != null) break;
                        sentTotal++;
                        if (!sleepWhileRunning(s.totalMicros() / 1000 + PAUSE_MS)) break;
                    }
                    if (!repeat || error != null || !running) break;

                    for (int left = waitS; left > 0 && running; left--) {
                        showSending(MuteButton.WAITING, (float) left / waitS, formatShort(left),
                                "Próximo ciclo", "Ciclo " + cycle + " listo · " + sentTotal + " códigos enviados");
                        if (!sleepWhileRunning(1000)) break;
                    }
                }

                final long sent = sentTotal;
                final int cycles = cycle;
                final String err = error;
                final boolean completed = !repeat && running && err == null;
                running = false;
                post(new Runnable() {
                    @Override
                    public void run() {
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        muteButton.setState(MuteButton.IDLE, 0, null, null);
                        setBusy(false);
                        if (err != null) {
                            nowSending.setText("Error al transmitir: " + err);
                        } else if (completed) {
                            nowSending.setText("Listo · " + sent + " códigos enviados (" + label + ")");
                        } else if (repeat) {
                            nowSending.setText("Detenido · " + sent + " códigos en " + cycles
                                    + (cycles == 1 ? " ciclo" : " ciclos"));
                        } else {
                            nowSending.setText("Detenido · " + sent + " de " + list.size() + " códigos enviados");
                        }
                    }
                });
            }
        }).start();
    }

    /** Texto corto para el centro del botón durante la espera. */
    private static String formatShort(int s) {
        if (s < 60) return s + "s";
        if (s < 3600) return (s / 60) + ":" + String.format(Locale.ROOT, "%02d", s % 60);
        return (s / 3600) + "h" + String.format(Locale.ROOT, "%02d", (s % 3600) / 60);
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
                post(new Runnable() {
                    @Override
                    public void run() {
                        nowSending.setText(err == null ? "Código enviado" : "Error al transmitir: " + err);
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
