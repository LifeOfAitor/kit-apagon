# Licencias de terceros

Todo código, dato o recurso ajeno que use el proyecto aparece aquí antes de incorporarse.

## Código distribuido con la app

Estas librerías forman parte del APK que recibe el usuario.

| Componente | Versión | Licencia | Uso |
|---|---|---|---|
| Jetpack Compose (ui, material3, tooling-preview), mediante el BOM | 2026.11.01 | Apache-2.0 | Interfaz |
| AndroidX Activity Compose | 1.13.0 | Apache-2.0 | Punto de entrada de la interfaz |
| AndroidX Core KTX | 1.19.1 | Apache-2.0 | Utilidades de Android |
| AndroidX Lifecycle Runtime Compose | 2.11.1 | Apache-2.0 | Ciclo de vida en Compose |
| Biblioteca estándar de Kotlin | 2.4.21 | Apache-2.0 | Lenguaje |

## Herramientas de compilación y pruebas

No se distribuyen con la app, así que su licencia no condiciona la del proyecto.

| Componente | Versión | Licencia | Uso |
|---|---|---|---|
| Gradle | 9.9.2 | Apache-2.0 | Sistema de compilación |
| Android Gradle Plugin | 9.5.3 | Apache-2.0 | Compilación Android |
| Kotlin Gradle Plugin y compilador de Compose | 2.4.21 | Apache-2.0 | Compilación Kotlin |
| JUnit 4 | 4.13.2 | EPL-1.0 | Tests |
| Robolectric | 4.18.1 | MIT | Tests de Android sin emulador |
| AndroidX Test (core, ext-junit) y Compose UI Test | 1.7.0, 1.3.0 y BOM | Apache-2.0 | Tests |
| ktlint | 1.8.0 | MIT | Formato del código |
| detekt | 1.23.8 | Apache-2.0 | Reglas de código limpio |

## Datos

Ninguno incorporado todavía. Previsto: datos de OpenStreetMap bajo ODbL 1.0 (ver decisión 0004).

## Compatibilidad con la licencia del proyecto

El proyecto es GPL-3.0-or-later (decisión 0001). Son compatibles el código GPL-3, LGPL, Apache-2.0,
MIT, BSD y de dominio público. Antes de incorporar código GPL-2.0 hay que comprobar si es "or later":
el GPL-2.0 sin esa cláusula no se puede combinar con GPL-3.
