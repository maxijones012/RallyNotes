# Rally GPS AI 0.2.2 — guía de verificación Android

## Búsqueda
1. Habilitar GPS y buscar «25 de Mayo 456» sin especificar ciudad.
2. Verificar las opciones con localidad y distancia; no elegir automáticamente otro país.
3. Si OpenStreetMap carece del número, mostrar «ubicación aproximada».
4. Buscar «25 de Mayo 456, Trelew, Chubut» para probar contexto explícito.
5. Alternativa: mantener pulsado el mapa para señalar el destino.

## Cámara
1. En DEMO, la vista 3D debe seguir el rumbo de la polilínea aunque el teléfono esté quieto.
2. Alternar 3D (inclinada, rumbo arriba) y 2D (norte arriba).
3. En GPS, evitar vueltas de cámara al cruzar de 359 a 0 grados.
4. El enfoque del mapa debe estar adelantado durante la marcha.

## Límites
Nominatim y OpenStreetMap no garantizan números de puerta exactos. El mapa 3D no es video ni realidad aumentada. Probar desde asiento de acompañante.