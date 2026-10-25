# Rally GPS AI 0.2.3 — Recálculo y voces

## Recálculo automático (rutas calculadas por OSRM)

- Mantiene el destino real separado de la búsqueda; conserva hasta cuatro paradas sin volver a las ya recorridas.
- Filtra errores GPS: necesita tres lecturas fiables (<30 m de precisión), 85 m o más fuera del trazado y 3 segundos de confirmación.
- Solicita nueva ruta en hilo de fondo desde ubicación GPS y reemplaza trazado/notas únicamente al finalizar exitosamente y si la sesión sigue activa.
- En caso de error de internet conserva ruta antigua y avisa. No repite consultas hasta pasar al menos 50 segundos.
- GPX, trazados de DEMO y navegación offline no recalculan; no hay motor offline de rutas todavía.
- OSRM demo es SOLO para prototipos no comerciales; aplicar backend propio antes de distribución pública.

## Voz offline seleccionable

- Android TextToSpeech; enumera voces `es` instaladas que no requieren conexión, prioriza español argentino y latino.
- Preajustes: Normal, Copiloto rápido, Copiloto intenso, Copiloto pausado; muestra de prueba.
- Configuración del motor de voz del sistema mediante Ajustes; el motor se reinicializa al volver a la app.
- `TextToSpeech.QUEUE_FLUSH` evita cola de anuncios obsoletos; AudioAttributes para instrucciones de navegación.
- Motores recomendados con descarga de voces: RHVoice, SherpaTTS. No incluir modelos de voz ni audios ajenos sin revisar cada licencia.

## Pruebas y limitaciones

- `kotlinc` y verificaciones offline del motor de notas, GUI de notas, cámara y estado de recálculo.
- Aún requiere compilación Android en CI y pruebas en el dispositivo (ubicación real, integración TTS, cambios de ruta, reconexión y permisos de Android).
