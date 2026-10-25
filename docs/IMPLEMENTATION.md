# Implementación y checklist técnico

1. Android nativo (Kotlin, Java 17, SDK 35; min 26). App ID **ar.com.rallygpsai**.
2. MapLibre 12.3.1, OpenFreeMap dark, render dinámico GeoJSON; fuente coloreada por curva.
3. Origen GPS + destino por selección en mapa o Nominatim. OSRM demo no comercial para prueba.
4. GPX seguro: XML DOCTYPE prohibido, entidades externas deshabilitadas, hasta 30.000 puntos.
5. Motor de notas remuestrea cada 10 m, suaviza, estima radio con ventanas; clasifica 1=radios cortos hasta 6=abierto. Cada nota es aproximada.
6. Modo navegación: indicador de progreso aproximado por cercanía y alerta cuando se aleja 70 m; cola de voz en TTS local.
7. DEMO sin necesidad de velocidad real y aviso sobre ruta sintética si no hay datos importados.
8. Sin cuentas, permisos superfluos ni servicios que cobren por generación de voz.

## Revisión manual obligatoria

- [ ] Compilar APK debug en Android SDK real
- [ ] Instalar en Android 16 y verificar lifecycle MapLibre
- [ ] Verificar capa oscura, zoom, posición y permisos
- [ ] Cargar GPX de ruta conocida (20 km) y contrastar notas con mapa real
- [ ] Comprobar errores de OSRM/Nominatim y límites de uso
- [ ] Comprobar TTS es-AR, locuciones repetidas y cambio de idioma
- [ ] Probar sin internet y con GPS degradado
- [ ] No usar modo de copiloto mientras se conduce deportivamente en calles abiertas
