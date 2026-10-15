# CorreoPlus V0.9 Beta

Basado en V0.8.3.1. Sigue siendo una sola cuenta.

## Nuevas funciones
- Sincronización IMAP periódica con WorkManager (mínimo 15 minutos, Android puede retrasarla).
- Notificación de nuevos mensajes no leídos desde la última sincronización de referencia; no se notifican los correos antiguos en el primer ciclo.
- Interruptor de sincronización y notificaciones en Cuenta. Requiere contraseña recordada cifrada con Android Keystore.
- Archivar y mover a Papelera desde el mensaje, con confirmación. Se usan carpetas IMAP existentes (Archive/Archives/Archivo, Trash/Deleted Items/Papelera); si no existen, se avisa y no se mueve nada.

## Limitaciones
- Beta no compilada en este entorno; verificar GitHub Actions y pruebas reales.
- No hay sincronización instantánea ni garantía de ejecución cada 15 minutos.
- No se han añadido todavía gestos ni editor HTML avanzado.
- Si Android restringe notificaciones o batería, puede retrasar o impedir avisos.
- Antes de instalar, conserva una copia de la V0.8.3.1.

## Instalación
Descomprime el ZIP en el repositorio y usa MGit: Stage, Commit, Push. Descarga el APK de GitHub Actions si la compilación finaliza correctamente.
