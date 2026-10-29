# PRAYO — reproductor personal Android

Proyecto Android nativo inicial de PRAYO, con interfaz oscura minimalista inspirada en reproductores premium.

## Funciones incluidas
- Lee archivos de audio locales del teléfono (requiere permiso).
- Busca canciones por título o artista.
- Reproduce, pausa, anterior y siguiente.
- Funciona sin conexión y no requiere cuenta.

## Compilar el APK

### Opción A: GitHub Actions (sin instalar Android Studio)
1. Crea un repositorio nuevo en GitHub.
2. Sube el contenido completo de esta carpeta al repositorio (la carpeta `.github` también).
3. Abre la pestaña **Actions** y ejecuta **Build PRAYO APK** (o espera al build automático tras subir a `main`).
4. En la ejecución terminada, descarga el artefacto **PRAYO-Android-APK**.
5. Extrae el ZIP del artefacto y abre `app-debug.apk` en tu Android para instalarlo. Puede pedirte permitir instalación desde esa fuente.

### Opción B: Android Studio
Abre esta carpeta como proyecto Gradle y ejecuta Build > Build APK(s). Requiere Android SDK y JDK 17.

## Nota
Esta es una primera versión funcional del reproductor local. La reproducción se detiene al cerrar la aplicación; todavía no incluye reproducción en segundo plano, carátulas incrustadas, playlists persistentes ni ecualizador. El diseño mostrado anteriormente era una propuesta visual, no una app ya compilada.

## Reproducción en segundo plano
La versión incluye `PlaybackService`, un servicio Android en primer plano de tipo `mediaPlayback`. Al iniciar una canción muestra una notificación persistente con el nombre de la canción y un control para pausar/reanudar. La reproducción continúa al salir de PRAYO o apagar la pantalla. Android puede detener la reproducción si el usuario fuerza la detención de la app desde Ajustes.
