CorreoPlus V1.0.5

- Barra inferior fija con favoritos, archivar, eliminar, responder, reenviar y menú Más.
- Menú Más: leído/no leído según estado, descargar contenido, ver HTML y archivar.
- Retirados los botones grandes del final del mensaje.
- Conserva adjuntos y destinatarios desplegables.

Compilación Android no verificada; probar con GitHub Actions.


## V1.0.6 - barra fija visible
Se corrige la pantalla de lectura: el contenido se desplaza dentro de un FrameLayout y la barra de acciones se ancla abajo, fuera del ScrollView. Se reserva espacio al final del contenido para que la barra no tape el mensaje.

Pendiente de compilación en GitHub Actions y prueba en un dispositivo Android.


## V1.0.7: barra de acciones visible
- Eliminado FrameLayout superpuesto; la barra ahora es hija directa del LinearLayout vertical, debajo del ScrollView con peso 1.
- Altura 76 dp; cada acción tiene icono y etiqueta, centrados, y ocupa una quinta parte de la anchura.
- Responder, Reenviar, Archivar, Eliminar y Más. Las opciones adicionales permanecen en Más.
- El contenido desplazable nunca tapa la barra.
- Requiere compilar y probar en Android; no se ha validado el APK en este entorno.
