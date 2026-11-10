# Rally GPS AI — Plan maestro de 200 pasos

Base: v0.3.0. Rama de trabajo: `feature/v031-ux-vehicle-destination-reductores`.

**Criterio:** la lista es un backlog técnico, no una declaración de que los 200 pasos ya están completados. Los pasos con dependencia de servicios externos o validación real quedan pendientes hasta probarlos. No se deben publicar Releases sin tests.

## Etapa 1 — Auditoría y control de calidad

- [ ] 001. Revisar capturas reales de Android
- [ ] 002. Identificar elementos que tapan el mapa
- [ ] 003. Revisar el flujo de elección de destino
- [ ] 004. Revisar capas y orden de MapLibre
- [ ] 005. Identificar mensajes de ayuda innecesarios
- [ ] 006. Analizar distribución de botones inferiores
- [ ] 007. Comparar vehículos de la galería
- [ ] 008. Comprobar tamaños en distintas pantallas
- [ ] 009. Verificar localización de mensajes
- [ ] 010. Auditar estados GPS y DEMO
- [ ] 011. Identificar rutas calculadas y GPX
- [ ] 012. Verificar persistencia de opciones
- [ ] 013. Revisar la lógica de permisos GPS
- [ ] 014. Verificar control de errores de red
- [ ] 015. Localizar puntos de actualización de notas
- [ ] 016. Analizar cola de voz durante curvas
- [ ] 017. Comprobar dibujos sobre modo oscuro
- [ ] 018. Revisar accesibilidad y tamaños táctiles
- [ ] 019. Establecer pruebas de regresión
- [ ] 020. Documentar riesgos y límites de datos

## Etapa 2 — Interfaz de navegación limpia

- [ ] 021. Ocultar búsqueda al iniciar GPS
- [ ] 022. Ocultar búsqueda al iniciar DEMO
- [ ] 023. Restablecer búsqueda al finalizar
- [ ] 024. Eliminar cartel verde persistente
- [ ] 025. No repetir nombres de destino
- [ ] 026. Quitar ayudas redundantes del mapa
- [ ] 027. Conservar errores críticos visibles
- [ ] 028. Mostrar solo controles necesarios en marcha
- [ ] 029. Evitar desplazamientos bruscos del panel
- [ ] 030. Permitir ver mapa sin panel de edición
- [ ] 031. Reducir márgenes superiores innecesarios
- [ ] 032. Separar modo planificación de conducción
- [ ] 033. Revisar tipografía de distancias
- [ ] 034. Uniformar colores de acciones
- [ ] 035. Mantener indicador discreto de GPS
- [ ] 036. Reorganizar opciones del pie
- [ ] 037. Evitar palabras partidas en botones
- [ ] 038. Conservar acceso al menú al parar
- [ ] 039. Hacer la limpieza reversible
- [ ] 040. Probar estados con y sin ruta

## Etapa 3 — Pin de origen, destino y paradas

- [ ] 041. Crear pin visual de destino
- [ ] 042. Definir pin de origen distinto
- [ ] 043. Definir pin de parada distinto
- [ ] 044. Poner halo contrastado en el mapa
- [ ] 045. Dibujar marcador sobre la línea de ruta
- [ ] 046. Aumentar tamaño al seleccionar
- [ ] 047. Mantener pin al terminar de calcular
- [ ] 048. Mantener pin después de recálculo
- [ ] 049. Restaurar pin al abrir una ruta guardada
- [ ] 050. Eliminar pin al borrar su punto
- [ ] 051. Mostrar distancia opcional al destino
- [ ] 052. Confirmar selección con feedback breve
- [ ] 053. Respetar coordenadas longitud/latitud
- [ ] 054. Evitar pin duplicado en cambios rápidos
- [ ] 055. Conservar pin durante cambio de estilo
- [ ] 056. Verificar visibilidad con zoom cercano
- [ ] 057. Verificar visibilidad con zoom lejano
- [ ] 058. Crear iconos aptos para mapa oscuro
- [ ] 059. No tapar el marcador del Subaru
- [ ] 060. Probar origen, destino y cuatro paradas

## Etapa 4 — Galería y vehículos

- [ ] 061. Unificar estética de miniaturas
- [ ] 062. Conservar Subaru azul favorito
- [ ] 063. Aumentar tamaño de ilustraciones
- [ ] 064. Reorganizar galería en dos columnas
- [ ] 065. Evitar nombres cortados
- [ ] 066. Mostrar selección claramente
- [ ] 067. Agregar descripción accesible
- [ ] 068. Mostrar grupo de cada vehículo
- [ ] 069. Mantener alternativa de punto clásico
- [ ] 070. Separar auto y fondo visualmente
- [ ] 071. Optimizar bordes y contornos
- [ ] 072. Revisar orientación de capó
- [ ] 073. Revisar rotación en GPS
- [ ] 074. Revisar rotación en DEMO
- [ ] 075. Conservar tamaño normal
- [ ] 076. Conservar tamaño grande
- [ ] 077. Conservar tamaño extra grande
- [ ] 078. Guardar elección tras reiniciar
- [ ] 079. Reducir consumo de memoria de sprites
- [ ] 080. Probar contrastes sobre ruta violeta

## Etapa 5 — Rutas y mapa

- [ ] 081. Verificar que DEMO no invente calles
- [ ] 082. Distinguir ruta real y GPX
- [ ] 083. Verificar respuesta de OSRM
- [ ] 084. Rechazar geometrías inválidas
- [ ] 085. Advertir rutas dudosas
- [ ] 086. Respetar sentido del trazado
- [ ] 087. No dibujar conexiones imposibles
- [ ] 088. Resaltar tramo activo
- [ ] 089. Simplificar visualmente tramos completados
- [ ] 090. Mantener destino visible
- [ ] 091. Conservar origen al planificar
- [ ] 092. Mantener rutas por encima del mapa base
- [ ] 093. Evitar tapas de marcador por líneas
- [ ] 094. Recalcular ante desvíos validados
- [ ] 095. No recalcular por salto de GPS
- [ ] 096. Mantener paradas aún pendientes
- [ ] 097. Restaurar cámara de primera persona
- [ ] 098. Alternar 2D y 3D sin perder marcadores
- [ ] 099. Mantener zoom adaptable
- [ ] 100. Revisar modos satélite y oscuro

## Etapa 6 — Reductores y obstáculos confirmados

- [ ] 101. Definir categorías de reductor
- [ ] 102. Reconocer tags OSM traffic_calming
- [ ] 103. Consultar reductores de la zona de ruta
- [ ] 104. Hacer consulta solo cuando haga falta
- [ ] 105. Limitar área consultada
- [ ] 106. Manejar errores y falta de conexión
- [ ] 107. Filtrar puntos próximos al trazado
- [ ] 108. Rechazar obstáculos muy alejados
- [ ] 109. Eliminar duplicados cercanos
- [ ] 110. Identificar origen OSM o manual
- [ ] 111. Mostrar símbolo de reductor en mapa
- [ ] 112. Permitir agregar reductor manualmente
- [ ] 113. Guardar reductores manuales localmente
- [ ] 114. Permitir borrar registros manuales
- [ ] 115. Mostrar metros hasta el próximo reductor
- [ ] 116. Priorizar la nota de curva sobre aviso secundario
- [ ] 117. No anunciar reductores desconocidos
- [ ] 118. Evitar avisos repetidos del mismo punto
- [ ] 119. Etiquetar información no verificada
- [ ] 120. Probar diversos trazados en Trelew

## Etapa 7 — Notas, voz y anticipaciones

- [ ] 121. Conservar notas 1 a 6
- [ ] 122. Mantener actual, siguiente y tercera
- [ ] 123. Verificar transición al pasar curva
- [ ] 124. Evitar notas repetidas
- [ ] 125. Mantener voz de 800 metros
- [ ] 126. Mantener cuenta 3-2-1 GO
- [ ] 127. Integrar anuncio opcional de reductor
- [ ] 128. Bajar volumen de comentarios ante curva
- [ ] 129. Respetar voz activada o silenciada
- [ ] 130. No mezclar grabaciones de idiomas
- [ ] 131. Ajustar frecuencia del copiloto contextual
- [ ] 132. Evitar dos mensajes simultáneos
- [ ] 133. Respetar pausa de DEMO
- [ ] 134. Revisar frases de curva larga
- [ ] 135. Revisar enlace de curvas
- [ ] 136. Conservar nota estimada visible
- [ ] 137. Evitar órdenes de velocidad insegura
- [ ] 138. Comprobar claridad de voz en parlantes
- [ ] 139. Agregar prueba de previsualización
- [ ] 140. Probar simulación de múltiples curvas

## Etapa 8 — Ajustes, accesibilidad y usabilidad

- [ ] 141. Ordenar ajustes por categorías
- [ ] 142. Usar iconos comprensibles
- [ ] 143. Agregar accesibilidad a botones
- [ ] 144. Aumentar áreas táctiles
- [ ] 145. Respetar escalado de fuentes
- [ ] 146. Evitar texto cortado en español
- [ ] 147. Revisar textos en inglés
- [ ] 148. Revisar textos en portugués
- [ ] 149. Conservar preferencias al actualizar
- [ ] 150. Agregar explicación breve solo si ayuda
- [ ] 151. Evitar popups repetitivos
- [ ] 152. Permitir corregir destino antes de navegar
- [ ] 153. Restringir edición con el auto en movimiento
- [ ] 154. Conservar modo compacto
- [ ] 155. Conservar panel de notas grande
- [ ] 156. Mostrar estados sin consumir mapa
- [ ] 157. Revisar orientación horizontal
- [ ] 158. Revisar modo nocturno
- [ ] 159. Probar TalkBack
- [ ] 160. Revisar idioma inicial

## Etapa 9 — Pruebas y rendimiento

- [ ] 161. Crear pruebas de pin visible
- [ ] 162. Crear pruebas de persistencia de destino
- [ ] 163. Probar estados de barra de búsqueda
- [ ] 164. Probar límites de vehículo seleccionado
- [ ] 165. Probar mapa cargado sin ruta
- [ ] 166. Probar mapa cargado con ruta
- [ ] 167. Probar cambio de estilo de mapa
- [ ] 168. Crear pruebas del parser OSM
- [ ] 169. Probar matching de reductores
- [ ] 170. Probar filtros de distancias
- [ ] 171. Probar deduplicación
- [ ] 172. Probar rechazo de GPS impreciso
- [ ] 173. Probar aviso de reductor y curva próxima
- [ ] 174. Probar silencio de avisos
- [ ] 175. Probar pérdida de internet
- [ ] 176. Probar persistencia de reductores manuales
- [ ] 177. Probar JUnit completo
- [ ] 178. Compilar APK Android
- [ ] 179. Inspeccionar integridad de APK
- [ ] 180. Probar visualmente con capturas

## Etapa 10 — Publicación y mantenimiento

- [ ] 181. Actualizar número de versión
- [ ] 182. Escribir changelog de la versión
- [ ] 183. Documentar fuentes OSM y atribución
- [ ] 184. Documentar límites de API pública
- [ ] 185. Explicar reductores no detectados
- [ ] 186. Documentar flujo de marcación
- [ ] 187. Documentar opciones de vehículos
- [ ] 188. Comprobar firma de APK
- [ ] 189. Resguardar compatibilidad de datos
- [ ] 190. Preparar rama de integración
- [ ] 191. Crear Pull Request para revisión
- [ ] 192. Verificar GitHub Actions
- [ ] 193. Examinar informes de errores
- [ ] 194. Corregir fallas de compilación
- [ ] 195. Conservar Release anterior disponible
- [ ] 196. Publicar prerelease solo al aprobar
- [ ] 197. Comprobar enlace permanente de APK
- [ ] 198. Definir prueba segura en Motorola
- [ ] 199. Recoger observaciones de la versión
- [ ] 200. Cerrar tareas y dejar pendientes visibles
