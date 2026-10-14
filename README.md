# CorreoPlus v0.1 — prototipo

## Implementado
- Configuración temporal de un servidor IMAP seguro (IMAPS 993).
- Lectura de los asuntos, remitentes y fechas de los últimos 15 correos de INBOX.
- Redacción y envío mediante SMTP STARTTLS (587).
- Las credenciales permanecen solo en memoria mientras la actividad está abierta; no se guardan en disco.
- GitHub Actions para compilar APK sin ordenador.

## No implementado todavía
- OAuth2 de Gmail y Microsoft/Outlook: **no funciona el inicio de sesión normal de estas cuentas**. Se necesita registrar aplicaciones OAuth, obtener client IDs y usar autorización segura.
- Múltiples cuentas persistentes, vista de cuerpo de mensajes, adjuntos, búsqueda, carpetas, notificaciones, modo oscuro propio y sincronización en segundo plano.
- Compatibilidad con todos los proveedores y puertos.

## Seguridad y limitaciones
- Usa TLS para IMAP y exige STARTTLS para SMTP.
- No pegues contraseñas en GitHub ni en el chat.
- No se ha compilado ni probado en un dispositivo en este entorno.
- La aplicación es un prototipo de aprendizaje, no un cliente de correo listo para producción.

## Compilar desde Android
Descomprime y sube el contenido de esta carpeta a la raíz de un repositorio GitHub con MGit. Haz commit y push a main. En Actions descarga el artefacto CorreoPlus-APK del flujo Build CorreoPlus APK.
