# Rally GPS AI — Plan v0.3.0: Editor de notas, reconocimiento y ruteo más confiable

**Base:** v0.2.9 en rama `feature/pace-queue-countdown-trip-history` (PR #8).  
**Rama de implementación:** `feature/recce-note-editor-route-validation-v030`.  
**Regla del proyecto:** terminar la implementación y revisar archivos ANTES de iniciar la primera compilación de esta etapa.

## Etapa 1 · Modelo editable y persistencia
- Permitir corregir una nota 1–6: sentido izquierda/derecha, dificultad, posición longitudinal, "cierra a", "abre a".
- Validar posiciones, rangos y modificadores; evitar notas ordenadas incorrectamente.
- Guardar correcciones localmente por huella de la ruta (no enviarlas a terceros).
- Restaurar modificaciones al recuperar GPX, ruta guardada o un trazado idéntico.
- Conservar separado el trazado original de las correcciones: editar una nota **no crea ni cambia calles**.

## Etapa 2 · Modo reconocimiento
- Añadir acceso a RECONOCIMIENTO desde la barra inferior, con una ficha clara de cada nota.
- Navegar entre notas anterior/siguiente, enfocar la curva en el mapa y escucharla.
- Editar con controles accesibles, guardar, restablecer y volver a visualizar el cambio.
- No permitir editar mientras está en marcha GPS/DEMO (evitar distracciones y estados inconsistentes).
- Notas geométricas = estimaciones que requieren revisión: no implican velocidad segura.

## Etapa 3 · Ruteo y recálculo
- Seguir usando OSRM demo y OpenStreetMap solo para pruebas; no prometer disponibilidad comercial ni exactitud de calles.
- Inspeccionar JSON de OSRM: rutas válidas, geometría, distancia, puntos ajustados a calle y saltos sospechosos.
- Rechazar rutas inválidas y pedir confirmación si una ubicación fue ajustada demasiado lejos de lo solicitado.
- En recálculo, conservar la ruta anterior ante resultados inválidos o sospechosos; no inventar polilíneas.
- Mantener rutas GPX como recorridos aportados por el usuario, separados de rutas verificadas por OSRM.
- Validar que notas editadas no se transfieran automáticamente a un recálculo con trazado diferente.

## Etapa 4 · Integración y verificaciones
- Conservar 3-2-1-GO, notas encadenadas, voces, Subaru, DEMO sobre rutas reales y estadísticas del GPS real.
- Pruebas JUnit: aplicación y recuperación de modificaciones, límites, orden, ruteo inválido y sospechoso, notas al navegar.
- Verificar correcciones con la ruta completa recargada, sin conexión y con cambios de trazado.
- Revisar cambios estáticamente y abrir PR para ejecutar pruebas y compilar **al final**.
- No fusionar a main ni publicar Release si no pasan las pruebas.

## Criterios de aceptación
1. Una nota modificada se ve igual en RECONOCIMIENTO, mapa, tarjetas y voz.
2. No se altera el recorrido ni se inventan calles.
3. GPX repetido recupera las modificaciones; ruta distinta no las hereda.
4. Ruta OSRM dudosa solicita revisión antes de navegar.
5. Recalcular no sustituye una ruta válida por una dudosa.
6. DEMO e historial previo siguen funcionando.
7. Gradle `testDebugUnitTest` y `assembleDebug` deben pasar antes de entregar APK.

**Limitación:** los proveedores externos y datos OSM pueden contener errores; la validación geométrica no equivale a un levantamiento de campo. Para cualquier prueba conduciendo, utilizar copiloto/pasajero y respetar las normas de tránsito.
