# Investigación: contenidos sin conexión y fuentes oficiales

Fecha: 2026-11-25. Estado: primera revisión.

## Pregunta

¿Qué guía y qué contenidos de consulta puede llevar la app sin red, con fuentes fiables y
respetando las licencias?

## Kiwix (Wikipedia y otros contenidos sin conexión)

Kiwix lee archivos `.zim`, que empaquetan sitios web completos para consultarlos sin red.

| Pieza | Licencia |
|---|---|
| `kiwix/kiwix-android` | GPL-3.0 |
| `kiwix/libkiwix` | GPL-3.0 |
| `openzim/libzim` | GPL-2.0 (comprobar si es "or later" antes de combinarla con GPL-3) |
| Contenido de Wikipedia | CC BY-SA 4.0 |

Tamaños en español consultados en el catálogo oficial (library.kiwix.org, 2026-11-25):

| Paquete | Tamaño |
|---|---|
| Wikipedia médica, sin imágenes (`wikipedia_es_medicine nopic`) | 171 MB |
| Wikipedia médica, con imágenes | 469 MB |
| Lo mejor de Wikipedia, versión mínima (`wikipedia_es_top mini`) | 217 MB |
| Wikipedia completa, versión mínima | 4,1 GB |
| Wikipedia completa, sin imágenes | 13,3 GB |
| MedlinePlus en español | 1,8 GB |
| Wikiviajes, sin imágenes | 43 MB |

Conclusión para Kiwix: es viable, pero no para la primera versión. Un móvil de 2 GB de RAM suele
tener poco almacenamiento, y el núcleo de la app tiene que ser ligero. Propuesta: en una fase
posterior, descarga opcional de la Wikipedia médica (171 MB). Una alternativa sin integrar código
es abrir los archivos con la app oficial de Kiwix si el usuario la tiene instalada.

## Guía propia: la pieza principal

Lo más útil en un apagón no es una enciclopedia, sino una guía corta, clara y oficial. Fuentes
candidatas (pendientes de revisar una a una en `docs/FUENTES.md`):

- **Ministerio del Interior, Dirección General de Protección Civil y Emergencias:** modelo de plan
  familiar de emergencias (mayo de 2025), con un kit básico para 72 horas.
- **Ministerio del Interior y Ministerio de Educación:** guía de prevención y autoprotección para familias.
- **Generalitat de Catalunya, Protección Civil:** la guía más detallada en cantidades (agua: mínimo
  1,5 litros por persona y día para beber, recomendado 3 litros para beber y cocinar).
- **Comisión Europea, Estrategia de la Unión de la Preparación (26 de marzo de 2025):** recomienda
  autosuficiencia mínima de 72 horas y dice expresamente que no existe un kit único para todos.

Fuentes descartadas como origen: artículos de prensa, contenido patrocinado y webs comerciales.
Solo sirven para encontrar el documento oficial original.

## Condiciones legales para reutilizar contenidos públicos

- **Sector público estatal:** la Ley 37/2007 de reutilización de la información del sector público
  y el Real Decreto 1495/2011 permiten reutilizar con condiciones generales: citar la fuente, no
  desnaturalizar el sentido de la información y no sugerir que el organismo respalda la app.
  El aviso legal del Ministerio del Interior sigue este esquema. Esto encaja con el principio 9.
- **Comunidades autónomas:** cada una tiene su propio aviso legal. Hay que revisarlo antes de usar su contenido.
- **Organizaciones privadas (por ejemplo, Cruz Roja):** no les aplica la Ley 37/2007. Hace falta
  una licencia abierta explícita o su permiso por escrito.

Regla práctica: redactar nuestros propios textos a partir de las fuentes oficiales, citando cada
una, en lugar de copiar documentos enteros.

## Fuentes

- https://library.kiwix.org/catalog/v2/entries?lang=spa
- https://github.com/kiwix/kiwix-android y https://github.com/kiwix/libkiwix
- https://www.boe.es/eli/es/l/2007/11/16/37/con
- https://www.interior.gob.es/opencms/es/aviso-legal/
- https://recursosemergencias.educacionfpydeportes.gob.es/dam/jcr:53d2439d-69e5-4606-a79f-3fe1610638cd/esp-guiafamiliar.pdf
- https://commission.europa.eu/topics/preparedness-union_en
- https://reliefweb.int/report/world/factsheet-eu-preparedness-union-strategy
