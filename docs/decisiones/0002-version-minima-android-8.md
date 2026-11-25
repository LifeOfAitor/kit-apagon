# 0002. Versión mínima: Android 8.0 (API 26)

Fecha: 2026-11-25. Estado: Propuesta.

## Contexto

El principio 7 exige funcionar en móviles modestos. Cuanto más antigua es la versión mínima, más
móviles cubrimos, pero más casos especiales hay en el código de Bluetooth.

## Alternativas consideradas

1. **API 21 (Android 5).** Unos pocos puntos más de cobertura. El soporte de Bluetooth LE en modo
   periférico es irregular en esas versiones y las librerías modernas de Jetpack ya no las admiten.
2. **API 26 (Android 8).** Cerca del 96 % de los dispositivos. Es el mínimo de BitChat para Android,
   así que su experiencia práctica nos sirve.
3. **API 31 (Android 12).** Permisos de Bluetooth más sencillos, pero deja fuera muchos móviles de
   gama baja, justo los del principio 7.

## Decisión

API 26 (Android 8.0) como mínimo. La versión objetivo (`targetSdk`) será siempre la más reciente
que exija Google en cada momento.

## Consecuencias

- En Android 8 a 11 habrá que pedir el permiso de ubicación para escanear Bluetooth y explicar
  al usuario que no se usa para localizarle.
- Los tests y las pruebas reales deben incluir al menos un móvil con Android 8 a 11 y otro con 12 o superior.
- Hay que confirmar el porcentaje con el gráfico de distribución de Android Studio al crear el proyecto.
