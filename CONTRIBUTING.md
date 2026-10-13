# Cómo contribuir

Flujo de trabajo del repositorio: ramas, pull requests y mensajes de commit.

## Ramas (GitFlow)

| Rama | Para qué sirve | Sale de | Se une a |
|---|---|---|---|
| `main` | Versiones entregadas, cada una con su etiqueta `vX.Y.Z` | — | — |
| `develop` | Integración del trabajo en curso | — | — |
| `feature/*` | Una funcionalidad | `develop` | `develop` |
| `release/*` | Preparar una versión | `develop` | `main` y `develop` |
| `hotfix/*` | Corregir una versión ya entregada | `main` | `main` y `develop` |

- Una rama por funcionalidad, con nombre en minúsculas y guiones: `feature/catalogo-paquete-restaurante`.
- Las ramas `release/*` y `hotfix/*` las maneja el líder técnico.
- Los cambios llegan a `develop` y a `main` solo mediante pull request, y se unen con `--no-ff`.

## Ciclo de una funcionalidad

1. Crear la rama desde `develop` actualizado:

   ```bash
   git switch develop
   git pull
   git switch -c feature/<nombre>
   ```

2. Hacer los commits siguiendo la [guía de mensajes](#guía-de-mensajes-de-commit).
3. Dejar `mvn test` en verde antes de entregar: corre las pruebas unitarias y las reglas de arquitectura de ArchUnit, y no necesita Docker. El CI corre además `mvn verify` en cada push y en cada pull request.
4. Subir la rama:

   ```bash
   git push -u origin feature/<nombre>
   ```

5. Abrir el pull request hacia `develop`. La descripción dice:
   - qué se hizo;
   - los RF que cubre;
   - cómo probarlo;
   - los `TODO` que quedan pendientes.

## Commits

- Cada commit compila por sí solo, aplicado en el orden de la rama.
- Las pruebas unitarias de una clase van en el mismo commit que la clase.
- `git add` con rutas explícitas; nunca `git add .` ni `git add -A`.

## Guía de mensajes de commit

Formato de la primera línea:

```
tipo(alcance): descripción corta en minúsculas
```

### Tipos

Solo se usan estos:

| Tipo | Cuándo |
|---|---|
| `feat` | Funcionalidad nueva |
| `fix` | Corrección de error |
| `docs` | Solo documentación o comentarios |
| `style` | Formato, sin cambio de lógica |
| `refactor` | Cambio de código que no corrige ni agrega funcionalidad |
| `test` | Agregar o corregir pruebas |
| `chore` | Mantenimiento: dependencias, build, Docker, Nginx, CI |

### Alcances

El alcance es opcional. Si se usa, es uno de estos: `catalogo`, `nucleo`, `recomendacion`, `seguridad-jwt`, `contratos`, `frontend`, `infra`, `ci`.

### Reglas

- La descripción empieza con un verbo en infinitivo: "agregar", "corregir", "crear"; nunca "agrega", "agregado" ni "agregando".
- Va en minúsculas, salvo nombres propios o de clases (por ejemplo `ReglaVisibilidad`), y sin punto final.
- La primera línea completa, con tipo y alcance, no pasa de **50 caracteres**. Si hace falta explicar más, se agrega un cuerpo con un `-m` adicional:

  ```bash
  git commit -m "feat(catalogo): reintentar ante conflicto" -m "Vuelve a ejecutar el caso de uso completo, máximo 3 intentos."
  ```

- Sin línea `Co-Authored-By` y sin menciones a herramientas de IA, ni en los commits ni en la descripción del pull request.

### Ejemplos válidos

```
feat(catalogo): agregar versión de paquete
feat(catalogo): agregar filtros de accesibilidad
feat(catalogo): reintentar ante conflicto
docs: agregar ADR-05 de concurrencia
chore(infra): agregar healthcheck del catálogo
```
