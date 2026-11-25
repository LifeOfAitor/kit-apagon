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
| 0001 | Licencia del proyecto: GPL-3.0-or-later | Propuesta |
| 0002 | Versión mínima: Android 8.0 (API 26) | Propuesta |
| 0003 | Mensajería en malla compatible con el protocolo de BitChat | Propuesta |
| 0004 | Mapas sin conexión con MapLibre y PMTiles | Propuesta |
| 0005 | Guía propia primero; Kiwix como ampliación posterior | Propuesta |
