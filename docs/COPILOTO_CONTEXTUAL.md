# Copiloto contextual — Rally GPS AI

**Estado:** implementado en la rama `feature/subaru-pacenotes-distance-ui`. Sin compilación APK ni fusión a `main`.

## Objetivo
Incorporar comentarios ocasionales durante los espacios libres entre notas de rally mediante reglas deterministas sobre la geometría de la ruta, con consumo mínimo de CPU/memoria.

**No contiene un modelo de inteligencia artificial**, no usa servicios pagos, cámara, micrófono ni llamadas a internet adicionales.

## Ajustes
En **AJUSTES → Copiloto contextual**:
- Desactivado (solo notas de rally).
- Ocasional (**predeterminado**): mínimo 240 s entre comentarios, cuando haya motivo geométrico.
- Conversador: mínimo 105 s entre comentarios, sin interrumpir las notas.
- Opción para escuchar un ejemplo con la ruta detenida.

Las preferencias persisten mediante `context_commentary_mode`.

## Señales verificables que emplea
- `WINDING`: cuatro curvas agrupadas o tres curvas con al menos dos cambios de dirección en el intervalo próximo.
- `LINKED`: dos curvas consecutivas de sentidos alternados, a una separación aproximada de 85 m o menos.
- `STRAIGHT`: tras una curva reciente, el mapa muestra un tramo relativamente recto de 800 m o más.

Son datos geométricos aproximados de la ruta; **no implican estado real de la calzada, tránsito, obstáculos, barro, lluvia ni velocidades seguras**.

## Protección contra interrupciones
1. No habla si el usuario desactivó la voz o seleccionó el modo apagado.
2. No habla cuando hay nota de rally pendiente o en reproducción, ni durante el recálculo de ruta.
3. Exige una ventana libre hasta la próxima curva de por lo menos `max(320 m, velocidad × 25 segundos)`.
4. Espera 18 s como mínimo desde el último anuncio prioritario; espera 30 s al inicio de cada navegación y entre 105–240 s entre comentarios según el modo.
5. Omite comentarios si el GPS ofrece precisión inferior a 25 m, al salir de la ruta, al estar detenido o cerca del destino.
6. Usa TTS Android en el idioma elegido; las grabaciones CarlFM siguen reservadas para las notas de rally.
7. No repite el mismo comentario asociado al mismo sector y reinicia su estado al iniciar otra ruta o recalcular.

## Archivos
- `ContextCopilot.kt`: reglas sin dependencias de Android.
- `RallyLanguage.kt`: mensajes localizados en español, inglés y portugués.
- `MainActivity.kt`: activación y ajustes; reproduce comentarios solo si no hay instrucciones prioritarias.
- `RallyVoicePack.kt`: consulta de reproducción en curso.
- `ContextCopilotTest.kt`: pruebas para disparos de contexto, frecuencia, silencio, prioridades y textos.

## Pruebas de aceptación pendientes
- Ejecutar JUnit del módulo Android y confirmar compilación Kotlin.
- Probar DEMO y rutas con curvas encadenadas, tramos rectos y trayectos largos.
- Verificar que una nota normal **interrumpa inmediatamente** un comentario TTS cuando corresponda.
- Verificar mensajes en español argentino, español español, inglés y portugués.
- Probar GPS impreciso, desvíos y recálculos desde el teléfono.
- Comprobar uso de memoria/batería y audibilidad con música o el audio del auto.

**No se compiló la APK en esta etapa.** La navegación real exige pruebas como pasajero y respetando señalización.
