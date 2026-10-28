# Rally GPS AI — Diseño de notas, distancias y Subaru (borrador sin compilar)

**Estado:** cambios implementados en la rama `feature/subaru-pacenotes-distance-ui`. A pedido del usuario, **no ejecutar compilación Gradle ni generar APK todavía**.

## Panel superior
- **Número grande (39 sp) arriba a la izquierda**: distancia a la próxima curva; unidad `m` / `km` en cuerpo menor.
- Leyenda discreta `HASTA LA PRÓXIMA CURVA`.
- **Debajo:** tarjeta rectangular de curva (izquierda/derecha 1–6), con color por dificultad, modificadores estimados y la segunda nota enlazada cuando corresponde.
- Menos de 20 m: `AHORA`. Entre 20 y 994 m: `80 m`, `300 m`, etc.; a partir de aproximadamente 1 km: `1,9 km`, etc.
- No confundir la distancia hasta la curva con la distancia restante hasta el destino.

## Panel inferior
- A la izquierda, velocidad GPS (`54 km/h`).
- A la derecha, **distancia al destino en tipografía mayor**, con metros/kilómetros automáticos.

## Marcador sobre el mapa
- Reemplaza el punto turquesa por un **Subaru azul, visto desde arriba, con frente apuntando al norte en el recurso base**.
- En GPS/DEMO el sprite rota de acuerdo con el rumbo del trazado; fuera de navegación utiliza bearing GPS cuando esté disponible.
- Fallback: círculo GPS si el sprite no se puede decodificar.
- En selección de destino sobre el mapa, muestra confirmación textual adicional.

## Revisiones previas a generar la APK
1. Ejecutar tests existentes y tests del formato de distancia.
2. Verificar compilación Android, firma e instalación sobre versión anterior.
3. Probar `1,9 km`, `300 m`, `80 m`, `AHORA` sin solapamientos.
4. Probar Subaru en rumbo norte, sur, este, oeste y en curvas enlazadas; comprobar orientaciones en 2D y vista inclinada.
5. Validar marcación de destino con pulsación prolongada y recálculo de ruta en modo demostración y pruebas seguras.
6. Revisar el contraste de las tarjetas con distintos tamaños de fuente y escalado Android.

## Precaución
Las notas derivadas de geometría del mapa son **estimaciones**, no consignas de velocidad segura. No usar en conducción deportiva en vía pública.
