# CorreoPlus V1.0.9 Beta

Base: V1.0.8, una sola cuenta.

- Botones «Más reciente» y «Más antiguo» al abrir un mensaje, usando el orden de la caché local (hasta los 100 mensajes visibles de la carpeta). No requieren nueva conexión IMAP.
- HTML adaptable: viewport, imágenes y contenido multimedia limitados al ancho de pantalla, texto largo ajustado, tablas con ancho máximo. Algunas tablas con anchuras rígidas todavía pueden necesitar zoom horizontal.
- Lectura individual de caché SQLite por clave, sin cargar toda la lista para abrir un mensaje.
- Se conserva la cabecera y la barra inferior fija, en vista normal y HTML.

## Instalación
Extraer el ZIP, copiar archivos al repositorio, MGit Stage / Commit / Push y comprobar GitHub Actions. No se ha probado la compilación ni la instalación Android en este entorno.
