# Visual Style

## 1. Propósito

Este documento define preferencias generales para diseño visual, interfaces, presentaciones y generación de imágenes.

Son valores por defecto, no reglas absolutas.

Las instrucciones específicas de un proyecto tienen prioridad.

## 2. Principios generales

Prefiero diseños:

- Limpios.
- Funcionales.
- Relativamente minimalistas.
- Con jerarquía visual evidente.
- Con pocos elementos puramente decorativos.
- Fáciles de entender antes que visualmente complejos.

No llenar un diseño de elementos solo para hacerlo parecer más trabajado.

Una solución visual sencilla puede ser preferible a una más llamativa si comunica mejor.

## 3. Jerarquía y composición

- Debe existir una idea o elemento principal claro.
- La composición no debe competir consigo misma.
- Utilizar espacio vacío cuando ayude a separar información.
- Evitar que todos los componentes tengan el mismo peso visual.
- Agrupar información relacionada.
- No crear secciones adicionales únicamente para llenar espacio.
- Mantener una relación clara entre función y forma.

## 4. Color

Preferencia general:

- Utilizar una paleta relativamente limitada.
- No depender de muchos colores para establecer jerarquía.
- Introducir color cuando tenga una función clara: énfasis, estado, categoría o identidad.

Evitar por defecto:

- Gradientes innecesarios.
- Combinaciones excesivamente saturadas.
- Neón sin una razón temática.
- Efectos de color destinados únicamente a hacer que algo parezca futurista.

### Coherencia cromática y eliminación de acentos dinámicos/default

- **Dominio estricto del color de marca:**
  - Toda la interfaz debe mantener coherencia absoluta con el color principal de la app combinado únicamente con una escala de grises neutra
  - Anular e impedir acentos dinámicos no controlados
  - Los estados de selección, enfoque o resalte deben usar exclusivamente un tono atenuado del color principal  o gris neutro.

## 5. Interfaces

Prefiero:

- Espacios amplios.
- Componentes simples.
- Jerarquía clara.
- Bordes semi-cuadrados.
- Información agrupada de forma evidente.
- Diferencias elementos por colores y no bordes
- Controles que expliquen su función sin decoración adicional.
- Interfaces que permitan entender rápidamente qué hacer.

Evitar:

- Sombras exageradas.
- Que todos los elementos tengan lineas como bordes si ya se diferencian visualmente por color, forma, etc.
- Demasiadas tarjetas.
- Iconos decorativos sin función.
- Listas planas, varios atributos separados simplemente por comas (en su lugar usar pills u otro)
- Contenedores dentro de contenedores sin necesidad.
- Interfaces que parezcan una plantilla corporativa genérica.
- Añadir dashboards, métricas o gráficos si no responden a una necesidad real.

### Preferencias para formularios e inputs (UI)

- **Campos de entrada (Inputs):**
  - Usar fondo de relleno con contraste sutil sobre el respectivo fondo para delimitar claramente la zona interactiva.
  - Mantener animación de *floating hint* e indicador de línea inferior (*filled box*) con trazo en el color primario de la marca al enfocar.
  - El texto de sugerencia (*hint/placeholder*) debe ser opaco para diferenciarse del texto ingresado.
  - Estilizar selectores/dropdowns (Spinners) con el mismo fondo y bordes semi-cuadrados (`4dp`) para mantener consistencia con los inputs de texto.

- **Estructura y márgenes:**
  - Garantizar margen o padding lateral mínimo en formularios para evitar que los campos se peguen a los bordes de pantalla en dispositivos móviles
  - Evitar tarjetas contenedoras cuando no aportan distincion (e.g. una pagina con varios formularios expandibles se beneficiaria, pero una pantalla de login que solo tiene un formulario no)

- **Atributos y datos secundarios:**
  - Nunca concatenar atributos en cadenas planas de texto. Presentar datos numéricos o estados mediante *pills/chips* independientes con bordes semi-cuadrados (`4dp`).
  - No utilizar emojis decorativos en notificaciones (*Toasts*), botones ni encabezados.

## 6. Presentaciones

Para diapositivas o material visual de exposición:

- Una idea principal por diapositiva cuando sea posible.
- Poco texto si la explicación será oral.
- No convertir párrafos enteros en diapositivas.
- Preferir diagramas o escenarios concretos cuando expliquen mejor una idea.
- Utilizar contraste visual para apoyar comparaciones.
- No abusar de negritas.
- No utilizar emojis como decoración.
- Evitar títulos excesivamente grandilocuentes.
- No intentar llenar todo el espacio disponible.

Cuando haya una pregunta o dilema, puede ocupar visualmente el centro de la diapositiva.

## 7. Generación de imágenes

Prioridad general:

composición → iluminación → detalle → efectos.

La imagen debe tener una idea central clara.

Prefiero:

- Composiciones relativamente simples.
- Elementos con una función dentro de la escena.
- Iluminación coherente con el contexto.
- Detalle suficiente para sostener la imagen, no detalle añadido por acumulación.
- Escenas que parezcan intencionales en lugar de generadas a partir de una lista de adjetivos.

Evitar añadir automáticamente:

- Partículas.
- Lens flare.
- Neón.
- Cyberpunk.
- Textos flotantes.
- Hologramas.
- Profundidad de campo extrema.
- Brillos excesivos.
- Luces volumétricas sin justificación.
- Fondos saturados de objetos.
- Efectos "cinematográficos" solo para aumentar dramatismo.

Estos recursos se pueden utilizar cuando el concepto realmente los requiera.

## 8. Prompts para imágenes

Al crear un prompt:

1. Definir primero el sujeto o escena.
2. Definir después la composición.
3. Indicar relación entre elementos.
4. Especificar iluminación.
5. Añadir estilo visual.
6. Añadir detalles secundarios.
7. Incluir restricciones negativas solo cuando ayuden.

No construir prompts como acumulaciones interminables de adjetivos.

Cuando una elección visual sea ambigua, preferir una solución clara y funcional antes que una espectacular.

## 9. Imágenes personales o representativas

No inventar rasgos físicos míos.

No representar automáticamente mis intereses profesionales mediante clichés como:

- Código flotando.
- Hologramas.
- Cerebros digitales.
- Robots humanoides.
- Habitaciones llenas de pantallas.

Si una imagen debe representarme personalmente, utilizar solo información visual proporcionada específicamente para esa tarea.

## 10. Relación entre estilo visual y contenido

El diseño debe apoyar la función.

Ejemplos:

- Un concepto técnico complejo puede requerir un diagrama.
- Una opinión puede funcionar mejor con una composición simple y contrastante.
- Una presentación ética puede utilizar una escena o dilema visual.
- Una interfaz de productividad debe priorizar claridad operativa.

No aplicar minimalismo de manera que se pierda información necesaria.

## 11. Estado de estas preferencias

Estas preferencias visuales son una base inicial.

Cuando existan referencias visuales proporcionadas por mí, esas referencias tienen más peso que este documento.

Con el tiempo, este archivo puede ampliarse con:

- Colores preferidos.
- Tipografías.
- Fotografía.
- Arquitectura.
- Ilustración.
- Estilos 3D.
- UI de referencia.
- Composición.
- Moda.
- Interiores.
- Ejemplos positivos y negativos.
