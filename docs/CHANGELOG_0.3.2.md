# Rally GPS AI 0.3.2 — refuerzo de navegación y usabilidad

## Implementado en código
- Controles de planificación en dos filas, de 50 dp y con etiquetas claras.
- Durante GPS o DEMO no se muestra búsqueda, edición, GPX ni historial.
- Colores diferenciados para tramo recorrido y ruta pendiente, sin modificar las notas ni la geometría usada para navegar.
- Día/noche mediante estilos OpenFreeMap oficiales. No se ofrece falso modo satélite.
- GPS más conservador: descarta lecturas sin precisión, imprecisas, inválidas o antiguas. Requiere permiso de ubicación precisa.
- El GPX no se confunde con una ruta de calles de OSRM; el recálculo vial solamente tiene destino verificado.
- Bloqueo de cambios de origen, destino y reductores por pulsación larga mientras hay navegación activa.
- DEMO con **PAUSA/REANUDAR real**: conserva posición y anuncios; el coche avanza interpolando metros entre puntos, sin saltar entre vértices.
- El modo pausado conserva el Subaru sobre el recorrido simulado en vez de volver al GPS real.
- Pantalla encendida durante GPS o DEMO activa; al detener o pausar se libera el bloqueo.
- Búsqueda oculta el teclado al elegir destino; traducciones de opciones nuevas a es-AR/es-ES/en-US/pt-BR.
- Voz de reductor solo si no interfiere con curvas prioritarias; frases que aclaran que el dato fue *señalado en el mapa*.
- Pruebas JVM para UI, modelos de vehículos, idiomas, estilos de mapa, GPS, priorización vocal, geometría y parsing de Overpass.

## Límites explícitos
- Las notas de rally provienen de la geometría, no de la adherencia real, el estado de la calzada ni la visibilidad.
- Los reductores de OSM/manuales son datos orientativos. **No garantizan detectar todos**; algunos pueden estar mal ubicados o etiquetados.
- El servicio público OSRM/Overpass no garantiza disponibilidad o cobertura y el modo satélite sigue pendiente de un proveedor autorizado.
- El motor de ruta puede no reflejar cortes, obras o calles recientes. Revisá el mapa y respetá señalización real.
- Un recorrido GPX puede contener segmentos rectos que no representan calles; DEMO solo sigue su geometría, no convierte ese GPX en camino vial.
- Las pruebas automáticas no equivalen a comprobar pantalla, GPS, Bluetooth ni audio del teléfono.

## Versión
- Android package: ar.com.rallygpsai
- Version Code: 14
- Version Name: 0.3.2
- Release: pre-release hasta validación en hardware.
