# CorreoPlus 2.0.13 Beta

## Notificaciones mejoradas
- Muestra remitente y asunto en la notificación individual, con texto expandible.
- Al tocar un aviso individual, abre directamente el mensaje descargado en caché; en avisos agrupados abre INBOX.
- Mantiene un único aviso actualizado para evitar acumulación.
- Canal Android «Correos nuevos» configurable desde ajustes del sistema.
- Respeta el permiso de notificaciones en Android 13+ y las restricciones de WorkManager.
- No notifica como nuevos los mensajes anteriores durante la primera sincronización.

**Limitación:** comprobaciones periódicas de WorkManager (mínimo 15 minutos), no push instantáneo/IMAP IDLE. Compilación de APK pendiente de validar en GitHub Actions.
