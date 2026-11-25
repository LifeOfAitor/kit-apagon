# 0003. Mensajería en malla compatible con el protocolo de BitChat

Fecha: 2026-11-25. Estado: Aceptada.

## Contexto

La mensajería entre vecinos sin red es la parte técnicamente más difícil. Ver
`docs/investigacion/01-mensajeria-en-malla.md`.

## Alternativas consideradas

1. **Protocolo propio desde cero.** Control total, pero meses de trabajo y riesgo alto de errores en
   un área (radio, cifrado) donde los errores son difíciles de detectar.
2. **Compatible con BitChat.** Especificación de dominio público, implementación Android de
   referencia en Kotlin (GPL-3) y versión de iOS ya existente. Nuestros usuarios podrían hablar con
   cualquier usuario de BitChat.
3. **Basado en Briar.** Más seguro, pero sin iOS y con alto consumo de batería reconocido.
4. **Basado en Berty.** Interoperabilidad entre Android e iOS sin resolver y arquitectura pesada.

## Decisión

Implementar solo la parte de malla Bluetooth del protocolo de BitChat, sin Nostr, Tor ni canales
geográficos. Se escribe con nuestras normas de código limpio y tests, tomando como referencia su
especificación y su código Android, que podemos reutilizar gracias a la licencia de la decisión 0001.

## Consecuencias

- Antes de programar hay que leer `WHITEPAPER.md` completo y revisar a fondo su código de malla.
- Hacen falta al menos dos móviles Android reales para probar, y mejor tres.
- BitChat no tiene auditoría de seguridad externa: la app lo dirá y no se venderá como canal seguro
  para información sensible.
- Si BitChat cambia su protocolo, habrá que seguir esos cambios para mantener la compatibilidad.
- Depende de que se acepte la decisión 0001.
