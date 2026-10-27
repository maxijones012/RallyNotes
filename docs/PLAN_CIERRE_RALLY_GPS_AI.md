# Rally GPS AI — Plan de cierre y cambios implementados

**Rama de trabajo:** `feature/subaru-pacenotes-distance-ui`
**Versión base:** Rally GPS AI 0.2.4 · `ar.com.rallygpsai`
**Estado:** código actualizado sin compilar, sin merge a `main` y sin APK nueva.

## Objetivo

Cerrar la experiencia de navegación inspirada en Sideways y Rally SatNav: lectura clara de las notas 1–6, ubicación visible del destino y un mapa con marcador seleccionable, sin afectar las funciones existentes de ruteo, GPX ni voces.

## Cambios aplicados en el código

| Área | Estado | Implementación |
| --- | --- | --- |
| Ícono de la app | Hecho en código | Recurso original aprobado por el usuario convertido a WebP con margen de seguridad; `mipmap-anydpi-v26/ic_launcher.xml` con fondo oscuro y primer plano artístico; `AndroidManifest.xml` apunta al nuevo ícono adaptativo. Se incluye un vector alternativo. |
| Distancia a próxima curva | Hecho en código | `RallyDistanceFormatter`: número grande (39 sp) separado de la unidad (18 sp) **encima** de la tarjeta; `1,9 km`, `300 m`, `80 m` o `AHORA`. |
| Notas de rally | Hecho en código | Recuadros más rectangulares, flechas gruesas, número destacado, color según dificultad, modificadores y próxima curva enlazada. |
| Distancia total / velocidad | Hecho en código | Velocidad a la izquierda y distancia al destino independiente a la derecha, abajo. |
| Subaru GPS | Hecho en código | Marcador Subaru azul visto desde arriba; frente orientado con el rumbo del trazado durante GPS y DEMO; GPS bearing fuera de la ruta si existe. |
| Selector de marcador | Hecho en código | Ajustes → **Elegir vehículo del GPS**: Subaru azul o punto tradicional. Preferencia persistente y fallback si falta el recurso. |
| Destino visible | Hecho en código | Chip `✓ DESTINO` persistente, tocable para centrar el mapa; aviso de dirección aproximada; posibilidad de elegir un destino con toque de mapa desde PUNTOS o pulsación prolongada. |
| Pines diferenciados | Hecho en código | Destino verde, origen azul, paradas amarillas. El destino sigue en el mapa después de trazar la ruta. |
| Ruteo / recálculo | Conservado | Validación por varias lecturas GPS, antirrebote de desvíos y regeneración de notas para rutas de auto. Requiere internet y depende del servidor demo OSRM; no recalcula GPX. |
| Voces de copiloto | Conservado | CarlFM offline, efecto de intercomunicador, TTS Android, selección y prueba de voz. No se incorporaron packs sin autorización. |

## Validación pendiente (no compilar todavía)

1. **Revisión de código y recursos**: confirmar manifiesto, archivo WebP, XML adaptativo, referencias MapLibre, botones y control de estados.
2. **Cuando se autorice compilar**: ejecutar `gradle :app:testDebugUnitTest` y `gradle :app:assembleDebug` en GitHub Actions.
3. **Instalación en Motorola**: comprobar actualización sobre 0.2.4 sin pérdida de preferencias ni rutas guardadas.
4. **UI en pantalla real**: revisar escalado 100–130%, rotación, barras del sistema y visibilidad de tarjetas con curva enlazada.
5. **Marcador**: ensayar rumbo norte, sur, este y oeste; Subaru y punto clásico en 2D, 3D y DEMO.
6. **Destino**: búsqueda por calle y altura, resultado aproximado, selección con toque, selección prolongada, visualización y recálculo.
7. **Copiloto**: reproducción clara y radio, cambios entre TTS y grabación, anticipación y no duplicación de audios.
8. **Rutas**: GPS impreciso, ruta con paradas, recuperación desde archivo, GPX sin recálculo y reconexión tras pérdida de internet.

## Criterios para aceptar la próxima APK

- Abre sin cierres inesperados, muestra mapa y permisos correctamente.
- El ícono nuevo se ve bien en el launcher y menú de aplicaciones.
- El indicador de la próxima curva nunca se confunde con la distancia total.
- Los botones no se superponen en el Motorola.
- El Subaru gira en el sentido de avance, no marcha atrás, y el selector funciona.
- El destino queda inequívocamente marcado y la ruta mantiene el pin final.
- Las notas son estimaciones de la geometría, no velocidades recomendadas; avisos y voz funcionan.
- Navegación y archivos GPX originales no presentan regresiones observables.

## Restricciones conocidas y seguridad

- Las curvas calculadas por OSM/GPX son aproximaciones; los peligros, saltos y velocidades no deben inferirse sin verificación.
- OSRM demo es exclusivamente para pruebas, no garantiza servicio público permanente.
- No se habilita navegación con pantalla bloqueada ni cartografía offline completa.
- El arte incluye referencias visuales a un Subaru; si se publica comercialmente, revisar cualquier uso de marcas y logotipos.
- **No activar CI, crear un PR que dispare CI, ni compilar hasta recibir una orden explícita del usuario.**


## Nueva etapa: bienvenida, idioma y configuración inicial
- [x] Mostrar una pantalla completa de bienvenida con el ícono oficial en la primera apertura.
- [x] Preseleccionar según el idioma del dispositivo (es-AR, es-ES, en-US, pt-BR; español argentino como alternativa).
- [x] Elegir idioma de interfaz principal y notas de copiloto.
- [x] Configurar voz activada y Subaru azul o punto clásico antes de comenzar.
- [x] Guardar la elección y no mostrar el asistente en cada apertura.
- [x] Permitir reabrir la pantalla desde Ajustes > Idioma de la aplicación sin borrar la ruta.
- [x] Usar voces CarlFM solo en español; para inglés/portugués, usar TTS del dispositivo en ese idioma.
- [x] Añadir pruebas JUnit de idioma y mensajes de rally en `RallyLanguageTest.kt`.
- [ ] Ejecutar JUnit, compilar la APK y probar el arranque en Android real (pospuestos a pedido del usuario).
- [ ] Revisar mensajes secundarios heredados que todavía están en español.

**Ver detalle:** `docs/INICIO_GUIADO_IDIOMAS.md`. **No se compiló ni fusionó a main.**

## Nueva etapa: copiloto contextual sin IA pesada

- [x] Agregar motor Kotlin determinista que usa notas de curvas y recorrido ya cargado.
- [x] Detectar tramos sinuosos, curvas enlazadas y secciones relativamente rectas.
- [x] Incorporar comentarios ocasionales, sin deducir pozos, barro, tráfico ni condiciones climáticas.
- [x] Agregar ajustes Desactivado / Ocasional (predeterminado) / Conversador.
- [x] Guardar preferencias, suprimir comentarios cerca de curvas, durante audio prioritario, con GPS poco preciso y durante recálculos.
- [x] Localizar comentarios en los cuatro idiomas disponibles.
- [x] Agregar pruebas JUnit independientes del mapa y documentar comportamiento.
- [ ] Ejecutar pruebas Android y compilar la APK (pendiente por instrucción previa de no compilar).
- [ ] Validar interrupciones, frecuencia y audibilidad en Motorola en pruebas seguras.

**Documentación:** `docs/COPILOTO_CONTEXTUAL.md`. Esta etapa permanece en la rama, no fusionada a `main`.
