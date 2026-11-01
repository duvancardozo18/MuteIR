# Mute IR

App Android que usa el emisor infrarrojo del teléfono (pensada para el Redmi Note 11) para enviar el comando de **silencio (mute)** a televisores, equipos de sonido y audio de carro.

## Obtener el APK

Cada vez que se sube un cambio a este repositorio, GitHub Actions compila el APK: pestaña **Actions** → última ejecución de **Build APK** → **Artifacts** → `MuteIR-apk`.

También se puede abrir la carpeta en Android Studio y usar **Build → Build APK(s)**.

## Cómo se usa

La pantalla tiene un diseño oscuro con tarjetas y guarda tus ajustes entre usos.

- **Botón circular "Silenciar"**: envía los códigos de la categoría elegida. Mientras envía muestra un anillo con el porcentaje y el equipo actual; tócalo otra vez para detener. Arriba a la derecha, un indicador dice si el teléfono tiene emisor IR.
- **Qué silenciar**: chips para Todo, TV, Audio, Carro u Otros. Debajo se ve cuántos códigos son y cuánto tarda aproximadamente (TV ≈ 1,2 min, audio ≈ 2 min, carro ≈ 2 s, todo ≈ 6 min). Los códigos que comparten más marcas van primero.
- **Repetir automáticamente**: interruptor que despliega el intervalo. Ajusta con − y +, con los atajos (10 s, 30 s, 1 min, 5 min, 15 min) o tocando el número para escribir los segundos exactos (hasta 24 h). Entre ciclos, el botón muestra la cuenta regresiva.
- **Repetir sin reactivar el sonido** (activado por defecto): el primer ciclo envía todo y los siguientes solo los códigos que silencian sin alternar. Si lo desactivas, cada repetición vuelve a alternar el sonido de los equipos con mute de tipo interruptor.
- **Mi equipo**: busca tu marca con autocompletado o toca un atajo (LG, Samsung, Sony, JBL, Philips…). Muestra cuántos códigos tiene y envía solo esos, que es mucho más rápido. También respeta la repetición.
- **Código manual** (plegable): para marcas que no estén en la lista. Elige protocolo e ingresa D, S y F.
- Apunta el borde superior del teléfono hacia los aparatos (hasta unos 8 m, con línea de vista) y mantén la app abierta mientras envía.

## Límites que conviene saber

- En la mayoría de equipos mute es un **interruptor**: si ya estaban en silencio, el sonido vuelve. Los códigos "mute ON" discretos se envían al final de cada ciclo para que esos equipos queden siempre silenciados.
- Solo responden aparatos con control remoto infrarrojo. No afecta celulares, computadores ni parlantes que solo usan Bluetooth.
- No hay garantía de que la tabla cubra cada modelo. Para marcas que falten, usa **Código propio**.
- Un código puede hacer otra cosa en un aparato de otra marca. Úsala en tus propios aparatos o con permiso de quien los use.

## Fuentes de códigos

La tabla tiene 2.670 códigos distintos de unas 1.050 marcas, sin duplicados. Por marca: Sony 155, Samsung 45, LG 18, JBL 11 (cada código distinto suele cubrir muchos modelos; por ejemplo, casi todos los televisores LG usan el mismo código de mute).

| Fuente | Qué aporta |
|---|---|
| [IRDB](https://github.com/probonopd/irdb) | Todas las categorías con botón de mute (TV, receptores, amplificadores, CD, radio, etc.) |
| [Flipper-IRDB](https://github.com/Lucaslhm/Flipper-IRDB) | TV, barras de sonido, parlantes, receptores, CD, unidades de carro y sus colecciones convertidas de Pronto e IR Plus |
| [SmartIR](https://github.com/smartHomeHub/SmartIR) | Códigos Broadlink de TV, barras de sonido y receptores |
| [LIRC remotes](https://github.com/probonopd/lirc-remotes) | Espejo de la base de controles de LIRC, con códigos en formato Pronto |
| [irext](https://github.com/TanJunYi/database) | Base abierta (MIT) de TV y equipos de audio, decodificada con [irext-core](https://github.com/TanJunYi/core) |

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
- `MuteButton.java`: botón circular con anillo de progreso y cuenta regresiva.
- `Theme.java`: colores y estilos (chips, tarjetas, botones).
