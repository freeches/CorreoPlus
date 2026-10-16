# CorreoPlus 2.0.15 Beta

- Copias en Enviados con marca IMAP Seen.
- Sincronización de 25 encabezados recientes.
- Revisión por lotes de las marcas leído/no leído de todos los mensajes de la caché.
- Paginación de bandeja en grupos de 25.
- Caché local y avisos IMAP IDLE anteriores conservados.

Nota: el historial remoto anterior al contenido de la caché no se importa automáticamente; no se eliminan mensajes locales al no encontrar un UID durante esta revisión.


## 2.0.16 Beta
- Evita bloquear el hilo principal al cerrar conexiones IMAP IDLE durante pausas y cambios de pantalla.
- Evita iniciar simultáneamente más de un observador IDLE en la misma actividad.
- Conserva la comprobación de lectura de todos los mensajes locales.


## V2.0.17 Beta
- Encabezado de carpeta dinámico, sin duplicación de Recibidos.
- Ocultos botones inferiores de paginación en modo despejado; navegación disponible en menú de opciones.


## V2.0.18 Beta
La bandeja abierta se refresca automáticamente tras sincronizar WorkManager, conservando su desplazamiento; al volver a la aplicación comprueba si la caché cambió. No interrumpe la lectura de un mensaje.
