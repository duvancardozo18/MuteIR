package com.muteir.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tabla de códigos de silencio (mute) para televisores y equipos de sonido.
 *
 * Fuente: base de datos pública IRDB (github.com/probonopd/irdb), categorías
 * TV, receptores, amplificadores, barras de sonido y similares.
 *
 * Formato de cada fila:  marcas ; tipos ; protocolo ; D ; S ; F ; clase
 *   marcas    : una o varias marcas que usan ese mismo código, separadas por "/"
 *   tipos     : T = televisor, A = audio (puede traer ambos)
 *   protocolo : NEC, NECX, RC5, SONY12, SONY15, JVC
 *   D, S, F   : dispositivo, subdispositivo y función (comando) en decimal
 *   clase     : T = alterna silencio/sonido (toggle), D = "mute ON" discreto
 *
 * Para añadir códigos propios basta con agregar filas a DATA.
 */
public final class CodeBook {

    private CodeBook() {}

    public static final class Entry {
        public final String[] brands;
        public final boolean tv;
        public final boolean audio;
        public final boolean discrete;
        public final String proto;
        public final int d, s, f;

        Entry(String[] brands, boolean tv, boolean audio, boolean discrete,
              String proto, int d, int s, int f) {
            this.brands = brands;
            this.tv = tv;
            this.audio = audio;
            this.discrete = discrete;
            this.proto = proto;
            this.d = d;
            this.s = s;
            this.f = f;
        }

        public IrCodec.Signal signal() {
            if ("NEC".equals(proto)) return IrCodec.nec(d, s, f);
            if ("NECX".equals(proto)) return IrCodec.necx(d, s, f);
            if ("RC5".equals(proto)) return IrCodec.rc5(d, f, false);
            if ("SONY12".equals(proto)) return IrCodec.sony(d, f, 12, 3);
            if ("SONY15".equals(proto)) return IrCodec.sony(d, f, 15, 3);
            if ("JVC".equals(proto)) return IrCodec.jvc(d, f);
            throw new IllegalStateException("Protocolo desconocido: " + proto);
        }

        public String brandLabel() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < brands.length; i++) {
                if (i > 0) sb.append(" / ");
                sb.append(brands[i]);
            }
            return sb.toString();
        }
    }

    private static final String[] DATA = {
    "ADLER/Vivax;T;NEC;2;253;7;T",
    "ADS;A;NEC;0;255;89;T",
    "AMC;A;NEC;7;252;130;T",
    "ANITECH/Carver/Loewe/Magnavox/Philips;AT;RC5;0;0;13;T",
    "AOC;T;NEC;0;189;4;T",
    "Acesonic;A;NEC;5;122;75;T",
    "Adcom;A;NEC;26;229;81;T",
    "Adcom;A;NEC;26;229;95;T",
    "Adcom;A;NEC;26;229;130;T",
    "Adcom;A;NEC;26;232;86;D",
    "Aesthetix;A;RC5;16;0;7;T",
    "Aiwa/CIS BOX/Sony;A;SONY12;16;0;20;T",
    "Apple/iPort;A;NEC;1;222;17;T",
    "Aragon;A;RC5;31;0;38;T",
    "Arcam/Audio Control;A;RC5;16;0;13;D",
    "Arcam/Audio Control/Ayre/B.A.T/Electrocompaniet/Gryphon/Harman Kardon/Krell/Linn/Marantz/Myryad/Philips/Rega;A;RC5;16;0;13;T",
    "Arcam;A;RC5;16;0;119;D",
    "Arcam;A;RC5;16;0;119;T",
    "Arcam/Cambridge Audio/Meridian;A;RC5;19;0;13;T",
    "Architectural Audio;A;NECX;130;130;19;T",
    "Atlantic Technology/Fosgate;A;NEC;64;64;72;T",
    "Audio Access;A;NEC;133;83;143;T",
    "Audio Refinement;A;RC5;20;0;29;T",
    "Audio Research;A;RC5;7;0;27;T",
    "BnK Components;A;NEC;11;79;3;T",
    "BnK Components;A;NEC;27;78;3;T",
    "Bose;A;NEC;186;1;1;T",
    "Bose;A;NEC;186;75;1;T",
    "Bose;A;NEC;186;85;1;T",
    "Bose;A;NEC;186;133;1;T",
    "Bose;A;NEC;186;160;1;T",
    "Bose;A;NEC;186;161;1;T",
    "Bose;A;NEC;186;162;1;T",
    "Bose;A;NEC;186;163;1;T",
    "Bose;A;NEC;186;164;1;T",
    "Bose;A;NEC;186;165;1;T",
    "Bose;A;NEC;186;166;1;T",
    "Bose;A;NEC;186;167;1;T",
    "Bose;A;NEC;186;168;1;T",
    "Bose;A;NEC;186;169;1;T",
    "Bose;A;NEC;186;170;1;T",
    "Bose;A;NEC;186;171;1;T",
    "Bose;A;NEC;186;172;1;T",
    "Bose;A;NEC;186;173;1;T",
    "Bose;A;NEC;186;174;1;T",
    "Bose;A;NEC;186;176;1;T",
    "Bose;A;NEC;186;177;1;T",
    "Bose;A;NEC;186;178;1;T",
    "Bose;A;NEC;186;179;1;T",
    "Bose;A;NEC;186;180;1;T",
    "Bose;A;NEC;186;181;1;T",
    "Bose;A;NEC;186;182;1;T",
    "Bose;A;NEC;186;183;1;T",
    "Bose;A;NEC;186;184;1;T",
    "Bose;A;NEC;186;185;1;T",
    "Bose;A;NEC;186;186;1;T",
    "Bose;A;NEC;186;187;1;T",
    "Bose;A;NEC;186;188;1;T",
    "Bose;A;NEC;186;189;1;T",
    "Bose;A;NEC;186;190;1;T",
    "Bose;A;NEC;186;190;77;T",
    "Bose;A;NEC;186;191;1;T",
    "Bose;A;NEC;186;213;1;T",
    "Bose;A;NEC;186;229;1;T",
    "CIS BOX/Sony;T;SONY12;1;0;20;T",
    "CIS BOX;A;SONY12;17;0;20;T",
    "CIS BOX;A;SONY15;121;0;20;T",
    "Calypso;A;SONY12;17;0;56;D",
    "Cambridge Audio;A;NEC;192;63;0;T",
    "Cambridge Audio;A;RC5;16;0;75;T",
    "Cambridge Audio;A;RC5;16;0;102;D",
    "Carver;A;NEC;135;123;91;T",
    "Cary Audio Design;A;RC5;19;0;121;T",
    "Citation/Fosgate/Harman Kardon/JBL;A;NEC;132;66;80;T",
    "Classe Audio;A;RC5;25;0;13;T",
    "Classe Audio;A;RC5;25;0;33;T",
    "Coby;T;NEC;0;127;76;T",
    "Crestron/Imerge;A;RC5;14;0;11;T",
    "D-LINK;A;NEC;8;230;26;T",
    "D-LINK;A;NEC;18;37;30;T",
    "Emerson;T;NEC;135;34;243;T",
    "Fisher;A;NEC;54;200;92;T",
    "Fisher/Sanyo;T;NEC;56;199;24;T",
    "Gericom;T;NEC;3;252;3;T",
    "Harman Kardon;A;NEC;128;112;193;T",
    "Harman Kardon;A;NEC;134;118;42;T",
    "Hitachi;T;NEC;80;173;5;T",
    "Hitachi/Marantz/Yamaha;AT;NEC;80;175;11;T",
    "InFocus;T;NEC;7;248;145;T",
    "Insignia;T;NEC;134;5;14;T",
    "Integra;A;NEC;210;17;5;T",
    "Integra;A;NEC;210;18;132;D",
    "Integra;A;NEC;210;21;5;T",
    "Integra;A;NEC;210;22;132;D",
    "Integra;A;NEC;210;108;132;D",
    "Integra/Onkyo/Onkyo Integra;A;NEC;210;109;5;T",
    "Integra;A;NEC;210;109;204;T",
    "IntelliNet Controls/Sonance;A;NEC;0;90;7;T",
    "JBL/Lexicon;A;NEC;130;11;21;T",
    "JVC;AT;JVC;3;0;28;T",
    "JVC;A;JVC;163;0;28;T",
    "Kenwood/Sunfire;A;NEC;184;0;156;T",
    "Kenwood;A;NEC;184;71;156;T",
    "Kinergetics Research;A;RC5;28;0;32;T",
    "Krell;A;RC5;28;0;7;T",
    "LG;T;NEC;1;1;10;T",
    "LG/LXI/Memorex/Proton;T;NEC;4;251;9;T",
    "LG;A;NECX;44;44;31;T",
    "Lexicon;A;NEC;133;2;21;T",
    "Loewe;T;RC5;0;0;64;T",
    "Luxman;A;NEC;204;51;91;T",
    "Marantz/NEC;T;NEC;24;231;9;T",
    "Marantz;A;NEC;176;79;81;T",
    "Marantz;A;RC5;14;0;13;T",
    "Marantz;A;RC5;18;0;42;T",
    "Maxx;T;NEC;96;159;69;T",
    "McIntosh;A;NEC;202;85;70;T",
    "NAD;A;NEC;135;124;148;T",
    "NEC;T;NEC;4;251;10;T",
    "NEC;A;NEC;26;197;9;T",
    "Nakamichi;A;NEC;92;163;1;T",
    "Nakamichi;A;NEC;92;163;129;T",
    "Nakamichi;A;NEC;130;93;12;T",
    "Nakamichi;A;NEC;186;69;1;T",
    "Parasound;A;NEC;3;240;143;T",
    "Parasound;A;NEC;3;240;200;T",
    "Parasound;A;NEC;3;240;201;T",
    "Parasound;A;RC5;27;0;13;T",
    "Pioneer;A;NEC;165;90;81;D",
    "RME;A;NEC;18;52;46;T",
    "Samsung;T;NECX;7;7;15;T",
    "Sony;A;SONY12;16;0;32;T",
    "Sony;A;SONY15;48;0;20;T",
    "Theta Digital;A;RC5;16;0;8;T",
    "Toshiba;T;NEC;64;191;16;T",
    "Turtle Beach;A;NEC;1;249;20;T",
    "Velodyne;A;NEC;0;69;196;T",
    "Yamaha;T;NEC;80;175;5;T",
    "Yamaha;A;NEC;120;135;156;T",
    "Yamaha;A;NEC;120;135;162;D",
    "Yamaha;A;NEC;122;133;28;T",
    "Yamaha;A;NEC;122;133;220;T",
    "Yamaha;A;NEC;122;133;255;T",
    "Yamaha;A;NEC;125;130;114;T",
    "Yamaha;A;NEC;125;130;148;T",
    "Yamaha;A;NEC;126;129;38;D",
    "Yamaha;A;NEC;126;129;160;D",
    "Yamaha;A;NEC;126;129;162;D",
    "Yamaha;A;NEC;128;55;157;T",
    };

    private static List<Entry> cache;

    public static synchronized List<Entry> all() {
        if (cache == null) {
            List<Entry> list = new ArrayList<Entry>();
            for (String row : DATA) {
                String[] p = row.split(";");
                list.add(new Entry(
                        p[0].split("/"),
                        p[1].indexOf('T') >= 0,
                        p[1].indexOf('A') >= 0,
                        "D".equals(p[6]),
                        p[2],
                        Integer.parseInt(p[3]),
                        Integer.parseInt(p[4]),
                        Integer.parseInt(p[5])));
            }
            cache = Collections.unmodifiableList(list);
        }
        return cache;
    }

    /** Todas las marcas disponibles, ordenadas sin distinguir mayúsculas. */
    public static List<String> brands() {
        List<String> out = new ArrayList<String>();
        for (Entry e : all()) {
            for (String b : e.brands) {
                if (!out.contains(b)) out.add(b);
            }
        }
        Collections.sort(out, String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    /**
     * Códigos para enviar, primero los que alternan y al final los "mute ON" discretos,
     * así un equipo que tenga ambos termina siempre silenciado.
     */
    public static List<Entry> select(boolean tv, boolean audio, String brand) {
        List<Entry> toggles = new ArrayList<Entry>();
        List<Entry> discretes = new ArrayList<Entry>();
        for (Entry e : all()) {
            if (brand != null) {
                boolean match = false;
                for (String b : e.brands) {
                    if (b.equals(brand)) { match = true; break; }
                }
                if (!match) continue;
            }
            boolean typeOk = (tv && e.tv) || (audio && e.audio);
            if (!typeOk) continue;
            if (e.discrete) discretes.add(e); else toggles.add(e);
        }
        toggles.addAll(discretes);
        return toggles;
    }
}
