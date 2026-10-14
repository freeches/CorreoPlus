# CorreoPlus V0.7-alpha — avance comprobable

Basado en CorreoPlus V0.6.1-preview. **No es la V0.7 completa.**

## Incorporado en este avance
- Firma de correo configurable desde Cuenta, añadida al enviar mensajes de texto.
- Preferencia de modo oscuro para las pantallas principales (no todos los controles nativos quedan adaptados).
- Se conserva caché, favoritos, filtros, búsqueda y credenciales de la versión anterior.

## No incorporado todavía
- Adjuntos entrantes y salientes, notificaciones, sincronización periódica, gestos, biometría y operaciones de mover/eliminar en servidor.
- OAuth2 para Gmail y Outlook.

## Verificación
No se ha compilado ni probado en Android. Mantén la V0.6.1 instalada hasta obtener una compilación verde y probar el APK. Esta versión no modifica el esquema de la base de datos.


## V0.7.1 — estabilidad IMAP
- Reconexión mediante nueva sesión IMAP y un reintento tras 1,2 segundos ante cortes temporales.
- Cierre defensivo de carpetas y conexiones incluso ante errores.
- Ante fallo temporal persistente se conserva y muestra la caché sin diálogo modal.
- No incluye aún adjuntos ni notificaciones.
- La compilación y la prueba real con el servidor deben verificarse en GitHub Actions y Android.
