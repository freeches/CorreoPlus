# CorreoPlus V1.1.1 Beta

Proyecto Android de una sola cuenta basado en V1.1 Beta.

## Novedades
- Responder desde la barra inferior, con el mensaje original completo citado debajo de la respuesta.
- Responder a todos desde «Más», con destinatarios Reply-To en Para y demás destinatarios originales en CC, sin duplicados ni la cuenta propia.
- Reenviar con cabeceras del mensaje original y el cuerpo completo citado.
- Campo CC editable en la redacción y envío SMTP.
- Cabeceras In-Reply-To y References al responder.
- Se conserva el editor de formato, adjuntos manuales, HTML, caché, filtros, acciones y sincronización.

## Limitaciones importantes
- La cita del mensaje original usa la representación en texto del MIME original. Se envía una alternativa HTML generada a partir de ese texto, pero **no conserva exactamente el HTML original** (tablas, estilos e imágenes incrustadas).
- El reenvío no adjunta automáticamente los archivos originales: hay que añadirlos manualmente.
- La composición de respuesta requiere conexión IMAP para consultar las cabeceras y el contenido originales.
- No se ha compilado ni probado el APK en Android en este entorno. Comprobar GitHub Actions antes de instalar.

## Instalación
Extraer ZIP, copiar contenidos al repositorio, MGit Stage → Commit → Push y descargar APK del workflow de GitHub Actions.


## V1.1.3 — Vista HTML y respuestas
- HTML con desplazamiento interno hasta el final, incluso en mensajes largos.
- En Cuenta: casilla para elegir HTML por defecto o texto plano.
- Acción «A todos» visible en la barra inferior; también disponible en Más.
- HTML requiere conexión IMAP. Las vistas sin conexión siguen mostrando el texto en caché.
- Conserva la barra fija, el encabezado y los adjuntos desplegables.
