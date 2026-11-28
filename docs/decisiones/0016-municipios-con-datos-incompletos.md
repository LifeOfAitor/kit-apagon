# 0016. Municipios con un límite incompleto o mal etiquetado en OpenStreetMap

Fecha: 2026-11-28. Estado: Propuesta.

Completa las decisiones 0013 y 0014. El resto de ambas sigue vigente.

## Contexto

La primera versión completa dejó sin paquete a 3 de los 8.132 municipios oficiales (apartados 12 y 14 de
la investigación 10):

- **Ceuta.** Su relación está bien etiquetada, pero a su contorno le faltan 4 tramos de costa en Benzú,
  junto a la frontera con Marruecos, en todos los extractos de Geofabrik, y osmium no puede cerrarlo.
- **Casariche.** Una edición del 24 de noviembre de 2026 cambió su `admin_level` de 8 a 7.
- **Campo.** Una edición del 12 de noviembre de 2026 le quitó `boundary=administrative` y
  `admin_level=8`.

Las dos últimas parecen despistes: las ediciones se comentaron con "ww" y "atualização", y la de Campo
tocó también una carretera de la zona y una vía de tren en Portugal. Algo así puede volver a pasar
cualquier mes con cualquier municipio.

## Alternativas consideradas

1. **Corregir los datos en OpenStreetMap.** Es lo correcto para Casariche y Campo, porque beneficia a
   todos los que usan esos datos. Pero no arregla Ceuta, que no tiene un error de etiquetas, y no
   protege de un error nuevo el mes siguiente.
2. **Aceptar en el generador cualquier límite con un código oficial**, aunque tenga otro nivel. Arregla
   Casariche, pero no Campo, que solo aparece en el extracto por casualidad, ni Ceuta.
3. **Pedir el contorno a la API principal de OpenStreetMap.** Descartada: sus normas dicen que la API es
   para editar el mapa y no para proyectos que solo leen.
4. **Pedir a Overpass el contorno de cada municipio oficial que no salga bien del extracto.** Overpass es
   el servicio pensado para consultas de lectura, y admite unas 10.000 consultas y 1 GB al día. Resuelve
   los tres casos (probado el 2026-11-28, entre 25 y 33 segundos por consulta).

## Decisión

Se eligen la alternativa 4 en el generador y la 1 en los datos.

1. **Después de exportar el extracto**, el trabajo mensual busca los municipios oficiales que no tienen un
   límite de nivel 8. Si son 20 o menos, pide cada uno a Overpass por su código (`ine:municipio`),
   construye el contorno con osmium y lo añade a los límites. Si son más de 20, el extracto está
   probablemente roto: no se consulta nada y se aplica la regla del 1 % de la decisión 0014.
2. **Un límite que llega de Overpass se acepta aunque su nivel no sea 8**, porque su código está en la
   relación oficial. Si Overpass devuelve varias relaciones para el mismo código, se usa la de nivel 8 y,
   si ninguna lo es, no se usa ninguna.
3. **Si Overpass no responde o rechaza la consulta**, ese municipio se queda sin paquete ese mes, la
   publicación sigue y el informe lo dice.
4. **El informe de cada versión** lista los municipios completados desde Overpass y el motivo:
   - "contorno incompleto en el extracto" cuando su nivel es 8;
   - "etiquetado con `admin_level` X en OpenStreetMap, conviene corregirlo" en los demás casos.
5. **Identificación ante Overpass:** Overpass rechaza las consultas sin un identificador de la
   aplicación (lo comprobamos con un 406). El trabajo se identifica como
   `kit-apagon-datos (+https://github.com/LifeOfAitor/kit-apagon-datos)`, la dirección pública del
   repositorio de datos.
6. **El propietario corrige Casariche y Campo en OpenStreetMap**, con su cuenta, devolviéndoles las
   etiquetas que tenían.

## Consecuencias

- Los tres municipios tendrán paquete desde la próxima versión, aunque las correcciones en OpenStreetMap
  tarden.
- El trabajo mensual depende de un servicio más, pero solo para los pocos municipios que fallen, y si
  falla no detiene la publicación.
- Hasta 20 consultas de unos 30 segundos: como mucho 10 minutos más al mes.
- Los informes mensuales sirven también para avisar de errores en OpenStreetMap que alguien puede corregir.
