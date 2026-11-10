# Rally GPS AI — Plan maestro de 200 pasos

Base: v0.3.0. Rama de trabajo: `feature/v031-ux-vehicle-destination-reductores`.

**Estado actualizado:** 121 pasos con código aplicado o revisión técnica realizada; 79 pendientes de implementación, validación visual o pruebas reales. El tilde de una revisión no reemplaza una prueba en el Motorola.

**Criterio:** las tareas restantes no se dan por realizadas. Los pasos con dependencia de servicios externos o validación real quedan pendientes hasta probarlos. No se deben publicar Releases sin tests.

## Etapa 1 — Auditoría y control de calidad

- [x] 001. Revisar capturas reales de Android
- [x] 002. Identificar elementos que tapan el mapa
- [x] 003. Revisar el flujo de elección de destino
- [x] 004. Revisar capas y orden de MapLibre
- [x] 005. Identificar mensajes de ayuda innecesarios
- [x] 006. Analizar distribución de botones inferiores
- [x] 007. Comparar vehículos de la galería
- [ ] 008. Comprobar tamaños en distintas pantallas
- [x] 009. Verificar localización de mensajes
- [x] 010. Auditar estados GPS y DEMO
- [x] 011. Identificar rutas calculadas y GPX
- [x] 012. Verificar persistencia de opciones
- [ ] 013. Revisar la lógica de permisos GPS
- [x] 014. Verificar control de errores de red
- [x] 015. Localizar puntos de actualización de notas
- [x] 016. Analizar cola de voz durante curvas
- [x] 017. Comprobar dibujos sobre modo oscuro
- [ ] 018. Revisar accesibilidad y tamaños táctiles
- [ ] 019. Establecer pruebas de regresión
- [x] 020. Documentar riesgos y límites de datos

## Etapa 2 — Interfaz de navegación limpia

- [x] 021. Ocultar búsqueda al iniciar GPS
- [x] 022. Ocultar búsqueda al iniciar DEMO
- [x] 023. Restablecer búsqueda al finalizar
- [x] 024. Eliminar cartel verde persistente
- [x] 025. No repetir nombres de destino
- [ ] 026. Quitar ayudas redundantes del mapa
- [x] 027. Conservar errores críticos visibles
- [ ] 028. Mostrar solo controles necesarios en marcha
- [ ] 029. Evitar desplazamientos bruscos del panel
- [ ] 030. Permitir ver mapa sin panel de edición
- [ ] 031. Reducir márgenes superiores innecesarios
- [ ] 032. Separar modo planificación de conducción
- [ ] 033. Revisar tipografía de distancias
- [ ] 034. Uniformar colores de acciones
- [ ] 035. Mantener indicador discreto de GPS
- [x] 036. Reorganizar opciones del pie
- [x] 037. Evitar palabras partidas en botones
- [ ] 038. Conservar acceso al menú al parar
- [x] 039. Hacer la limpieza reversible
- [ ] 040. Probar estados con y sin ruta

## Etapa 3 — Pin de origen, destino y paradas

- [x] 041. Crear pin visual de destino
- [x] 042. Definir pin de origen distinto
- [x] 043. Definir pin de parada distinto
- [x] 044. Poner halo contrastado en el mapa
- [x] 045. Dibujar marcador sobre la línea de ruta
- [x] 046. Aumentar tamaño al seleccionar
- [x] 047. Mantener pin al terminar de calcular
- [ ] 048. Mantener pin después de recálculo
- [x] 049. Restaurar pin al abrir una ruta guardada
- [x] 050. Eliminar pin al borrar su punto
- [ ] 051. Mostrar distancia opcional al destino
- [x] 052. Confirmar selección con feedback breve
- [x] 053. Respetar coordenadas longitud/latitud
- [x] 054. Evitar pin duplicado en cambios rápidos
- [x] 055. Conservar pin durante cambio de estilo
- [ ] 056. Verificar visibilidad con zoom cercano
- [ ] 057. Verificar visibilidad con zoom lejano
- [x] 058. Crear iconos aptos para mapa oscuro
- [x] 059. No tapar el marcador del Subaru
- [ ] 060. Probar origen, destino y cuatro paradas

## Etapa 4 — Galería y vehículos

- [ ] 061. Unificar estética de miniaturas
- [x] 062. Conservar Subaru azul favorito
- [x] 063. Aumentar tamaño de ilustraciones
- [x] 064. Reorganizar galería en dos columnas
- [ ] 065. Evitar nombres cortados
- [x] 066. Mostrar selección claramente
- [x] 067. Agregar descripción accesible
- [x] 068. Mostrar grupo de cada vehículo
- [x] 069. Mantener alternativa de punto clásico
- [x] 070. Separar auto y fondo visualmente
- [x] 071. Optimizar bordes y contornos
- [ ] 072. Revisar orientación de capó
- [ ] 073. Revisar rotación en GPS
- [ ] 074. Revisar rotación en DEMO
- [x] 075. Conservar tamaño normal
- [x] 076. Conservar tamaño grande
- [x] 077. Conservar tamaño extra grande
- [x] 078. Guardar elección tras reiniciar
- [x] 079. Reducir consumo de memoria de sprites
- [x] 080. Probar contrastes sobre ruta violeta

## Etapa 5 — Rutas y mapa

- [x] 081. Verificar que DEMO no invente calles
- [x] 082. Distinguir ruta real y GPX
- [x] 083. Verificar respuesta de OSRM
- [x] 084. Rechazar geometrías inválidas
- [x] 085. Advertir rutas dudosas
- [ ] 086. Respetar sentido del trazado
- [ ] 087. No dibujar conexiones imposibles
- [ ] 088. Resaltar tramo activo
- [x] 089. Simplificar visualmente tramos completados
- [x] 090. Mantener destino visible
- [x] 091. Conservar origen al planificar
- [x] 092. Mantener rutas por encima del mapa base
- [x] 093. Evitar tapas de marcador por líneas
- [x] 094. Recalcular ante desvíos validados
- [x] 095. No recalcular por salto de GPS
- [ ] 096. Mantener paradas aún pendientes
- [ ] 097. Restaurar cámara de primera persona
- [ ] 098. Alternar 2D y 3D sin perder marcadores
- [ ] 099. Mantener zoom adaptable
- [ ] 100. Revisar modos satélite y oscuro

## Etapa 6 — Reductores y obstáculos confirmados

- [x] 101. Definir categorías de reductor
- [x] 102. Reconocer tags OSM traffic_calming
- [x] 103. Consultar reductores de la zona de ruta
- [x] 104. Hacer consulta solo cuando haga falta
- [x] 105. Limitar área consultada
- [x] 106. Manejar errores y falta de conexión
- [x] 107. Filtrar puntos próximos al trazado
- [x] 108. Rechazar obstáculos muy alejados
- [x] 109. Eliminar duplicados cercanos
- [x] 110. Identificar origen OSM o manual
- [x] 111. Mostrar símbolo de reductor en mapa
- [x] 112. Permitir agregar reductor manualmente
- [x] 113. Guardar reductores manuales localmente
- [x] 114. Permitir borrar registros manuales
- [x] 115. Mostrar metros hasta el próximo reductor
- [x] 116. Priorizar la nota de curva sobre aviso secundario
- [x] 117. No anunciar reductores desconocidos
- [x] 118. Evitar avisos repetidos del mismo punto
- [x] 119. Etiquetar información no verificada
- [ ] 120. Probar diversos trazados en Trelew

## Etapa 7 — Notas, voz y anticipaciones

- [ ] 121. Conservar notas 1 a 6
- [ ] 122. Mantener actual, siguiente y tercera
- [x] 123. Verificar transición al pasar curva
- [ ] 124. Evitar notas repetidas
- [x] 125. Mantener voz de 800 metros
- [x] 126. Mantener cuenta 3-2-1 GO
- [x] 127. Integrar anuncio opcional de reductor
- [x] 128. Bajar volumen de comentarios ante curva
- [x] 129. Respetar voz activada o silenciada
- [x] 130. No mezclar grabaciones de idiomas
- [x] 131. Ajustar frecuencia del copiloto contextual
- [x] 132. Evitar dos mensajes simultáneos
- [ ] 133. Respetar pausa de DEMO
- [ ] 134. Revisar frases de curva larga
- [ ] 135. Revisar enlace de curvas
- [ ] 136. Conservar nota estimada visible
- [ ] 137. Evitar órdenes de velocidad insegura
- [ ] 138. Comprobar claridad de voz en parlantes
- [ ] 139. Agregar prueba de previsualización
- [ ] 140. Probar simulación de múltiples curvas

## Etapa 8 — Ajustes, accesibilidad y usabilidad

- [x] 141. Ordenar ajustes por categorías
- [x] 142. Usar iconos comprensibles
- [x] 143. Agregar accesibilidad a botones
- [ ] 144. Aumentar áreas táctiles
- [x] 145. Respetar escalado de fuentes
- [x] 146. Evitar texto cortado en español
- [ ] 147. Revisar textos en inglés
- [ ] 148. Revisar textos en portugués
- [x] 149. Conservar preferencias al actualizar
- [ ] 150. Agregar explicación breve solo si ayuda
- [ ] 151. Evitar popups repetitivos
- [ ] 152. Permitir corregir destino antes de navegar
- [x] 153. Restringir edición con el auto en movimiento
- [ ] 154. Conservar modo compacto
- [ ] 155. Conservar panel de notas grande
- [x] 156. Mostrar estados sin consumir mapa
- [ ] 157. Revisar orientación horizontal
- [ ] 158. Revisar modo nocturno
- [ ] 159. Probar TalkBack
- [x] 160. Revisar idioma inicial

## Etapa 9 — Pruebas y rendimiento

- [ ] 161. Crear pruebas de pin visible
- [ ] 162. Crear pruebas de persistencia de destino
- [ ] 163. Probar estados de barra de búsqueda
- [ ] 164. Probar límites de vehículo seleccionado
- [ ] 165. Probar mapa cargado sin ruta
- [ ] 166. Probar mapa cargado con ruta
- [ ] 167. Probar cambio de estilo de mapa
- [ ] 168. Crear pruebas del parser OSM
- [x] 169. Probar matching de reductores
- [x] 170. Probar filtros de distancias
- [x] 171. Probar deduplicación
- [ ] 172. Probar rechazo de GPS impreciso
- [x] 173. Probar aviso de reductor y curva próxima
- [x] 174. Probar silencio de avisos
- [ ] 175. Probar pérdida de internet
- [x] 176. Probar persistencia de reductores manuales
- [x] 177. Probar JUnit completo
- [x] 178. Compilar APK Android
- [x] 179. Inspeccionar integridad de APK
- [ ] 180. Probar visualmente con capturas

## Etapa 10 — Publicación y mantenimiento

- [x] 181. Actualizar número de versión
- [ ] 182. Escribir changelog de la versión
- [x] 183. Documentar fuentes OSM y atribución
- [ ] 184. Documentar límites de API pública
- [x] 185. Explicar reductores no detectados
- [x] 186. Documentar flujo de marcación
- [x] 187. Documentar opciones de vehículos
- [ ] 188. Comprobar firma de APK
- [ ] 189. Resguardar compatibilidad de datos
- [x] 190. Preparar rama de integración
- [x] 191. Crear Pull Request para revisión
- [x] 192. Verificar GitHub Actions
- [ ] 193. Examinar informes de errores
- [ ] 194. Corregir fallas de compilación
- [ ] 195. Conservar Release anterior disponible
- [ ] 196. Publicar prerelease solo al aprobar
- [ ] 197. Comprobar enlace permanente de APK
- [ ] 198. Definir prueba segura en Motorola
- [ ] 199. Recoger observaciones de la versión
- [ ] 200. Cerrar tareas y dejar pendientes visibles
