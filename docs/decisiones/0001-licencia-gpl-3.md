# 0001. Licencia del proyecto: GPL-3.0-or-later

Fecha: 2026-11-25. Estado: Aceptada.

## Contexto

La licencia condiciona qué código ajeno podemos reutilizar. Las piezas más valiosas que hemos
encontrado son GPL-3: BitChat para Android, Briar, Kiwix y Meshtastic. Además, una herramienta de
emergencia para la ciudadanía gana confianza si cualquiera puede revisar su código.

## Alternativas consideradas

1. **GPL-3.0-or-later.** Permite reutilizar todo lo anterior y también el código Apache-2.0, MIT,
   BSD y de dominio público. Obliga a que cualquier versión distribuida publique su código con la
   misma licencia, así que nadie puede cerrar el proyecto y venderlo cerrado.
2. **Apache-2.0 o MIT.** Más permisivas y cómodas para empresas, pero impiden reutilizar código GPL.
   Habría que escribir la malla desde cero a partir de la especificación.
3. **EUPL-1.2.** Licencia de la Unión Europea pensada para administraciones públicas. Es compatible
   con GPL-3 en un sentido, pero combinarla con código GPL-3 añade complejidad legal innecesaria.
4. **Código privado.** Contradice el objetivo de que el ayuntamiento y la ciudadanía confíen en la app.

## Decisión

GPL-3.0-or-later, con repositorio público.

## Consecuencias

- Podemos estudiar y reutilizar código de BitChat para Android, Briar y Kiwix, citándolo en
  `docs/licencias-de-terceros.md` y manteniendo sus avisos de copyright.
- Todo el código del proyecto es público desde el primer día.
- GitHub Actions es gratuito e ilimitado para repositorios públicos.
- Google Play y F-Droid aceptan apps GPL-3.
- Un ayuntamiento puede usar, modificar y distribuir la app libremente, siempre que publique sus cambios.
