# Rally GPS AI — Inicio guiado e idiomas (versión en desarrollo)

**Rama:** `feature/subaru-pacenotes-distance-ui`  
**Estado:** implementado en código; **sin compilar APK** ni fusionar con main, según lo acordado.

## Pantalla inicial (primera apertura)
- Ícono oficial Rally GPS AI, título de bienvenida y breve explicación.
- Se preselecciona el idioma del teléfono si está contemplado; en otros casos, español (Argentina).
- Idiomas: **Español (Argentina), Español (España), English, Português (Brasil)**.
- Casilla para activar o desactivar las notas habladas.
- Casilla para usar el Subaru azul o el marcador GPS clásico.
- Botón grande para guardar y comenzar. Solo tras finalizar el inicio se pide autorización GPS.
- Se guardan `app_language`, `onboarding_done`, `voice` y `subaru_marker` en preferencias privadas.
- No se repite cada vez que se abre la aplicación; **Ajustes > Idioma de la aplicación** permite volver a abrirlo.

## Traducciones y voz
- Etiquetas principales (buscar, destino, navegación, ajustes), menú de paradas y ajustes, distancia y direcciones de curvas se adaptan al idioma.
- El modo CarlFM grabado sigue disponible solo en español, porque no se dispone de permisos ni packs equivalentes en otros idiomas.
- En inglés y portugués, las notas se anuncian mediante TTS Android configurado en el idioma elegido. Depende de que el motor del teléfono tenga esa voz instalada.
- No se descargan voces sin consentimiento ni se incluye contenido de juegos comerciales.
- Las advertencias y ciertos mensajes avanzados heredados del motor todavía tienen texto en español; requieren revisión de localización completa.

## No se modifica
- Motor de rutas OSRM experimental y sus límites de servicio.
- Detector de curvas 1–6 y recálculo controlado ante desvíos.
- GPX, DEMO, mapa MapLibre, Subaru azul, notas rectangulares y voces autorizadas.

## Pruebas previstas antes de compilar
1. Primera apertura sin configuración: muestra bienvenida, idioma correcto y no solicita GPS antes de continuar.
2. Persistencia: segunda apertura entra directamente al mapa y conserva voz, Subaru e idioma.
3. Configuración desde Ajustes durante ruta: no descarta el trayecto ni convierte la distancia a próxima curva en `AHORA` por error.
4. Selección de inglés y portugués: TTS con voz del idioma elegido; CarlFM español deshabilitado para evitar mezcla.
5. Pruebas JUnit de normalización, idioma inicial, etiquetas y frases de copiloto.
6. Revisar pantalla con fuente grande, modo horizontal y navegación por accesibilidad.
7. Compilar, instalar, verificar permisos y probar GPS/DEMO **solo cuando el usuario autorice compilar**.

## Limitaciones conocidas
No es una localización exhaustiva de cada mensaje secundario del código antiguo; se traducen los controles principales y las notas habladas/visuales. Los valores de curva siguen siendo estimaciones a partir del mapa, no velocidades seguras.
