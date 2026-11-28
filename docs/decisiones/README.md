# Registro de decisiones

Cada decisión técnica relevante tiene su propio archivo numerado (`0001-...md`).
Una decisión aceptada no se edita: si cambia, se escribe una nueva que la sustituye y se enlazan.
Única excepción: corregir un error de hecho que no cambia la decisión (por ejemplo, el nombre de un
dispositivo). La corrección se hace en el mismo texto, indicando su fecha.

## Estados

- **Propuesta:** escrita y pendiente de aprobación del propietario.
- **Aceptada:** aprobada; el código debe seguirla.
- **Sustituida:** reemplazada por otra decisión, que se indica.
- **Rechazada:** se valoró y se descartó; se conserva para no repetir el análisis.

## Plantilla

```markdown
# NNNN. Título

Fecha: AAAA-MM-DD. Estado: Propuesta.

## Contexto
Qué problema hay que resolver y qué restricciones aplican.

## Alternativas consideradas
Cada opción con sus ventajas e inconvenientes.

## Decisión
Qué se elige y por qué.

## Consecuencias
Qué implica, qué se gana, qué se pierde y qué queda pendiente.
```

## Índice

| Número | Título | Estado |
|---|---|---|
| 0001 | Licencia del proyecto: GPL-3.0-or-later | Aceptada |
| 0002 | Versión mínima: Android 8.0 (API 26) | Aceptada |
| 0003 | Mensajería en malla compatible con el protocolo de BitChat | Aceptada |
| 0004 | Mapas sin conexión con MapLibre y PMTiles | Aceptada |
| 0005 | Guía propia primero; Kiwix como ampliación posterior | Aceptada |
| 0006 | Generación y distribución de los paquetes por municipio | Aceptada (límites y firma sustituidos por la 0013) |
| 0007 | Formato abierto para los datos oficiales de los ayuntamientos | Aceptada |
| 0008 | Arquitectura de la app y librerías base | Aceptada |
| 0010 | Primer mapa sin conexión: MapLibre, estilo y recursos | Aceptada |
| 0011 | Puntos de interés: tipos, formato y presentación | Aceptada |
| 0012 | Repetir las comprobaciones solo cuando falla una descarga | Aceptada |
| 0013 | Paquetes de toda España: límites, firma, publicación y descarga | Aceptada (publicación sustituida por la 0014) |
| 0014 | Publicación mensual de los paquetes | Aceptada |
| 0015 | Reintentos de la publicación mensual cuando Geofabrik rechaza la descarga | Aceptada |
| 0016 | Municipios con un límite incompleto o mal etiquetado en OpenStreetMap | Propuesta |

## Próximas decisiones (pendientes de investigar)

- **Parte 3c. Elegir y descargar el municipio en la app** (decisiones 0013 y 0014). Ya acordado o medido:
  - la app muestra la fecha de los datos de su paquete (por ejemplo, "Datos de noviembre de 2026") y,
    cuando hay conexión y una versión más reciente, sugiere actualizar sin hacerlo nunca por su cuenta
    (acordado con el propietario el 2026-11-27);
  - el índice de toda España ocupa 5,8 MB (1,5 MB con gzip): hay que decidir cómo descargarlo sin gastar
    datos de más (apartado 11 de la investigación 10);
  - la app exige la respuesta 206 a cada descarga parcial, y hay que comprobar con un servidor de prueba
    que OkHttp mantiene la cabecera `Range` tras la redirección de GitHub.
- **Paquetes de Ceuta, Casariche y Campo** (apartado 12 de la investigación 10): corregir los datos en
  OpenStreetMap o hacer el generador tolerante a un contorno incompleto o a un nivel administrativo
  distinto.

- **0009. Estrategia de pruebas con dispositivos reales.** Solo hay un móvil disponible: un Samsung
  Galaxy A51 con crDroid (Android 16, 4 GB de RAM). No es de gama baja ni cubre Android 8 a 11. Hay que
  definir qué se prueba en el emulador, qué en el A51, cómo cubrir esos dos huecos y cómo conseguir un
  segundo móvil para la malla.
