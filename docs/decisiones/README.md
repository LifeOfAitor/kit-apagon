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
| 0006 | Generación y distribución de los paquetes por municipio | Aceptada |
| 0007 | Formato abierto para los datos oficiales de los ayuntamientos | Aceptada |
| 0008 | Arquitectura de la app y librerías base | Aceptada |
| 0010 | Primer mapa sin conexión: MapLibre, estilo y recursos | Aceptada |
| 0011 | Puntos de interés: tipos, formato y presentación | Aceptada |

## Próximas decisiones (pendientes de investigar)

- **0009. Estrategia de pruebas con dispositivos reales.** Solo hay un móvil disponible: un Samsung
  Galaxy A51 con crDroid (Android 16, 4 GB de RAM). No es de gama baja ni cubre Android 8 a 11. Hay que
  definir qué se prueba en el emulador, qué en el A51, cómo cubrir esos dos huecos y cómo conseguir un
  segundo móvil para la malla.
