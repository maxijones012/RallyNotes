# Rally GPS AI · Etapas 1 a 8 · v0.2.0

Desarrollo de una app independiente (`ar.com.rallygpsai`), sin reutilizar código de Sideways o Rally SatNav y sin modificar Patrulla AI.

| Etapa | Entregado | Comprobación |
|---|---|---|
| 1 Proyecto separado | Android/Kotlin, Gradle, nombre propio, repositorio RallyNotes | Manifest/applicationId |
| 2 Mapa | MapLibre/OpenFreeMap oscuro, cámara 2D/inclinada, brújula, zoom, HUD despejado | Revisión estática; Android pendiente |
| 3 Planificador | Origen GPS o manual, destino buscado/pulsación larga, hasta 4 paradas, GPX | Validación de argumentos; red requiere teléfono |
| 4 Geometría | Distancias por arcos, remuestreo 5 m, suavizado, grados y sentido | CLI recta, L/R, ruido, GPX denso |
| 5 Notas 1–6 | Intensidades, horquillas, cambios de radio estimados, enlaces y distancias | CLI y JUnit; pendientes calibraciones en terreno |
| 6 Gráficos | Ruta coloreada por curvas, marcadores `D/I` con grado y leyenda implícita; colores distintivos | Código MapLibre; pendiente observar en dispositivo |
| 7 Voz | TTS español, antelación 4/7/10 segundos, lectura de curvas enlazadas, cola no destructiva | Pruebas del scheduler y no duplicación; TTS Android pendiente |
| 8 Seguimiento | Ajuste GPS a segmento, avance monotónico, control de salto, desvío >55 m y GPS impreciso | Pruebas CLI y JUnit; hardware pendiente |

**Limitaciones conscientes:** solo foreground, mapas online, OSRM público de demostración no comercial, notas geométricas estimadas y no instrucciones de velocidad. Los tests CLI *no sustituyen* compilar e instalar la APK. La compilación automática está configurada en GitHub Actions para ejecutar primero JUnit y luego `assembleDebug`.

## Cómo probar de forma segura en el Motorola

1. Verificar que Actions tenga una ejecución verde y descargar su artefacto APK firmado para depuración.
2. Abrir, conceder ubicación y comprobar mapa oscuro y centrado.
3. Presionar **DEMO** con el vehículo detenido. Debe anunciar curvas simuladas y mostrar colores D/I.
4. Cargar un GPX conocido y comprobar que las notas y distancias sean razonables; revisar zoom y posición.
5. Probar desde el asiento de pasajero si las indicaciones son oportunas, sin depender de ellas para conducir.

## Comando CLI sin Android SDK

`kotlinc app/src/main/java/ar/com/rallygpsai/{PaceNoteEngine,PaceNotes,NavigationEngine}.kt tests/Stages1to8Verification.kt -include-runtime -d /tmp/etapas.jar && java -jar /tmp/etapas.jar`
