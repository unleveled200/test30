# IPTV Player Pro (Android)

Proyecto inicial Android Studio en Kotlin con AndroidX Media3 / ExoPlayer.

## Incluye
- Reproductor de streams HTTPS y HLS/M3U8.
- Importación local de listas M3U/M3U8 mediante el selector de archivos del sistema.
- Lista de canales y selección para reproducir.
- Pantalla completa, controles de reproducción de Media3, volumen y mute.
- Medidor VU visual mediante Android Visualizer (requiere permiso RECORD_AUDIO opcional).
- Sin SDK de anuncios, analítica, telemetría ni permisos de almacenamiento.
- Tráfico HTTP en texto plano bloqueado por configuración; se recomienda usar HTTPS.

## Compilar
1. Abrí la carpeta `IPTVPlayerPro` en Android Studio reciente.
2. Permití que Gradle sincronice dependencias.
3. Ejecutá en un dispositivo Android o elegí Build > Build APK(s).

Requisitos: Android Studio compatible con Android Gradle Plugin 8.13, JDK 17 y Android SDK 36.

## Privacidad y seguridad
La app no incluye publicidad, cuentas, trackers ni backend propio. La lista se elige mediante el selector de documentos de Android y se procesa localmente. Solo se solicitan `INTERNET` para reproducir streams HTTPS y `RECORD_AUDIO` para el medidor VU opcional. El tráfico HTTP sin cifrar está bloqueado. El permiso VU no se solicita hasta que el usuario intenta activarlo durante la reproducción; el Visualizer analiza la salida de audio de la sesión del reproductor y no guarda grabaciones.

La reproducción de enlaces depende del proveedor y del formato. Usá únicamente listas y streams que tengas autorización para ver. Este proyecto es una base funcional, no una auditoría de seguridad ni una APK firmada/publicada.
