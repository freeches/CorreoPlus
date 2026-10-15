# CorreoPlus V1.2 Beta — Motor de sincronización (primera entrega)

Base: V1.1.8 Beta. Una sola cuenta.

## Cambios implementados
- Progreso visible por carpeta, actualizado cada 10 mensajes procesados.
- Persistencia del último estado de sincronización en preferencias.
- Guardado incremental de cabeceras en lotes de 10 para reducir pérdidas tras cortes.
- Reintentos solo para errores de conexión transitorios (hasta 3).
- Diagnóstico IMAP consultable y copiable sin credenciales ni texto de mensajes.
- Actualización de estado leído/no leído desde servidor para la ventana sincronizada.
- Caché y lectura HTML de V1.1.8 conservadas.

## Pendiente para completar las 7 funciones del bloque de sincronización
IMAP IDLE real, cola offline de acciones, reconciliación completa de movimientos/borrados entre dispositivos y optimización de segundo plano. No se anuncian como implementadas.

## Validación
ZIP verificado; compilación Android pendiente de GitHub Actions.
