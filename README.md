## CorreoPlus 2.0.5 Beta

- Diagnóstico de sincronización automática con fase y progreso de encabezados.
- Historial local de las últimas 12 comprobaciones automáticas, sin contraseñas ni contenidos.
- Botón para solicitar una sincronización inmediata mediante WorkManager, respetando restricciones de red y batería.
- Actualización manual del panel de diagnóstico y copia de historial.
- El trabajo inmediato puede retrasarse según Android y conectividad; no es IMAP IDLE.
- Compilación APK no verificada en este entorno.

## CorreoPlus 2.0.4 Beta

- Búsqueda avanzada por texto en cuerpo de mensajes descargados, combinable con remitente, asunto, fecha y adjuntos.
- Filtro de mensajes no leídos en búsqueda avanzada.
- Consultas SQL parametrizadas y términos LIKE escapados.
- Los cuerpos aún no descargados no se pueden buscar sin conexión.

# CorreoPlus 2.0 — Beta ampliada

Basado en CorreoPlus V2.0 Beta. Mantiene una sola cuenta.

## Mejoras añadidas en esta entrega
- Búsquedas guardadas por cuenta, accesibles desde la bandeja.
- Tres firmas configurables: principal, respuestas y reenvíos.
- Guardado automático de borradores locales al editar, recuperación al iniciar una redacción nueva y limpieza al enviar correctamente.

## Funciones que ya existían
Densidad de bandeja, avatares con iniciales, gestos configurables, HTML, imágenes CID, responder a todos, sincronización por bloques y diagnósticos.

## Limitaciones honestas
- Los borradores se guardan en preferencias locales y no en la carpeta IMAP Borradores; solo hay un borrador local recuperable por cuenta y no conserva adjuntos ni formato enriquecido.
- Las firmas adicionales son texto convertido a HTML básico, no un editor completo de firmas HTML.
- No se han implementado aún las 24 funciones aprobadas; IMAP IDLE, cola sin conexión, contactos, protección antifraude y más siguen pendientes.
- No se ha verificado la compilación en un SDK Android. Comprobar GitHub Actions antes de instalar.


## Ampliación V2.0 (siguiente beta)
- Cuatro colores de acento persistentes para avatares y elementos que usan el color principal.
- Confirmación del destino real antes de abrir enlaces HTTP, HTTPS o mailto desde HTML.
- Aviso reforzado para enlaces HTTP, punycode (xn--) y URLs con arroba o direcciones IP. Son señales heurísticas, no una detección de phishing infalible.
- Se conserva el proyecto previo sin modificar el motor de sincronización.

Compilación Android no verificada en este entorno.


## CorreoPlus 2.0.1 Beta
- Numeración correlativa: 2.0.1 (versionCode 25).
- Barra de accesos rápidos configurable desde Ajustes: mostrar/ocultar Bandeja, Diagnóstico y Redactar.
- Tres órdenes de accesos rápidos disponibles, guardados localmente.
- Carpetas y Cuenta permanecen accesibles.
- Compilación Android pendiente de verificar en GitHub Actions.


## 2.0.2 Beta
- Agenda local de direcciones por cuenta, con añadir, seleccionar y eliminar.
- Registro automático de destinatarios al enviar correctamente un mensaje.
- Sugerencias de destinatarios guardados al enfocar Para o CC cuando hay texto parcial.
- Sin permiso de lectura de contactos del teléfono ni subida de la agenda.

## 2.0.3 Beta
- Bloqueo de recursos de seguimiento por dominios conocidos y rutas habituales de píxeles de apertura.
- El bloqueo se aplica incluso cuando se permiten imágenes remotas.
- Interruptor independiente en Ajustes, activado de forma predeterminada.
- Mantiene el bloqueo previo de recursos HTTP y las confirmaciones de enlaces externos.
- Protección heurística: no garantiza detectar todos los píxeles de seguimiento.

## V2.0.7 Beta
- Corregida la compilación de búsqueda avanzada: casilla local «Solo mensajes no leídos» declarada en su pantalla.


## CorreoPlus 2.0.8 Beta
- Menú de navegación compacto: Carpetas, Bandeja, Redactar y Cuenta accesibles desde ☰.
- Eliminados los cuatro botones grandes de la parte superior de la bandeja.
- Eliminada la línea redundante de mensajes guardados en la bandeja despejada.
- Restauradas las etiquetas bajo los iconos de acciones del lector, manteniendo la lectura despejada.
