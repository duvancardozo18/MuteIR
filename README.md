# Mute IR

App Android que usa el emisor infrarrojo del teléfono (pensada para el Redmi Note 11) para enviar el comando de **silencio (mute)** a televisores, equipos de sonido y audio de carro.

## Obtener el APK

Cada vez que se sube un cambio a este repositorio, GitHub Actions compila el APK: pestaña **Actions** → última ejecución de **Build APK** → **Artifacts** → `MuteIR-apk`.

También se puede abrir la carpeta en Android Studio y usar **Build → Build APK(s)**.

## Cómo se usa

- **Qué silenciar**: Todo, solo TV, solo audio (equipos, barras, parlantes), solo audio de carro, u otros sin clasificar.
- **MUTEAR TODO**: envía los códigos en secuencia, empezando por los que comparten más marcas, así los primeros segundos cubren los equipos más comunes. Tiempos aproximados: TV ≈ 25 s, audio ≈ 1,5 min, carro ≈ 2 s, todo ≈ 4 min. Pulsa de nuevo para detener.
- **Repetir**: marca "Repetir hasta que lo detenga" y escribe cada cuántos segundos repetir (de 1 s a 24 h). Muestra el ciclo y la cuenta regresiva. La app debe seguir abierta y en pantalla.
- **Solo "mute ON" en repeticiones** (marcada por defecto): el primer ciclo envía todo y los siguientes solo los códigos que silencian sin reactivar el sonido. Si la desmarcas, cada repetición vuelve a alternar el sonido de los equipos con mute de tipo interruptor.
- **Una sola marca**: envía solo los códigos de la marca elegida (también admite repetición).
- **Código propio**: para marcas que no estén en la lista. Elige protocolo e ingresa D, S y F.
- Apunta el borde superior del teléfono hacia los aparatos (hasta unos 8 m, con línea de vista).

## Límites que conviene saber

- En la mayoría de equipos mute es un **interruptor**: si ya estaban en silencio, el sonido vuelve. Los códigos "mute ON" discretos se envían al final de cada ciclo para que esos equipos queden siempre silenciados.
- Solo responden aparatos con control remoto infrarrojo. No afecta celulares, computadores ni parlantes que solo usan Bluetooth.
- No hay garantía de que la tabla cubra cada modelo. Para marcas que falten, usa **Código propio**.
- Un código puede hacer otra cosa en un aparato de otra marca. Úsala en tus propios aparatos o con permiso de quien los use.

## Fuentes de códigos

La tabla tiene 1.826 códigos distintos de unas 720 marcas, sin duplicados:

| Fuente | Qué aporta |
|---|---|
| [IRDB](https://github.com/probonopd/irdb) | Todas las categorías con botón de mute (TV, receptores, amplificadores, CD, radio, etc.) |
| [Flipper-IRDB](https://github.com/Lucaslhm/Flipper-IRDB) | TV, barras de sonido, parlantes, receptores, CD, unidades de carro y sus colecciones convertidas de Pronto e IR Plus |
| [SmartIR](https://github.com/smartHomeHub/SmartIR) | Códigos Broadlink de TV, barras de sonido y receptores |

Quedaron fuera los protocolos que la app aún no genera (RECS80, MCE, Mitsubishi, Proton y otros menos comunes).

## Añadir códigos

Edita `app/src/main/java/com/muteir/app/CodeBook.java` y agrega filas a `DATA`:

```
"Marca;A;NEC;D;S;F;T"
```

- Tipos: `T` TV, `A` audio, `C` carro, `O` otros (se pueden combinar, p. ej. `TA`).
- Protocolos: `NEC`, `NECX`, `PIONEER`, `RC5`, `RC6`, `SONY12`, `SONY15`, `SONY20`, `JVC`, `AIWA`, `DENON`, `SHARP`, `RCA`, `RCA38`, `KASE` (Panasonic/Kaseikyo), `RAW`.
- D, S, F en decimal. En NEC, S = 255 − D si el subdispositivo es "-1".
- Penúltima columna: `T` = alterna, `D` = mute ON discreto.
- Campo extra opcional: en NEC/NECX el cuarto byte si no es ~F; en RAW la lista de duraciones en microsegundos (y D es la portadora en Hz); en KASE los 6 bytes en hexadecimal.

## Estructura

- `IrCodec.java`: genera las señales de cada protocolo (sin dependencias de Android).
- `CodeBook.java`: tabla de códigos y selección por tipo o marca.
- `MainActivity.java`: interfaz, repetición y envío con `ConsumerIrManager`.
