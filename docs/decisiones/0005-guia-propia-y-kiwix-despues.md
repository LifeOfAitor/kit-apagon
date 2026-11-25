# 0005. Guía propia primero; Kiwix como ampliación posterior

Fecha: 2026-11-25. Estado: Propuesta.

## Contexto

La app debe incluir una guía sin conexión. Ver `docs/investigacion/03-contenidos-sin-conexion-y-fuentes.md`.

## Alternativas consideradas

1. **Integrar Kiwix y Wikipedia desde el principio.** Mucho contenido, pero cientos de MB o varios GB,
   librerías nativas pesadas y contenido general que no está pensado para una emergencia.
2. **Guía propia, corta y con fuentes oficiales.** Pocos KB, rápida en cualquier móvil, revisable
   línea a línea y adaptada al municipio.
3. **Ambas a la vez.** Duplica el trabajo de la primera versión.

## Decisión

La primera versión lleva una guía propia redactada a partir de fuentes oficiales, cada una citada en
`docs/FUENTES.md`. Kiwix, empezando por la Wikipedia médica sin imágenes (171 MB), se valorará como
descarga opcional en una fase posterior, con su propia decisión.

## Consecuencias

- Hay que redactar los textos de la guía y revisar el aviso legal de cada fuente.
- La app inicial se mantiene pequeña.
- Queda abierta una decisión futura sobre cómo integrar Kiwix.
