# Registro de decisiones

Cada decisión técnica relevante tiene su propio archivo numerado (`0001-...md`).
Una decisión aceptada no se edita: si cambia, se escribe una nueva que la sustituye y se enlazan.

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
| 0006 | Generación y distribución de los paquetes por municipio | Propuesta |
| 0007 | Formato abierto para los datos oficiales de los ayuntamientos | Propuesta |

## Próximas decisiones (pendientes de investigar)

- **0008. Arquitectura de la app y librerías base.** Interfaz, almacenamiento local, inyección de
  dependencias y librerías de tests, elegidas pensando en un móvil de 2 GB de RAM.
- **0009. Estrategia de pruebas con dispositivos reales.** Solo hay un móvil disponible (Samsung
  Galaxy A20). Hay que definir qué se prueba en el emulador, qué en el A20 y cómo conseguir un
  segundo móvil para la malla.
