# Glosario español → inglés

Cada término se traduce igual en toda la aplicación. Antes de escribir un texto en inglés, busca aquí el término; si no está, agrégalo en la misma rama en que lo uses.

La prueba `textos.spec.ts` revisa los términos de la sección "Términos del dominio": si un texto en español usa el término de la izquierda, el inglés debe usar el de la derecha. Si agregas un término aquí, agrégalo también a la lista `GLOSARIO` de esa prueba.

## Términos del dominio

| Español | Inglés |
|---|---|
| turista | tourist |
| prestador / prestador de servicios turísticos | provider / tourism service provider |
| oferta | offer |
| actividad | activity |
| paquete | package |
| alojamiento | accommodation |
| restaurante | restaurant |
| agencia operadora | tour operator agency |
| guía independiente | independent guide |
| solicitud de disponibilidad | availability request |
| operador de disponibilidad | availability operator |
| gestor del destino | destination manager |
| administrador | administrator |
| zona | area |
| área protegida | protected area |
| alta afluencia | high visitor traffic |
| opinión | review |
| RNT (Registro Nacional de Turismo) | RNT (National Tourism Registry) — la sigla no se traduce |
| política de tratamiento de datos | data processing policy |

**Cuándo usar cada forma.** "Provider" es la palabra corta para los textos de la interfaz. "Tourism service provider" se usa donde conviene la forma completa, por ejemplo al explicar qué es un prestador. La primera vez que aparece "RNT" en un texto se explica: "RNT (National Tourism Registry)"; después basta la sigla.

## Términos de la interfaz

No estaban en el glosario original. Siguen la redacción de `_referencia-html/js/i18n.js`, el diseño de referencia.

| Español | Inglés |
|---|---|
| iniciar sesión | log in |
| cerrar sesión | log out |
| registrarse | sign up |
| crear cuenta | create account |
| mis datos | my data |
| contraseña | password |
| catálogo | catalog |
| ficha de la oferta | offer page |

## Criterios de estilo

- **Natural, no palabra por palabra.** El texto en inglés debe sonar como lo escribiría alguien que habla inglés, con la misma intención: "Todavía no tienes opiniones" es "You don't have any reviews yet", no "Yet you do not have opinions".
- **Inglés estadounidense** (catalog, color, center), como el diseño de referencia.
- **Tuteo ↔ "you".** El español se dirige al usuario de tú; el inglés, con "you". Nada de "usted" ni de "the user".
- **Mayúsculas.** Los títulos y botones del inglés llevan solo la primera palabra en mayúscula ("Create account"), igual que en español.
- **Moneda.** En español: `$ 150.000`. En inglés: `COP 150,000`, para que un turista extranjero no lo lea como dólares.
- **Lo que viene del backend** (nombres y descripciones de ofertas, zonas, perfiles, el texto de la política) se muestra tal cual llega y no se traduce aquí.

## Cómo se agrega un texto

1. Elige la clave `area.pantalla.texto` y escribe el texto en español en el diccionario del área (`*.i18n.ts`).
2. Escribe el inglés en el mismo diccionario, con los términos de este glosario.
3. Úsalo en la plantilla con el pipe `t` (`{{ 'cuentas.login.titulo' | t }}`) o, en código, con `traducir('cuentas.login.titulo')`.

El compilador avisa si falta el inglés o si la clave no existe, y `textos.spec.ts` si los parámetros no coinciden, si quedó texto en español en el inglés o si se aleja del glosario.
