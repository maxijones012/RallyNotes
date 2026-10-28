# Rally GPS AI — Galería de vehículos GPS

**Rama:** `feature/subaru-pacenotes-distance-ui`  
**Estado:** cambios implementados, sin compilación APK ni merge a `main`.

## Galería de vehículos
Se incorporaron **24 alternativas**: 23 representaciones estilizadas de vehículos y el punto GPS clásico.

Rally:
- Subaru WRX STI (azul, con marcador original), Mitsubishi Lancer Evo, Toyota GR Yaris, Ford Fiesta R5, VW Polo Rally, Peugeot 208 Rally4, Renault Clio Rally, Škoda Fabia Rally2, Citroën C3 Rally, Hyundai i20 N Rally.

Calle:
- Honda Civic Type R, VW Golf GTI, Ford Focus RS, Renault Sandero RS, Chevrolet Onix, Fiat Palio.

Pickups:
- Toyota Hilux GR, Ford Ranger Raptor, Volkswagen Amarok.

4x4:
- Jeep Renegade, Lada Niva, Suzuki Jimny.

Especial:
- UTV todoterreno.

Clásico:
- Punto GPS turquesa.

## Arquitectura
- `RallyVehicles.kt` dibuja los marcadores localmente mediante Android Canvas, a partir de siluetas, carrocerías, colores, ventanillas y detalles propios. **No se descargan imágenes de internet ni se redistribuyen logos o fotos de terceros**.
- Todos los marcadores se dibujan con el capó hacia arriba (norte) para poder girarlos según el rumbo.
- `MainActivity.kt` muestra una galería con miniaturas reales de esos sprites, agrupados por tipo.
- Al tocar un vehículo: se guarda la preferencia `vehicle_id`, cambia el icono del GPS sin destruir la navegación y se mantiene también en DEMO.
- `updateVehicleLayers` alterna entre sprite y círculo clásico. Mantiene compatibilidad con la preferencia anterior `subaru_marker`.
- Las texturas se cargan una sola vez por estilo de MapLibre para no sobrecargar el seguimiento GPS.
- La galería está disponible desde **Ajustes → Elegir vehículo GPS** y desde la **pantalla inicial**.
- Mantiene el marcador original Subaru azul si el usuario no elige otra cosa. No se modifica `Patrulla AI`.

## Limitaciones y criterios para revisión visual
- Son marcadores vectoriales originales **estilizados**, no reproducciones fotorrealistas ni representaciones exactas de todos los modelos.
- Las miniaturas usan orientación frontal hacia arriba; comprobar que el modelo gire con el GPS en 2D y 3D.
- Verificar legibilidad de vehículos de calle y pickups en el mapa oscuro.
- Probar selección en navegación activa, con ruta guardada y sin conexión.
- Verificar persistencia tras cerrar/reabrir y selección en primer inicio.
- Revisar no solapamiento de las tarjetas de selección con letra grande.

## Pruebas pendientes
- Ejecutar JUnit `RallyVehiclesTest.kt`, las pruebas del motor contextual y el resto de la suite.
- Generar e instalar APK de prueba para comprobar el dibujo real de Canvas en un Motorola.
- Comprobar que el sprite no vuelva a subirse a MapLibre en cada actualización GPS.
- Revisar memoria, rendimiento y que las notas y el audio sigan funcionando.
- No se debe fusionar con `main` ni compilar hasta recibir nueva autorización del usuario.
