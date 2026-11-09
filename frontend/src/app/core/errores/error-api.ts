import { CodigoError } from '../api/modelos';
import { ERRORES } from '../idioma/errores.i18n';
import { tablaDeTextos } from '../idioma/tabla-de-textos';
import type { ClaveTexto } from '../idioma/textos';
import { traducir } from '../idioma/traducir';

/** Propiedades que el núcleo suma al problem+json en tres códigos (nucleo.openapi.yaml, esquema Problema). */
export interface ExtrasError {
  /** Con CUENTA_BLOQUEADA: hasta cuándo no se puede iniciar sesión (RF-05). Fecha ISO. */
  readonly bloqueadaHasta?: string;
  /** Con CODIGO_INCORRECTO: cuántos intentos quedan antes de que el código se invalide. */
  readonly intentosRestantes?: number;
  /** Con PRESTADOR_CON_COMPROMISOS: lo que el prestador debe cumplir antes de eliminar sus datos. */
  readonly compromisos?: readonly string[];
}

/**
 * Un error de la API ya traducido para el usuario. El interceptor de errores convierte cada respuesta
 * application/problem+json en uno de estos; el código nunca se muestra, solo decide el mensaje.
 *
 * Guarda la CLAVE del mensaje, no el texto: `mensaje` se arma en el idioma vigente cada vez que se lee. Así un error que
 * ya está en pantalla cambia de idioma junto con la interfaz, si la pantalla guarda el `ErrorApi` y lee `mensaje` en su
 * plantilla.
 */
export class ErrorApi extends Error {
  constructor(
    /** Estado HTTP; 0 si no hubo respuesta. */
    readonly estado: number,
    readonly codigo: CodigoError | undefined,
    /** Clave del texto para mostrar al usuario (ver `core/idioma/errores.i18n.ts`). */
    readonly clave: ClaveTexto,
    /** Campo afectado, cuando la API lo dice (DATO_INVALIDO). */
    readonly campo?: string,
    /** Lo que la API dice del campo, por ejemplo "debe ser mayor que cero". */
    readonly detalleCampo?: string,
    /** Datos que el núcleo agrega a algunos errores: hasta cuándo dura un bloqueo, intentos restantes, compromisos. */
    readonly extras?: ExtrasError,
  ) {
    super(traducir(clave));
    this.name = 'ErrorApi';
  }

  /** El texto para el usuario, en el idioma vigente. */
  get mensaje(): string {
    return traducir(this.clave);
  }

  get noEncontrado(): boolean {
    return this.estado === 404;
  }
}

const PREFIJO = 'errores.codigo.';

/** La clave del mensaje de un código de la API; un código que el frontend no conoce da el mensaje genérico. */
export function claveDeError(codigo: string | undefined): ClaveTexto {
  const clave = `${PREFIJO}${codigo}`;
  return codigo !== undefined && clave in ERRORES.es ? (clave as ClaveTexto) : 'errores.generico';
}

/**
 * El mensaje de cada código de la API (DC-15), en el idioma vigente: `MENSAJES.NO_AUTENTICADO`. Los textos están en
 * `core/idioma/errores.i18n.ts`, que exige un mensaje (en español y en inglés) para cada código del contrato.
 */
export const MENSAJES = tablaDeTextos<CodigoError>(
  Object.fromEntries(
    Object.keys(ERRORES.es)
      .filter((clave) => clave.startsWith(PREFIJO))
      .map((clave) => [clave.slice(PREFIJO.length), clave]),
  ) as Record<CodigoError, ClaveTexto>,
);
