# ADR-05 · Control de concurrencia en escrituras del catálogo

| Campo | Contenido |
|---|---|
| Estado | Aceptada. Implementación pendiente: se hace junto con el adaptador de MongoDB |
| Decidida en | Fase de Construcción, durante el servicio de catálogo (octubre de 2026) |
| Responsable | Líder técnico / Arquitecto |

## Contexto

El catálogo corre en varias réplicas sin estado detrás de Nginx (ADR-02) y guarda cada oferta como un documento en MongoDB (ADR-03). Dos de sus casos de uso escriben:

- `GestionarOferta`: el prestador publica, edita o retira una oferta.
- `AplicarAvisoNucleo`: el núcleo avisa una decisión de moderación o un cambio en la habilitación RNT.

Los dos hacen lo mismo: leen el documento completo, aplican la regla del dominio y guardan el documento completo. Si dos escrituras sobre la misma oferta se cruzan, la segunda en guardar pisa a la primera sin que nadie lo note. Pasa aunque las dos lleguen a la misma réplica.

Ejemplo: el prestador edita una oferta al mismo tiempo que llega la aprobación de su versión 3. La edición lee la oferta con la versión 3 pendiente; la aprobación la lee igual. La aprobación guarda la versión 3 como publicada; la edición guarda después la versión 4 como pendiente sobre lo que había leído, y la aprobación se pierde.

Hay un segundo problema, que no se resuelve protegiendo cada oferta por separado. La habilitación RNT del prestador vive en la réplica `PrestadorCatalogo` y se copia en cada una de sus ofertas (DC-02, DC-03):

- Dos avisos de habilitación (versión 2 y versión 3) pueden guardar la réplica en orden inverso y dejar guardada la versión 2.
- Aunque la réplica quede bien, los dos avisos pueden ejecutar su actualización masiva de ofertas en orden inverso. El aviso de la versión 2, atrasado, dejaría las ofertas con el RNT viejo después de que el de la versión 3 ya las había actualizado.

## Decisión

Bloqueo optimista con un número de revisión técnico en cada oferta, reintento del caso de uso completo en la capa de aplicación y escrituras de habilitación que solo avanzan.

### Revisión de la oferta

- `Oferta` lleva una `revision`. Es un valor opaco: ninguna regla de negocio la interpreta. No es `VersionOferta.numero`, que es de negocio y solo cambia cuando el prestador edita (DC-05).
- El puerto de salida guarda la oferta solo si la revisión guardada coincide con la que se leyó. Si no coincide, lanza `ConflictoConcurrenciaException`.
- `ConflictoConcurrenciaException` es Java puro y vive en `catalogo.compartido.concurrencia`, para que un solo manejador la atienda en todos los módulos. La regla 2 de ArchUnit se amplía para que `aplicacion` pueda usar ese paquete.

### Reintento

- `EjecutorConReintento`, en Java puro y sin Spring Retry, lo usan los servicios de aplicación que escriben.
- Ante el conflicto vuelve a ejecutar el caso de uso entero: recargar, aplicar la regla y guardar. Máximo 3 intentos, con una espera aleatoria corta entre ellos.
- Si se agotan, la excepción sube y la API responde 409 `application/problem+json` con el código `CONFLICTO_CONCURRENCIA`.

### Avisos del núcleo

- El aviso se marca como procesado solo después de guardar con éxito. Si se agotan los reintentos se responde 409 y el núcleo lo reenvía más tarde.
- Un aviso de moderación sobre una oferta retirada se ignora y se marca como procesado.
- Los avisos procesados se guardan en la base compartida, así que la deduplicación cruza réplicas. Si dos réplicas reciben el mismo aviso a la vez, el caso queda cubierto por tres cosas juntas:
  1. el dominio es idempotente: `numeroVersion` en la moderación y `version` en la habilitación;
  2. el bloqueo optimista hace fallar la segunda escritura, y al reintentar el aviso se ignora;
  3. el adaptador de MongoDB tiene un índice único sobre `idAviso`, y `marcarProcesado` tolera el duplicado.

### Habilitación RNT: escrituras que solo avanzan

1. **`versionHabilitacion` en cada oferta.** Es la versión del aviso de habilitación del prestador, la misma `version` de `PrestadorCatalogo`, copiada en la oferta junto a `rntPrestadorActivo`. Los dos valores siempre se copian juntos.
2. **Guardado condicionado de `PrestadorCatalogo`.** La réplica solo se guarda si la versión guardada es menor que la nueva; si no, se lanza el conflicto. Al reintentar, `aplicarHabilitacion` devuelve `false` y el aviso queda `IGNORADO`.
3. **Actualización masiva con filtro.** `actualizarRntPrestador(prestadorId, rntActivo, version)` solo actualiza las ofertas del prestador cuya `versionHabilitacion` es menor que `version`. En MongoDB es un `updateMany` con el filtro `{prestadorId, versionHabilitacion < version}`, `$set` de `rntPrestadorActivo` y `versionHabilitacion`, e `$inc` de `revision`. Cada oferta solo avanza: un `updateMany` atrasado no encuentra ofertas que cumplan el filtro y no cambia nada.
4. **Editar y aprobar** copian `rntActivo` y `version` de `PrestadorCatalogo`, no solo el RNT.
5. **Una oferta nueva** nace con la `versionHabilitacion` de la réplica en ese momento: 0 si el prestador aún no tiene avisos.

### Notas para el adaptador de MongoDB

- En el documento, el campo `@Version` es `Long` y la revisión inicial se mapea a `null`. El mapper conserva la revisión en ambos sentidos.
- El `$inc` de `revision` en la actualización masiva hace que una edición o una aprobación que se cruce con ella falle y se reintente con el RNT ya actualizado.
- El adaptador en memoria del prototipo debe simular lo mismo: comparar la revisión al guardar e incrementarla en la actualización masiva.

## Alternativas descartadas

| Alternativa | Por qué se descarta |
|---|---|
| Bloqueo en memoria (`synchronized`, un candado por oferta) | No funciona entre réplicas |
| Enrutar cada oferta siempre a la misma réplica | Rompe las réplicas sin estado del ADR-02 |
| Bloqueo distribuido | Infraestructura adicional para una contención baja |
| Actualizaciones por campo con filtros de negocio | Saca las reglas del dominio y las reparte en las consultas |
| Responder 409 sin reintentar en el servidor | Traslada al cliente y al núcleo un conflicto que casi siempre se resuelve solo al segundo intento |
| ETag en la edición | Protege la edición del prestador, pero no el cruce con los avisos del núcleo |
| Transacciones multidocumento de MongoDB | Exigen replica set y el Compose levanta un solo `mongod`; por sí solas tampoco evitan manejar el conflicto |

El filtro por `versionHabilitacion` de la actualización masiva no es un filtro de negocio: ordena avisos, igual que la `version` de DC-02, y no decide ninguna regla.

## Consecuencias

Positivas:

- Ninguna escritura se pierde en silencio: o se aplica, o se reintenta, o se responde 409.
- Las réplicas siguen sin estado y no hace falta infraestructura nueva.
- Las reglas siguen en el dominio; el reintento vuelve a evaluarlas con el estado real.
- La copia del RNT en las ofertas converge aunque los avisos lleguen en desorden o se procesen a la vez.

Negativas:

- `Oferta` lleva dos datos técnicos más, `revision` y `versionHabilitacion`, que se mapean en ambos sentidos.
- Los servicios que escriben deben poder ejecutarse varias veces sin efectos fuera de la base: no pueden notificar ni llamar a otro servicio antes de guardar.
- Con contención alta sobre una misma oferta, un cliente puede recibir 409. Se espera que sea raro: una oferta la editan su prestador y los avisos del núcleo.
- La regla 2 de ArchUnit deja de ser "solo dominio" y admite `compartido.concurrencia`.

## Verificación

Escenario ligado a QAS-01 y QAS-02; no se crea un QAS nuevo.

- **Edición y aprobación a la vez.** Sobre una oferta con la versión 3 en moderación, llegan al mismo tiempo la aprobación de la versión 3 y una edición que crea la versión 4. Estado final válido: versión 3 publicada y versión 4 pendiente, o versión 4 pendiente con el aviso ignorado. En los dos casos, cero cambios perdidos.
- **Avisos de habilitación en desorden.** Con los avisos de las versiones 2 y 3 procesados a la vez o en orden inverso, la réplica y todas las ofertas del prestador terminan con el RNT y la `versionHabilitacion` de la versión 3.
- **Mismo aviso en dos réplicas.** Se aplica una sola vez.

Se prueba con pruebas unitarias que usan un doble en memoria que lanza el conflicto, y con una prueba de integración contra MongoDB cuando exista el adaptador.

## Trazabilidad

| Fuente | Relación |
|---|---|
| ADR-02 | Réplicas sin estado: el control no puede depender de la memoria de una réplica |
| ADR-03 | Un documento por oferta en MongoDB, sin transacciones entre documentos |
| ADR-04 | Avisos idempotentes y reintentos del núcleo |
| QAS-01, QAS-02 | Disponibilidad y escalado horizontal con varias réplicas |
| RF-66, RF-79 | Edición con versión pendiente y moderación |
| RF-77 | Retiro y restitución de la habilitación RNT |
| DC-02, DC-03 | Réplica `PrestadorCatalogo` y copia del RNT en cada oferta |
| DC-04, DC-05, DC-08 | Avisos del núcleo, número de versión y moderación idempotente |

## Alcance

- Este ADR cubre el servicio de catálogo.
- **No está implementado.** La `revision`, `versionHabilitacion`, `ConflictoConcurrenciaException`, `EjecutorConReintento` y el parámetro `version` del puerto se construyen junto con el adaptador de MongoDB. Hasta entonces, el prototipo en memoria puede perder una escritura si dos se cruzan sobre la misma oferta.
- El núcleo tendrá su propia decisión cuando se diseñe el módulo de solicitudes: bloqueo optimista de JPA con `@Version` y toma de la cola con `FOR UPDATE SKIP LOCKED`.
