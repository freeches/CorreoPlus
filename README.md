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


## V1.1.4 Beta — redacción HTML
- La preferencia `default_html` controla el formato inicial del editor en Redactar, Responder, Responder a todos y Reenviar.
- Casilla por mensaje para cambiar entre HTML y texto plano.
- Editor enriquecido Android con negrita, cursiva, subrayado y enlaces; conserva los estilos compatibles del HTML original en respuestas.
- En HTML se envía MIME multipart/alternative; en plano solo text/plain.
- Limitación: Android Html.fromHtml no conserva todo el CSS, tablas ni imágenes remotas de los correos originales.


## V1.1.5 Beta: sincronización inicial gradual
- IMAP sincroniza primero 50 cabeceras, y amplía la ventana en 50 al sincronizar de nuevo.
- Guarda cabeceras en transacciones de 25, para conservar progreso ante desconexiones.
- Evita descargar el MIME de adjuntos y los cuerpos de 20 mensajes durante la sincronización inicial; se obtienen al abrir un mensaje.
- Timeout de lectura IMAP aumentado a 45 segundos.
- No cambia la configuración de la cuenta ni el editor HTML.
- Limitación: el icono de adjuntos puede aparecer tras abrir el mensaje, y el contador refleja los mensajes guardados, no el total del servidor.


## V1.1.6 Beta
- Preferencias independientes: imágenes incrustadas CID (activadas) e imágenes remotas (desactivadas).
- Vista HTML: CID locales embebidos como data URI; imágenes remotas HTTPS solo con consentimiento.
- Respuestas/reenvíos HTML: conservan el fragmento HTML original y adjuntan imágenes CID como multipart/related.
- Límite de 2 MB por imagen incrustada. El editor Android muestra una aproximación editable; el original enviado se conserva en HTML.
- Sin cambios en la sincronización por bloques de V1.1.5.


## V1.1.7 Beta — navegación HTML
- La vista HTML tiene controles «Inicio HTML» y «Final HTML» para saltar al comienzo o al final incluso en mensajes largos.
- Se refuerza el control de gestos para que el ScrollView exterior no intercepte el desplazamiento del WebView.
- Se amplía el espacio de lectura HTML al 72 % de la altura de pantalla.
- Se mantienen las funciones de V1.1.6.


## V1.1.8 Beta
Corrección de lectura HTML: visor más corto para evitar recorte en el ScrollView exterior, margen final dentro del documento HTML, separación bajo el visor y botón Final HTML que salta a la posición final real. La barra de acciones continúa fija y separada del ScrollView.
