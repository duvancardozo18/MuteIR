package com.muteir.app;

import java.util.ArrayList;
import java.util.List;

/**
 * Construye patrones infrarrojos listos para ConsumerIrManager.transmit().
 * Todas las duraciones están en microsegundos y el patrón siempre empieza por ON.
 * Esta clase no depende de Android, así que se puede probar en cualquier JVM.
 */
public final class IrCodec {

    private IrCodec() {}

    /** Frecuencia de portadora + patrón ON/OFF alternado. */
    public static final class Signal {
        public final int carrierHz;
        public final int[] pattern;

        public Signal(int carrierHz, int[] pattern) {
            this.carrierHz = carrierHz;
            this.pattern = pattern;
        }

        public int totalMicros() {
            long t = 0;
            for (int v : pattern) t += v;
            return (int) Math.min(t, Integer.MAX_VALUE);
        }
    }

    /** Silencio final de cada trama (aprox. el que deja un control remoto real). */
    private static final int GAP_US = 40000;

    // ---------------------------------------------------------------- NEC

    /** NEC estándar: cabecera 9 ms / 4,5 ms. Palabra: D, S, F, ~F (LSB primero). */
    public static Signal nec(int d, int s, int f) {
        return nec(d, s, f, ~f);
    }

    /** NEC con el cuarto byte explícito (algunos equipos no usan ~F). */
    public static Signal nec(int d, int s, int f, int g) {
        return pulseDistance(38000, 16 * 564, 8 * 564, 564, 564, 1692, necWord(d, s, f, g), 32);
    }

    /** NEC "extendido" con cabecera corta 4,5 ms / 4,5 ms (Samsung y similares). */
    public static Signal necx(int d, int s, int f) {
        return necx(d, s, f, ~f);
    }

    public static Signal necx(int d, int s, int f, int g) {
        return pulseDistance(38000, 8 * 564, 8 * 564, 564, 564, 1692, necWord(d, s, f, g), 32);
    }

    private static long necWord(int d, int s, int f, int g) {
        long w = 0;
        w |= (d & 0xFFL);
        w |= (s & 0xFFL) << 8;
        w |= (f & 0xFFL) << 16;
        w |= (g & 0xFFL) << 24;
        return w;
    }

    // ---------------------------------------------------------------- JVC

    /** JVC: 16 bits, D luego F, LSB primero. */
    public static Signal jvc(int d, int f) {
        long data = (d & 0xFFL) | ((f & 0xFFL) << 8);
        return pulseDistance(38000, 16 * 525, 8 * 525, 525, 525, 3 * 525, data, 16);
    }

    private static Signal pulseDistance(int carrier, int hdrMark, int hdrSpace,
                                        int mark, int zeroSpace, int oneSpace,
                                        long data, int bits) {
        int[] p = new int[2 + 2 * bits + 2];
        int i = 0;
        p[i++] = hdrMark;
        p[i++] = hdrSpace;
        for (int b = 0; b < bits; b++) {
            p[i++] = mark;
            p[i++] = ((data >> b) & 1L) != 0 ? oneSpace : zeroSpace;
        }
        p[i++] = mark;      // bit de parada
        p[i] = GAP_US;
        return new Signal(carrier, p);
    }

    // ---------------------------------------------------------------- Pioneer

    /** Pioneer: misma trama que NEC pero con portadora de 40 kHz. */
    public static Signal pioneer(int d, int s, int f) {
        return pulseDistance(40000, 16 * 564, 8 * 564, 564, 564, 1692, necWord(d, s, f, ~f), 32);
    }

    // ---------------------------------------------------------------- Aiwa

    /** Aiwa: D:8, S:5, ~D:8, ~S:5, F:8, ~F:8 (42 bits, LSB primero). */
    public static Signal aiwa(int d, int s, int f) {
        long data = (d & 0xFFL)
                | ((long) (s & 0x1F) << 8)
                | ((long) (~d & 0xFF) << 13)
                | ((long) (~s & 0x1F) << 21)
                | ((long) (f & 0xFF) << 26)
                | ((long) (~f & 0xFF) << 34);
        return pulseDistance(38000, 16 * 550, 8 * 550, 550, 550, 3 * 550, data, 42);
    }

    // ---------------------------------------------------------------- Panasonic / Kaseikyo

    /** Kaseikyo (Panasonic, Technics, Denon-K…): 6 bytes ya armados, LSB primero, 37 kHz. */
    public static Signal kaseikyo(int[] bytes) {
        long data = 0;
        for (int i = 0; i < 6; i++) data |= (long) (bytes[i] & 0xFF) << (8 * i);
        return pulseDistance(37000, 8 * 432, 4 * 432, 432, 432, 3 * 432, data, 48);
    }

    // ---------------------------------------------------------------- Denon / Sharp

    /**
     * Denon y Sharp: dos tramas de 15 bits sin cabecera, D:5, F:8, X:2 (LSB primero);
     * la segunda lleva ~F. Denon usa X = 0 y 3, Sharp X = 1 y 2.
     */
    public static Signal denonSharp(int d, int f, int x1, int x2) {
        List<Integer> out = new ArrayList<Integer>();
        denonFrame(out, d, f, x1);
        denonFrame(out, d, ~f, x2);
        return new Signal(38000, toArray(out));
    }

    public static Signal denon(int d, int f) { return denonSharp(d, f, 0, 3); }

    public static Signal sharp(int d, int f) { return denonSharp(d, f, 1, 2); }

    private static void denonFrame(List<Integer> out, int d, int f, int x) {
        int data = (d & 0x1F) | ((f & 0xFF) << 5) | ((x & 0x3) << 13);
        for (int b = 0; b < 15; b++) {
            out.add(264);
            out.add(((data >> b) & 1) != 0 ? 7 * 264 : 3 * 264);
        }
        out.add(264);
        out.add(165 * 264);
    }

    // ---------------------------------------------------------------- RCA

    /** RCA: D:4, F:8, ~D:4, ~F:8, MSB primero. 58 kHz (o 38 kHz en la variante RCA-38). */
    public static Signal rca(int d, int f, int carrier) {
        int data = ((d & 0xF) << 20) | ((f & 0xFF) << 12) | ((~d & 0xF) << 8) | (~f & 0xFF);
        List<Integer> out = new ArrayList<Integer>();
        for (int frame = 0; frame < 2; frame++) {
            out.add(8 * 460);
            out.add(8 * 460);
            for (int b = 23; b >= 0; b--) {
                out.add(460);
                out.add(((data >> b) & 1) != 0 ? 4 * 460 : 2 * 460);
            }
            out.add(460);
            out.add(16 * 460);
        }
        return new Signal(carrier, toArray(out));
    }

    // ---------------------------------------------------------------- Sony SIRC

    /**
     * Sony SIRC de 12 o 15 bits: 7 bits de comando + dirección (5 u 8 bits), LSB primero.
     * Se envía {@code repeats} veces con periodo de 45 ms (los televisores Sony lo exigen).
     */
    public static Signal sony(int d, int f, int bits, int repeats) {
        int dBits = bits - 7;
        long data = (f & 0x7FL) | ((long) (d & ((1 << dBits) - 1)) << 7);
        List<Integer> out = new ArrayList<Integer>();
        for (int r = 0; r < repeats; r++) {
            long sum = 0;
            out.add(2400);
            out.add(600);
            sum += 3000;
            for (int b = 0; b < bits; b++) {
                int m = ((data >> b) & 1L) != 0 ? 1200 : 600;
                out.add(m);
                out.add(600);
                sum += m + 600;
            }
            long pad = 45000 - sum;
            if (pad > 0) {
                int last = out.size() - 1;
                out.set(last, out.get(last) + (int) pad);
            }
        }
        return new Signal(40000, toArray(out));
    }

    // ---------------------------------------------------------------- Philips RC5

    /** RC5 (bifásico, 36 kHz). Dirección de 5 bits, comando de 7 bits (RC5 extendido). */
    public static Signal rc5(int d, int f, boolean toggle) {
        int[] bits = new int[14];
        int k = 0;
        bits[k++] = 1;                               // inicio
        bits[k++] = (f & 0x40) != 0 ? 0 : 1;         // campo (bit 7 del comando, invertido)
        bits[k++] = toggle ? 1 : 0;
        for (int b = 4; b >= 0; b--) bits[k++] = (d >> b) & 1;
        for (int b = 5; b >= 0; b--) bits[k++] = (f >> b) & 1;

        boolean[] levels = new boolean[28];          // true = ON
        for (int n = 0; n < 14; n++) {
            if (bits[n] == 1) {                      // 1 = OFF luego ON
                levels[2 * n] = false;
                levels[2 * n + 1] = true;
            } else {                                 // 0 = ON luego OFF
                levels[2 * n] = true;
                levels[2 * n + 1] = false;
            }
        }
        return new Signal(36000, mergeLevels(levels, 889));
    }

    /** Une niveles consecutivos iguales en duraciones; descarta el OFF inicial. */
    private static int[] mergeLevels(boolean[] levels, int unit) {
        List<Integer> out = new ArrayList<Integer>();
        int i = 0;
        while (i < levels.length && !levels[i]) i++;
        boolean cur = true;
        int run = 0;
        for (; i < levels.length; i++) {
            if (levels[i] == cur) {
                run += unit;
            } else {
                out.add(run);
                cur = levels[i];
                run = unit;
            }
        }
        out.add(run);
        if (out.size() % 2 == 1) {                   // terminó en ON -> añadir silencio
            out.add(GAP_US);
        } else {
            out.set(out.size() - 1, out.get(out.size() - 1) + GAP_US);
        }
        return toArray(out);
    }

    // ---------------------------------------------------------------- Philips RC6

    /** RC6 modo 0 (36 kHz, bifásico): 8 bits de dirección y 8 de comando, MSB primero. */
    public static Signal rc6(int address, int command) {
        List<Boolean> lv = new ArrayList<Boolean>();
        for (int i = 0; i < 6; i++) lv.add(true);      // líder: 6 unidades ON
        for (int i = 0; i < 2; i++) lv.add(false);     // y 2 unidades OFF
        rc6Bit(lv, 1, 1);                              // bit de inicio
        for (int i = 0; i < 3; i++) rc6Bit(lv, 0, 1);  // modo 0
        rc6Bit(lv, 0, 2);                              // bit de alternancia (doble ancho)
        for (int b = 7; b >= 0; b--) rc6Bit(lv, (address >> b) & 1, 1);
        for (int b = 7; b >= 0; b--) rc6Bit(lv, (command >> b) & 1, 1);
        boolean[] levels = new boolean[lv.size()];
        for (int i = 0; i < levels.length; i++) levels[i] = lv.get(i);
        return new Signal(36000, mergeLevels(levels, 444));
    }

    /** En RC6, 1 = ON luego OFF y 0 = OFF luego ON (al revés que RC5). */
    private static void rc6Bit(List<Boolean> lv, int bit, int width) {
        boolean first = bit == 1;
        for (int i = 0; i < width; i++) lv.add(first);
        for (int i = 0; i < width; i++) lv.add(!first);
    }

    // ---------------------------------------------------------------- Patrón crudo

    /** Patrón capturado tal cual (ON/OFF alternado empezando por ON). */
    public static Signal raw(int carrierHz, int[] pattern) {
        int[] p = pattern;
        if (p.length % 2 == 1) {
            p = new int[pattern.length + 1];
            System.arraycopy(pattern, 0, p, 0, pattern.length);
            p[pattern.length] = GAP_US;
        }
        return new Signal(carrierHz, p);
    }

    private static int[] toArray(List<Integer> list) {
        int[] a = new int[list.size()];
        for (int i = 0; i < a.length; i++) a[i] = list.get(i);
        return a;
    }
}
