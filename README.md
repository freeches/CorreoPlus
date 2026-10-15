# CorreoPlus V0.8.2 — Indicadores de adjuntos

- Muestra 📎 en la lista de correos cuando la estructura MIME contiene adjuntos.
- Filtro «📎 Adjuntos» y compatibilidad con búsqueda local.
- Actualización de caché SQLite de versión 1 a 2 sin borrar mensajes.
- Los mensajes antiguos pueden no tener todavía el indicador: se detectan al abrirlos o al volver a descargarlos; el filtro solo incluye los adjuntos confirmados.
- Durante la sincronización se inspecciona la estructura MIME de los mensajes nuevos, lo que puede aumentar el tráfico IMAP.
- Conserva una sola cuenta y las funciones anteriores.

Compilar mediante GitHub Actions y comprobar en el móvil; no se ha validado una compilación local.
