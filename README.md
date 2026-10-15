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
