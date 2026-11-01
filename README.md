# Mute IR

App Android que usa el emisor infrarrojo del teléfono (pensada para el Redmi Note 11) para enviar el comando de **silencio (mute)** a televisores y equipos de sonido.

## Cómo abrirla y probarla

1. Abre Android Studio → **Open** → elige esta carpeta `MuteIR`. Deja que sincronice Gradle (descargará lo que falte; si pide actualizar el plugin de Android Gradle, acepta).
2. En el Redmi Note 11: **Ajustes → Acerca del teléfono** → toca 7 veces "Versión de MIUI" → **Ajustes adicionales → Opciones de desarrollador** → activa **Depuración USB** e **Instalar vía USB**.
3. Conecta el teléfono y pulsa **Run ▶**. Para un APK suelto: **Build → Build APK(s)**.

## Cómo se usa

- **MUTEAR TODO**: envía en secuencia todos los códigos de mute (≈1 minuto). Puedes limitarlo a TV, Audio o Carro. Pulsa de nuevo para detener.
- **Una sola marca**: elige la marca y envía solo sus códigos (útil para probar en tu propio equipo).
- **Código propio**: para marcas que no estén en la lista. Elige protocolo e ingresa D, S y F.
- Apunta el borde superior del teléfono hacia los aparatos (hasta unos 8 m, con línea de vista).

## Límites que conviene saber

- Mute es un **interruptor** en la mayoría de equipos: si ya estaban en silencio, el sonido vuelve. Los códigos "mute ON" discretos (unos 15 en la tabla) se envían al final para que esos equipos queden siempre silenciados.
- Solo responden aparatos con control remoto infrarrojo. No afecta celulares, computadores ni parlantes Bluetooth.
- La tabla (326 códigos, 234 marcas y modelos) viene de dos bases públicas: IRDB y Flipper-IRDB. Incluye TV, receptores, barras de sonido, parlantes, reproductores de CD y unidades de carro (JVC, Kenwood, Pioneer, Dual, PowerAcoustik y otras). No hay garantía de que cubra cada modelo. Quedaron fuera los códigos Panasonic/Kaseikyo y RCA, que la app todavía no sabe generar. Para marcas que falten, usa **Código propio**.
- Un código puede hacer otra cosa en un aparato de otra marca. Úsala en tus propios aparatos o con permiso de quien los use.

## Añadir códigos

Edita `app/src/main/java/com/muteir/app/CodeBook.java` y agrega filas a `DATA`:

```
"Marca;T;NEC;D;S;F;T"
```

- `T` = televisor, `A` = audio (o `TA`).
- Tipos: `T` TV, `A` audio, `C` carro (se pueden combinar, p. ej. `TA`).
- Protocolos: `NEC`, `NECX`, `RC5`, `RC6`, `SONY12`, `SONY15`, `JVC`, `RAW`.
- Campo extra opcional: en NEC/NECX el cuarto byte si no es ~F; en RAW la lista de duraciones en microsegundos (y D es la portadora en Hz).
- D, S, F en decimal. En NEC, S = 255 − D si el subdispositivo es "-1".
- Última columna: `T` = alterna, `D` = mute ON discreto.

Más códigos: https://github.com/probonopd/irdb (carpeta `codes/<Marca>/<Tipo>/`, función MUTE) y https://github.com/Lucaslhm/Flipper-IRDB (archivos `.ir`, botón Mute).

## Estructura

- `IrCodec.java`: genera los patrones NEC, Samsung/NEC corto, RC5, RC6, Sony, JVC y crudos (sin dependencias de Android).
- `CodeBook.java`: tabla de códigos y selección por tipo o marca.
- `MainActivity.java`: interfaz y envío con `ConsumerIrManager`.
