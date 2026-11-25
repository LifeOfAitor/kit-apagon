# Kit Apagón: normas del proyecto

Aplicación móvil que funciona sin conexión para la próxima caída de la red eléctrica:
información local en caché, mensajería en malla por Bluetooth y guía sin conexión.
Es una herramienta de emergencia: la fiabilidad está por encima de la velocidad de desarrollo.

## Principios innegociables

1. **La app no puede fallar.** Todo el código de lógica tiene tests unitarios. Nada se integra sin tests en verde.
2. **Investigar antes de decidir.** Toda decisión técnica relevante se documenta en `docs/decisiones/`
   (un archivo por decisión: contexto, alternativas consideradas, decisión y consecuencias).
3. **Coste cero.** Solo herramientas, servicios y datos gratuitos. Cualquier gasto lo decide el propietario.
4. **Nada ilegal.** Antes de reutilizar código, datos o textos se comprueba su licencia y se anota en
   `docs/licencias-de-terceros.md`. Sin datos personales fuera del dispositivo.
5. **Poco a poco.** Incrementos pequeños, cada uno verificado antes de pasar al siguiente.
6. **Sin conexión primero.** Todo lo esencial funciona sin red una vez instalada la app y descargado
   el paquete local. La red solo se usa para descargar o actualizar ese paquete.
7. **Móviles modestos.** El dispositivo de referencia es un Android de gama baja con 2 GB de RAM y la
   batería al 50 %. Sin animaciones costosas y con un modo de ahorro real. Cualquier función que use
   radio (Bluetooth, GPS, Wi-Fi) justifica su consumo, y el consumo se mide, no se supone.
8. **Privacidad total.** Sin cuentas, sin rastreo y sin analítica ni informes de errores de terceros.
   Los datos personales (plan familiar, ficha médica, etc.) se quedan solo en el dispositivo.
   Lo esencial no depende de los servicios de Google Play.
9. **Información fiable y con fuente.** Cada contenido de seguridad (guías, listas de comprobación)
   cita su fuente oficial en `docs/FUENTES.md`, con fecha de revisión. La app nunca sustituye al 112
   ni a los avisos oficiales, y lo dice de forma visible.

## Código limpio

- Nombres descriptivos y completos. Sin abreviaturas crípticas (`messageRepository`, no `msgRepo`).
- Funciones pequeñas con una sola responsabilidad.
- Sin código muerto, sin código comentado, sin TODO sin issue asociada.
- Los comentarios explican el porqué, nunca el qué.
- Identificadores y comentarios del código en inglés; documentación y textos de la interfaz en español.
- Sin emojis en el código, en los commits ni en la documentación.

## Stack

- Kotlin nativo, Android primero. iOS en una fase posterior.
- Los detalles (versión mínima de Android, librerías) se fijan en `docs/decisiones/`.

## Flujo de trabajo con Git y GitHub

- La rama `main` siempre compila y pasa los tests. Nunca se trabaja directamente sobre ella.
  Excepción temporal: mientras el repositorio solo exista en local y no haya código (fase de
  investigación), los commits de documentación van directamente a `main`.
- Cada cambio va en una rama propia (`feature/...`, `fix/...`, `docs/...`) y entra en `main` mediante pull request.
- Los commits siguen Conventional Commits en español: `feat: añadir mapa sin conexión`,
  `fix: ...`, `test: ...`, `docs: ...`, `refactor: ...`, `chore: ...`.
- Un commit hace una sola cosa. Mensajes en imperativo y sin emojis.
- La integración continua (GitHub Actions) compila, pasa el linter y ejecuta los tests en cada pull request.
