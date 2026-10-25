# Rally GPS AI 🏁

Aplicación Android **nueva e independiente**, inspirada en funciones de Sideways y Rally SatNav. No contiene código, recursos visuales ni audios propietarios de esas aplicaciones. Paquete: `ar.com.rallygpsai`. **Estado: etapas 1–8, versión experimental 0.2.3.**

## Incluido en esta entrega

- Mapa oscuro de MapLibre / OpenFreeMap con vista 2D e inclinación tipo 3D (no edificios 3D).
- GPS Android, origen manual o automático, destino por Nominatim o pulsación larga; hasta cuatro paradas intermedias desde PUNTOS.
- Ruta de auto desde ubicación actual con motor OSRM demo **solo para desarrollo y uso personal no comercial**, sin garantías de disponibilidad.
- Algoritmo Kotlin para notas 1–6 con remuestreo 5 m, horquillas, modificadores estimados, colores, etiquetas D/I y curvas enlazadas.
- TTS en español, avisos sin repeticiones y configurable por segundos; proyección GPS sobre segmentos, rechazo de saltos y alertas de desvío.
- Importar GPX, exportar GPX, ruta guardada localmente, simulación por ruta.
- Tests Kotlin independientes para rectas, curvas, simulación, saltos GPS, ruidos y anuncios; JUnit en GitHub Actions. Ver `docs/ETAPAS_01_A_08.md`.

## Cómo compilar

1. Abrir el proyecto con Android Studio reciente (JDK 17, SDK 35) y sincronizar Gradle.
2. Compilar `:app:assembleDebug` e instalar `app/build/outputs/apk/debug/app-debug.apk`.
3. Si está en GitHub, abrir pestaña **Actions** > `APK Android (Rally GPS AI)` > artefacto `Rally-GPS-AI-v0.2.3-debug` cuando la ejecución termine satisfactoriamente.
4. Dar permiso de ubicación; comprobar conexión y datos móviles si vas a trazar rutas. El modo DEMO y archivos GPX funcionan sin acceder al cálculo de rutas online.

## Uso

Opcional: PUNTOS → elegir origen o paradas en el mapa → BUSCAR destino → TRAZAR RUTA → revisá las notas → INICIAR GPS. También podés mantener pulsado el mapa para elegir destino. Con `DEMO`, las notas se reproducen a velocidad simulada. Para rutas reales, probalo primero como **pasajero** y corregí las notas incorrectas antes de depender de ellas.

**AVISO:** el número de la curva no indica una velocidad segura. Un GPX o una ruta OSM puede simplificar u omitir curvas, condiciones de calzada, tránsito y peligros. En la vía pública, respetar límites de velocidad y señalización. El diseño está pensado como referencia/entrenamiento; no ofrece instrucciones de conducción deportiva en calles.

## Límites conocidos (no afirmamos que estén resueltos)

- GPS/voz actualmente funcionan **solo con la actividad en primer plano**. El servicio de navegación en segundo plano, con pantalla bloqueada, todavía no está implementado.
- El GPS y el mapa no ofrecen descarga de regiones offline; una ruta GPX existente y sus notas pueden ser calculadas sin internet, pero el mapa necesita datos de tiles.
- **Recálculo de desvíos (versión experimental):** si el navegador detecta 3 posiciones GPS fiables fuera de ruta durante más de 3 segundos y a 85 m o más del trazado, solicita una ruta nueva desde la ubicación actual, conservando destino y paradas todavía pendientes. Limita la frecuencia a un intento cada 50 s; se conserva la ruta antigua si falla el servicio. Necesita internet para recalcular. No se recalculan rutas GPX importadas ni DEMO.
- **Voces descargadas:** Ajustes > Elegir voz en español permite seleccionar una voz offline ya instalada en el motor TTS del sistema, escuchar muestra y configurar ritmo/tono; Ajustes > Instalar o cambiar motor TTS abre el panel del dispositivo. RHVoice y SherpaTTS son opciones externas descargables; la aplicación no distribuye paquetes de voces de terceros ni garantiza que todos estén disponibles en cada teléfono.
- El motor OSRM público sigue siendo exclusivamente de demostración y no garantiza disponibilidad; para un producto público hace falta un servicio propio/autorizado.
- La navegación con pantalla bloqueada todavía no funciona; exige la aplicación en primer plano.
- Las notas automáticas se basan solo en la geometría, no infieren saltos, rasantes ni peligros de terreno.
- El servidor de rutas público OSRM demo **no es apto para publicar una app masiva**. Migrar a backend propio licenciado antes de producción.
- La búsqueda por Nominatim no es autocompletado; realiza una consulta manual por cada BUSCAR. Respetar política de uso público.
- **Sin certificación de instalación en Android real**: los tests de geometría funcionan en JVM pero es necesario revisar el resultado de GitHub Actions antes de entregar una APK. Si falla, corregir su log.

## Componentes / atribuciones

- [MapLibre Native](https://github.com/maplibre/maplibre-native), BSD-2-Clause.
- [OpenFreeMap](https://openfreemap.org/), mapas basados en OpenStreetMap. Mantener atribución visible.
- [OpenStreetMap contributors](https://www.openstreetmap.org/copyright), ODbL para datos.
- [OSRM](https://github.com/Project-OSRM/osrm-backend), BSD 2-Clause, servidor demo sujeto a política no comercial, máx. 1 req/s (este prototipo realiza solo peticiones bajo intervención del usuario).
- [Nominatim](https://nominatim.org/release-docs/latest/api/Overview/), OSM Foundation Nominatim public service, uso manual moderado.
- Referencias funcionales únicamente (no se reutiliza su código): Sideways y Rally SatNav.
- `PaceNoteEngine.kt` es una implementación original para este prototipo, no un archivo copiado de local-optimum/pacenotes.

## Qué sigue

Una vez que el MVP compile y se pruebe, sumar servicio de ubicación en primer plano, descarga offline legal, indicaciones convencionales de giros/semáforos, rutas con paradas, pantalla de revisión/edición de notas, GPX con metadatos, marcadores de confianza, pruebas de terreno de calibración 1–6 y reemplazo del servidor demo.

## Interfaz de notas v0.2.1

- Tarjetas rectangulares con pictograma dibujado de izquierda/derecha + número 1–6; color diferente por dificultad.
- Distancia grande `EN 120 M` o `AHORA`, sin mostrar distancias negativas.
- Segunda tarjeta `ENLAZA` únicamente si la siguiente curva está a 85 m o menos del final de la actual.
- Indicadores de `LARGA`, `CIERRA A`, `ABRE A` cuando la geometría estimada los respalda.
- Separación real entre botones, tamaños mínimos táctiles y mayor respeto de barras del sistema.
- Los números son **estimaciones geométricas**, no velocidades ni garantía de condiciones del camino.

Validación local: 672 verificaciones del motor de navegación y 423 de presentación de notas. La compilación Android se realiza en CI y debe estar exitosa antes de instalar.


## Cambios v0.2.3 — búsqueda local y perspectiva de conducción

- Búsqueda manual de calles y alturas contextualizada por la **localidad GPS**, no por el primer resultado global: Nominatim reverse solo al buscar, con localidad en caché efímera.
- Orden por cercanía, filtro de coincidencias locales fuera de radio, elección **explícita** entre alternativas; se indica claramente cuando la altura no está mapeada y el punto es aproximado. Si no aparece, añadir ciudad y provincia o marcar el punto en el mapa.
- Consultas públicas con User-Agent identificable y separación >1 segundo. Sin autocompletado masivo, conforme a la política del proveedor. Cobertura de altura depende de OpenStreetMap.
- Vista **3D de navegación**, no realidad aumentada ni cámara real: zoom cercano, inclinación 60°, rumbo de la ruta hacia adelante y centro adelantado para ver camino delante del vehículo. Demo y GPS usan la misma cámara; 2D vuelve a norte-arriba.
- Verificaciones CLI añadidas para rumbo, giros que cruzan 0/360°, distancia de adelantamiento, tramo final y modo 2D. Tests JUnit con `DrivingCameraTest`.
- Corrección adicional: los marcadores de destino deben elegirse, evitando mandar la ruta por defecto a otra ciudad.

**Importante:** prueba de UI en Motorola pendiente; la búsqueda online no garantiza ubicación exacta de todos los domicilios y el rumbo de ruta es una aproximación geométrica, no detección visual de la calzada.

## 0.2.4 — Pack offline de copiloto (redistribución revisada)

- Selector en Ajustes: **Voz del sistema**, **CarlFM (nítido)**, **CarlFM (radio)**; prueba de notas y efecto de intercomunicador.
- Audio grabado por síntesis de frases en **GitHub Actions** usando la voz CarlFM del catálogo MIT de Piper (modelo entrenado desde cero con datos indicados como dominio público). El modelo ni el motor Piper se incorporan al APK: solo fragmentos de audio OGG redistribuibles y avisos de atribución.
- El reproductor cancela frases obsoletas, encadena segmentos sin conexión y conserva TTS Android como alternativa automática si no se instaló el pack.
- Para compilar desde Android Studio sin conexión necesitás primero generar los recursos con `scripts/generate_rally_voice.py` en una máquina de desarrollo con Piper instalado. La acción de GitHub los genera automáticamente.
- Nota: el audio tiene acento español peninsular, **no** voz de un copiloto profesional grabado. No usamos material de DiRT Rally, Sideways ni terceros sin licencia de redistribución.
