# CorreoPlus V0.3 — caché local experimental

## Cambios
- Guarda cabeceras en SQLite por cuenta, carpeta, UIDVALIDITY y UID.
- Muestra al abrir la app los últimos mensajes previamente sincronizados, sin esperar a IMAP.
- Sincroniza los 50 últimos encabezados al conectar y descarga contenido de hasta 20 mensajes recientes para lectura sin conexión.
- No elimina cuerpos ya guardados al refrescar los encabezados.
- Permite volver a descargar el cuerpo de un mensaje al abrirlo.

## Limitaciones importantes
- La contraseña NO se almacena: después de cerrar completamente la aplicación es necesario volver a introducir los datos de conexión para sincronizar o enviar. La bandeja en caché sí se puede consultar sin conexión.
- La caché SQLite es privada de la aplicación, pero **no está cifrada**. No usar en un dispositivo compartido o comprometido.
- No sincroniza eliminaciones remotas ni carpetas completas, y no ejecuta tareas periódicas en segundo plano.
- Los cuerpos de mensajes solo se guardan para los 20 recientes o los abiertos con conexión. No incluye adjuntos offline.
- OAuth2 Gmail/Outlook aún no está implementado.
- No se ha validado la compilación en GitHub Actions ni probado en un teléfono; se entrega como versión de prueba.

## Instalación
Sustituir los archivos del proyecto en GitHub/MGit, commit y push a main; comprobar Actions y descargar APK si compila correctamente.
