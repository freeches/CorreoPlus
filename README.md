# CorreoPlus V0.2 — prototipo Outlook-inspired

## Incluido
- Bandeja con hasta 30 mensajes por página, remitente, asunto, fecha y estado leído.
- Lectura de mensajes de texto y conversión simple de HTML a texto.
- Decodificación MIME de encabezados.
- Navegación por carpetas IMAP disponibles.
- Respuesta, reenvío y composición básica.
- IMAP 993 TLS directo o 143 STARTTLS obligatorio.
- SMTP 465 TLS directo o 587 STARTTLS obligatorio.
- Diseño de navegación azul inspirado en Outlook (no es una copia de Microsoft).

## Limitaciones importantes
- Una cuenta por sesión. No almacena credenciales; vuelve a introducirlas al reiniciar.
- No incluye OAuth2 Gmail/Outlook, adjuntos, notificaciones, búsqueda, paginación completa ni borradores.
- Las respuestas y reenvíos no preservan adjuntos ni cabeceras de hilo.
- No admite contraseña SMTP distinta de la contraseña IMAP.
- Solo se ha verificado estructura del proyecto; la compilación en GitHub Actions y pruebas en dispositivo están pendientes.
- No usar como cliente de correo principal todavía. Evita introducir credenciales de cuentas sensibles hasta realizar auditoría de seguridad.

## Instalación
Subir el contenido del ZIP (incluida la carpeta oculta `.github`) al repositorio. Commit en main y consultar GitHub Actions. El flujo genera un APK de depuración cuando la compilación funciona.
