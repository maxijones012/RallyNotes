# Rally GPS AI 0.2.9 — notas encadenadas, cuenta regresiva e historial

## Experiencia de navegación
- **Nota principal** con tamaño grande (por defecto): distancia, curva 1–6 y detalles.
- **Siguiente nota** visible a la derecha incluso cuando no está enlazada. Si está enlazada, conserva el rótulo «ENLAZA».
- **Tercera nota** en una línea compacta «DESPUÉS», cuando existe. Se actualizan automáticamente cuando NavigationEngine avanza de curva.
- En Ajustes > Navegación y notas se puede elegir panel grande o compacto, sin cambiar la lógica de navegación.
- **Cuenta regresiva** visual y por voz «3, 2, 1, GO!» antes de GPS o DEMO, con cancelar. No inicia el movimiento antes de GO.

## Estadísticas e historial
- En el panel de controles aparece **HIST.**
- Registra trayectos reales con GPS: tiempo, kilómetros recorridos, promedio (distancia GPS / tiempo total), máxima GPS, fecha, recorrido planificado y huella GPS real.
- Al abrir una ficha: miniatura esquemática del trayecto, estadísticas, Repetir ruta, Ver mapa y Eliminar (con confirmación).
- Se guardan hasta 40 registros privados, comprimidos en almacenamiento interno del dispositivo. No se sincronizan ni se publican.
- DEMO nunca registra máximas ni medias reales.
- Filtros GPS: precisión <= 25m, velocidad razonable, saltos de ubicación rechazados. Recorridos demasiado breves no se guardan para evitar registros falsos.
- Repetir ruta vuelve a cargar su geometría guardada y recalcula notas a partir del trazado original. Ver mapa usa la huella GPS guardada.
- Se respeta el bloqueo de DEMO sin ruta cargada de 0.2.8.

## Pruebas
- JUnit para cola de notas, conteo y estadísticas GPS reales. Pasar la suite Android en CI y ensamblar APK de prueba.
- Validar en Motorola: voz «GO» y cancelación, reinicio de notas en curvas consecutivas, modos de UI con fuentes grandes, registro de salida GPS y persistencia al cerrar.
- Pruebas reales de GPS solo con el teléfono montado y sin manipularlo mientras conducís.

**Estado:** implementación de desarrollo. No publicar Release ni fusionar a main hasta verificar los tests y revisar el resultado de la APK.
