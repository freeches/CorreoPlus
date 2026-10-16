# CorreoPlus 2.0.14 Beta

- Tras enviar por SMTP, guarda una copia MIME íntegra en la carpeta Enviados mediante IMAP. Busca la carpeta especial `\\Sent` y nombres habituales. Si el envío funciona pero el guardado falla, informa de ambas situaciones por separado.
- IMAP IDLE cuando la aplicación está en primer plano y está activada la sincronización en segundo plano. Al recibir aviso de nuevos mensajes solicita una sincronización mediante WorkManager.
- Reconexión progresiva del observador IDLE después de fallos.
- **Limitación:** IMAP IDLE no se mantiene al cerrar o minimizar la aplicación. En segundo plano se mantiene la comprobación periódica existente (mínimo 15 minutos); por tanto no son notificaciones push permanentes.
- La copia en Enviados puede duplicarse si el proveedor ya guarda automáticamente una copia. No se garantiza deduplicación.

Proyecto para GitHub Actions. No se ha compilado localmente.
