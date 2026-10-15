# CorreoPlus V1.0 Beta

Actualización desde V0.9 Beta. Una sola cuenta IMAP/SMTP.

## Novedades
- Pulsación larga sobre un mensaje para iniciar selección múltiple.
- Selección múltiple: marcar leído/no leído, archivar o mover a Papelera.
- Deslizamiento horizontal en la lista: derecha para archivar, izquierda para eliminar (con confirmación).
- Marcar leído/no leído desde la vista del mensaje.
- Mejoras visuales en filas, espaciado y selección.

## Nota
El rendimiento con miles de mensajes sigue limitado por la consulta de 100 mensajes de la caché. No se ha implementado todavía paginación ni carga incremental. Los cambios de leído/no leído requieren conexión. No se ha probado la compilación en Android en este entorno.

## Instalación
Extraer el ZIP en el repositorio de GitHub, hacer Stage/Commit/Push con MGit y descargar el APK generado por GitHub Actions. Mantener una copia de la V0.9.


## V1.0.1
- Gestos horizontales corregidos: derecha archivar, izquierda Papelera. Pulsación prolongada selecciona.
- Bandeja en dos líneas: remitente; asunto y fecha/hora. Hoy HH:mm, últimos 6 días día de semana, anteriores dd/MM/yyyy.
- Sin cambios en la base de datos ni en las credenciales.


## V1.0.2
Muestra De, todos los Para, CC y CCO cuando el servidor incluye esa cabecera. Se consultan los destinatarios al abrir el correo, por lo que requieren conexión. En mensajes recibidos normalmente no se conoce CCO.
