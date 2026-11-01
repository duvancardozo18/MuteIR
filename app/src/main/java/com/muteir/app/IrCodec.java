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
        return pulseDistance(38000, 16 * 564, 8 * 564, 564, 564, 1692, necWord(d, s, f), 32);
    }

    /** NEC "extendido" con cabecera corta 4,5 ms / 4,5 ms (Samsung y similares). */
    public static Signal necx(int d, int s, int f) {
        return pulseDistance(38000, 8 * 564, 8 * 564, 564, 564, 1692, necWord(d, s, f), 32);
    }

    private static long necWord(int d, int s, int f) {
        long w = 0;
        w |= (d & 0xFFL);
        w |= (s & 0xFFL) << 8;
        w |= (f & 0xFFL) << 16;
        w |= ((~f) & 0xFFL) << 24;
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

    private static int[] toArray(List<Integer> list) {
        int[] a = new int[list.size()];
        for (int i = 0; i < a.length; i++) a[i] = list.get(i);
        return a;
    }
}
