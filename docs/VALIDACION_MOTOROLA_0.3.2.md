# Matriz de prueba real — Motorola (Rally GPS AI 0.3.2)

**No probar funciones tocando el teléfono mientras se conduce.** Usar DEMO detenido o acompañante en recorrido real permitido.

## Antes de salir
- [ ] Guardar GPX y datos que se deseen conservar. La APK debug puede usar firma diferente de otra instalación.
- [ ] Instalar encima de 0.3.1 y comprobar que conserva vehículo, idioma, preferencias, historial y reductores manuales.
- [ ] Abrir galería: miniaturas y nombres completos, Subaru por defecto, Normal/Grande/Extra grande.
- [ ] Abrir oscuro y día; comprobar que reaparecen ruta, pines, coche y reductores.
- [ ] Probar zoom cercano/lejos y estilo en las calles de Trelew, Rawson y Puerto Madryn.
- [ ] Marcar origen, destino y cuatro paradas: verificar etiquetas sin duplicados y pines sin tapa.
- [ ] Probar con fuentes accesibles grandes y TalkBack, especialmente botones GPX, Ajustes y PUNTOS.
- [ ] Girar el teléfono en modo planificación y verificar que nada sea inaccesible. Si el modo horizontal tapa información, registrar captura y no cerrar esta tarea.

## DEMO detenido
- [ ] Buscar destino y comprobar que no aparece cartel verde redundante.
- [ ] Iniciar 3-2-1-GO, escuchar notas y distancia en dos idiomas soportados.
- [ ] Comprobar que buscador y herramientas de edición están ocultos durante DEMO.
- [ ] Avanzar sobre varias curvas y confirmar nota actual/siguiente/tercera sin repetición.
- [ ] PAUSA: verificar que posición del Subaru y metros quedan congelados; REANUDAR continúa sin volver al inicio.
- [ ] Pulsar rápido PAUSA/REANUDAR y verificar que no se acelera por duplicación de temporizadores.
- [ ] Alternar vista 2D/3D y centrar mapa; comprobar rumbo y que el auto no se invierte.
- [ ] Asegurarse de que se diferencia tramo realizado del pendiente.
- [ ] Agregar reductor manual antes de simular y verificar marca y aviso; borrarlo y comprobar que desaparece.

## Con acompañante (conducción normal)
- [ ] Fijar teléfono sin manipularlo. Probar permiso GPS preciso y señalar si hay cobertura baja.
- [ ] Iniciar una ruta real OSRM y verificar que corresponde a calles transitables.
- [ ] Comparar notas de 800m y próximas con puntos reales sin obedecerlas como indicaciones de velocidad.
- [ ] Validar recálculo tras desvío permitido, no por saltos GPS.
- [ ] Confirmar que el punto de destino sigue marcado tras recálculo.
- [ ] Comprobar que las paradas restantes no desaparecen.
- [ ] Verificar rutas con reductores existentes: fuente OSM/manual, distancia, posición, duplicados.
- [ ] Apagar conexión y comprobar respuesta segura; restaurar conexión y reintentar manualmente.
- [ ] Revisar claridad de voz en parlantes del vehículo y con música.
- [ ] Verificar que no queda pantalla encendida al finalizar.

## Al terminar
- [ ] Historial: kilómetros, tiempo, media y máxima solo GPS real; DEMO no se registra.
- [ ] Abrir mapa del viaje, repetir ruta y exportar/importar GPX.
- [ ] Guardar capturas y descripción de cualquier problema sin exponer coordenadas privadas.

**Cierre:** la validación física requiere evidencia manual en este documento; un CI verde no autoriza marcar estas casillas.
