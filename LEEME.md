# Salta Canciones (Android 8 o superior)

Con música sonando:
- Subir volumen 2 veces rápido: siguiente canción
- Bajar volumen 2 veces rápido: canción anterior

## Crear e instalar (Android Studio)
1. Descomprime el ZIP.
2. En Android Studio: Open > carpeta `SaltaCanciones`. Espera a que sincronice.
3. En el teléfono activa Opciones de desarrollador > Depuración por USB.
4. Conecta el teléfono y pulsa Run (▶).
5. Abre la app y activa las funciones que quieras.

Para obtener el archivo APK: Build > Build APK(s).

## Sin computadora potente (GitHub)
Sube el contenido de la carpeta (incluida `.github`) a un repositorio.
En la pestaña Actions, al terminar "Crear APK", descarga `SaltaCanciones-apk`.

## Notas
- Si el teléfono cierra la app, pon su batería en "Sin restricciones".
- Después de reiniciar el teléfono hay que volver a abrir la app.
- Mientras suena música, la app evita el volumen máximo y el cero (queda un paso
  antes) para poder detectar las pulsaciones.
- Muchos reproductores, con "anterior", primero reinician la canción actual;
  repite el gesto para ir a la anterior.
- Dos pasos rápidos para ajustar el volumen se toman como gesto. Para cambiar el
  volumen, mantén el botón apretado o pulsa despacio.

## Agitar
- Activa "Agitar para la linterna".
- Dos sacudidas fuertes encienden la linterna; otras dos la apagan.
- Consume más batería y puede activarse al correr o llevarlo en el bolsillo.
