# Rally GPS AI 🏁

Aplicación Android **nueva e independiente**, inspirada en funciones de Sideways y Rally SatNav. No contiene código, recursos visuales ni audios propietarios de esas aplicaciones. Paquete: `ar.com.rallygpsai`. **Estado: MVP experimental 0.1.0.**

## Incluido en esta entrega

- Mapa oscuro de MapLibre / OpenFreeMap con vista 2D e inclinación tipo 3D (no edificios 3D).
- Ubicación GPS Android y elección del destino buscando localidad/calle (Nominatim) o manteniendo pulsado un punto del mapa.
- Ruta de auto desde ubicación actual con motor OSRM demo **solo para desarrollo y uso personal no comercial**, sin garantías de disponibilidad.
- Algoritmo Kotlin independiente para notas 1–6 por radio de curva estimado, color de sectores, distancia y curvas enlazadas.
- TTS local en español, anticipación seleccionable y HUD con próximo giro / distancia / velocidad.
- Importar GPX, exportar GPX, ruta guardada localmente, simulación por ruta.
- Tests CLI de recta, curva izquierda/derecha, rutas simuladas y escala 1–6.

## Cómo compilar

1. Abrir el proyecto con Android Studio reciente (JDK 17, SDK 35) y sincronizar Gradle.
2. Compilar `:app:assembleDebug` e instalar `app/build/outputs/apk/debug/app-debug.apk`.
3. Si está en GitHub, abrir pestaña **Actions** > `APK Android (Rally GPS AI)` > artefacto `Rally-GPS-AI-debug` cuando la ejecución termine satisfactoriamente.
4. Dar permiso de ubicación; comprobar conexión y datos móviles si vas a trazar rutas. El modo DEMO y archivos GPX funcionan sin acceder al cálculo de rutas online.

## Uso

Buscá el destino → BUSCAR → TRAZAR RUTA → revisá las notas → INICIAR GPS. También podés mantener pulsado el mapa para elegir destino. Con `DEMO`, las notas se reproducen a velocidad simulada. Para rutas reales, probalo primero como **pasajero** y corregí las notas incorrectas antes de depender de ellas.

**AVISO:** el número de la curva no indica una velocidad segura. Un GPX o una ruta OSM puede simplificar u omitir curvas, condiciones de calzada, tránsito y peligros. En la vía pública, respetar límites de velocidad y señalización. El diseño está pensado como referencia/entrenamiento; no ofrece instrucciones de conducción deportiva en calles.

## Límites conocidos (no afirmamos que estén resueltos)

- GPS/voz actualmente funcionan **solo con la actividad en primer plano**. El servicio de navegación en segundo plano, con pantalla bloqueada, todavía no está implementado.
- El GPS y el mapa no ofrecen descarga de regiones offline; una ruta GPX existente y sus notas pueden ser calculadas sin internet, pero el mapa necesita datos de tiles.
- No se recalcula automáticamente la ruta cuando el conductor se desvía (la app alerta si se aleja más de 70 m).
- Las notas automáticas se basan solo en la geometría, no infieren saltos, rasantes ni peligros de terreno.
- El servidor de rutas público OSRM demo **no es apto para publicar una app masiva**. Migrar a backend propio licenciado antes de producción.
- La búsqueda por Nominatim no es autocompletado; realiza una consulta manual por cada BUSCAR. Respetar política de uso público.
- **No se ha certificado compilación en el entorno local**; verificar resultados de GitHub Actions y pruebas en Android.

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
