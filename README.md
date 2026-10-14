# CorreoPlus V0.5 — credenciales cifradas (versión de prueba)

Base V0.4, con diseño y caché existentes. Añade guardado **opcional** de la contraseña IMAP/SMTP mediante AES-GCM y clave no exportable de Android Keystore. Al abrir, si hay contraseña guardada, recupera los ajustes IMAP/SMTP y sincroniza. También permite eliminar la contraseña guardada.

## Limitaciones importantes

- Esta versión mantiene **una cuenta activa**. No incluye todavía bandeja multicuenta ni OAuth2 para Gmail/Outlook.
- No incorpora adjuntos ni notificaciones; la sincronización sigue examinando hasta 100 mensajes recientes, no es una implementación IMAP QRESYNC completa.
- La **caché SQLite no está cifrada**. Las contraseñas sí se cifran si activas la opción.
- No se ha podido ejecutar una compilación Android ni pruebas en dispositivo aquí. Conserva V0.4 hasta confirmar la compilación y la actualización.
- En la primera ejecución de V0.5 introduce la contraseña y marca la casilla si deseas recordarla. Si la clave de Keystore deja de estar disponible, vuelve a introducirla.

## Instalación

Sube el contenido del proyecto al repositorio CorreoPlus (incluida `.github`), commit/push a `main`, y comprueba GitHub Actions. Instala el APK sobre V0.4, sin desinstalar, para preservar la caché.
