# Investigación: mensajería en malla por Bluetooth

Fecha: 2026-11-25. Estado: primera revisión.

## Pregunta

¿Qué proyectos existentes resuelven la mensajería entre móviles sin red, qué podemos reutilizar
legalmente y qué limitaciones técnicas hay que asumir?

## Proyectos revisados

| Proyecto | Licencia (comprobada en el repositorio) | Plataformas | Actividad | Valoración |
|---|---|---|---|---|
| BitChat para iOS (`permissionlesstech/bitchat`) | Unlicense (dominio público) | iOS, macOS | Muy activo | Contiene `WHITEPAPER.md` con la especificación del protocolo. |
| BitChat para Android (`permissionlesstech/bitchat-android`) | GPL-3.0 (el archivo `LICENSE.md` es GPL-3, aunque el README dice "dominio público") | Android 8+ (minSdk 26) | Muy activo | Kotlin, Jetpack Compose, corrutinas. Compatible con la versión de iOS. Sin auditoría de seguridad externa, según su propio README. |
| Briar | GPL-3.0-or-later | Android (el cliente de escritorio no tiene Bluetooth) | Activo, en mantenimiento | Muy sólido en seguridad. Consumo de batería alto reconocido por el propio proyecto. Sin iOS. |
| Berty | Apache-2.0 o MIT, a elegir | Android, iOS | Activo | La interoperabilidad Bluetooth entre Android e iOS aparece como reto abierto en su propia web. Arquitectura en Go, pesada para nuestro objetivo. |
| Meshtastic (`meshtastic/Meshtastic-Android`) | GPL-3.0 | Android + radio LoRa externa | Muy activo | Necesita hardware (20-30 €). Interesante como ampliación futura para el ayuntamiento, no para la primera versión. |
| Bridgefy | Comercial | Android, iOS | - | Descartado: de pago y con fallos de seguridad publicados. |
| Google Nearby Connections | Propietaria | Android con Google Play | - | Descartado: depende de Google Play Services (contradice el principio 8). |

## Lo que hace bien BitChat para Android y conviene estudiar

- Malla Bluetooth LE con reenvío en varios saltos (máximo 7), TTL y eliminación de duplicados.
- Formato de paquete binario compacto con fragmentación.
- Cifrado de extremo a extremo con el protocolo Noise (patrón XX, X25519 + ChaCha20-Poly1305).
- Gestión de energía (`mesh/PowerManager.kt`): cuatro modos (rendimiento, equilibrado, ahorro,
  ultra bajo consumo) y tres franjas de batería (normal, baja, crítica). Ajustan el modo de
  escaneo y de anuncio de Bluetooth y alternan periodos de escaneo y de descanso.
- Servicio en primer plano para mantener la malla viva dentro de los límites de Android.
- Su propio README reconoce que el comportamiento de radio no se puede emular: la lógica de
  protocolo se prueba con tests unitarios y la radio necesita móviles reales.

Lo que no necesitamos: el transporte por internet (Nostr), Tor y los canales geográficos.
Añaden complejidad y superficie de ataque sin aportar nada en un apagón.

## Limitaciones técnicas confirmadas

1. **iOS en segundo plano.** Cuando una app de iOS anuncia por Bluetooth en segundo plano, sus
   identificadores pasan a un área de desbordamiento propietaria que Android no puede leer.
   Un iPhone con la app cerrada apenas participa en la malla con Android. Fuente: ingenieros de
   Apple en los foros de desarrolladores (hilos 814450 y 773100).
2. **Emulador.** El emulador de Android no reproduce Bluetooth real. Hacen falta al menos dos
   móviles Android físicos para probar la malla, y mejor tres para probar el reenvío.
3. **Fabricantes.** Algunas capas de fabricante (por ejemplo Xiaomi HyperOS) suspenden servicios
   en primer plano en reposo profundo. Hay que probar en varias marcas.

## Conclusión

La opción más prometedora es **implementar el protocolo de BitChat**:

- La especificación es de dominio público, así que podemos implementarla sin restricciones.
- El código Android es GPL-3. Si nuestro proyecto es GPL-3, podemos reutilizar partes de él
  citando su origen (ver propuesta de decisión 0001).
- Ser compatibles con BitChat significa que nuestros usuarios podrían hablar también con
  cualquiera que tenga BitChat instalado, en Android o en iPhone. La malla crece sin esfuerzo nuestro.

Riesgo a vigilar: BitChat no tiene auditoría de seguridad externa. Para mensajes de emergencia
entre vecinos es aceptable, pero lo diremos claramente en la app.

## Fuentes

- https://github.com/permissionlesstech/bitchat-android (README, LICENSE.md, `mesh/PowerManager.kt`, `gradle/libs.versions.toml`)
- https://github.com/permissionlesstech/bitchat (LICENSE, WHITEPAPER.md)
- https://f-droid.org/packages/org.briarproject.briar.android/
- https://berty.tech/blog/bluetooth-low-energy/ y https://berty.tech/compare
- https://developer.apple.com/forums/thread/814450 y https://developer.apple.com/forums/thread/773100
- https://devzone.nordicsemi.com/f/nordic-q-a/129338/
