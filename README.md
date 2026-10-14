# CorreoPlus V0.4 — mejora incremental de la caché

## Incorporado
- Búsqueda local sin conexión por remitente y asunto (hasta 100 resultados por carpeta).
- Fecha y hora de la última sincronización IMAP exitosa por cuenta y carpeta.
- Conserva la caché de la V0.3, la interfaz y la lectura offline.
- Botón de sincronización manual.

## No implementado aún
- Guardado persistente de contraseñas y cuentas con protección criptográfica. Se conserva el comportamiento seguro de no almacenar contraseñas; para volver a sincronizar tras cerrar la app hay que introducirlas.
- Sincronización incremental completa: todavía se consultan las 50 cabeceras recientes para actualizar estados y se descargan cuerpos ausentes entre los 20 recientes. Los cuerpos existentes no se vuelven a descargar.
- Eliminaciones remotas, OAuth2, notificaciones, adjuntos offline y sincronización periódica.

## Seguridad y validación
La base de datos SQLite contiene correos en texto legible para la aplicación, no cifrado de extremo a extremo. No se ha ejecutado GitHub Actions ni probado en un teléfono. Guarda la V0.3 hasta comprobar esta versión.

## Instalación
Descomprime y sustituye los archivos en MGit/GitHub, stage, commit y push a `main`; revisa GitHub Actions.
