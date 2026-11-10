import { CuentaInterna, EstadoRnt, PrestadorAdmin, SincronizacionRnt } from '../../core/api/modelos-nucleo';
import { tablaDeTextos } from '../../core/idioma/tabla-de-textos';

/** El nombre de cada estado del prestador frente al RNT, en el idioma vigente. Exige los cuatro del contrato. */
export const ETIQUETA_ESTADO_RNT = tablaDeTextos<EstadoRnt>({
  ACTIVO: 'admin.rnt.estado.ACTIVO',
  INACTIVO: 'admin.rnt.estado.INACTIVO',
  NO_ENCONTRADO: 'admin.rnt.estado.NO_ENCONTRADO',
  VERIFICACION_PENDIENTE: 'admin.rnt.estado.VERIFICACION_PENDIENTE',
});

/** El color de la etiqueta de cada estado: una clase de las etiquetas globales (siempre con texto, no solo color). */
export const CLASE_DE_ESTADO_RNT: Readonly<Record<EstadoRnt, string>> = {
  ACTIVO: 'etiqueta--exito',
  INACTIVO: 'etiqueta--peligro',
  NO_ENCONTRADO: 'etiqueta--aviso',
  VERIFICACION_PENDIENTE: 'etiqueta--info',
};

export type FiltroPrestadores = 'todos' | 'sin-operador';

/** El filtro de la dirección (`?sinOperador=true`); cualquier otro valor, o ninguno, muestra todos. */
export function filtroDePrestadores(valor: string | null): FiltroPrestadores {
  return valor === 'true' ? 'sin-operador' : 'todos';
}

/**
 * Los operadores a los que se puede asignar un prestador: cuentas de operador de disponibilidad **activas**, por nombre. El núcleo
 * rechaza cualquier otra con `OPERADOR_NO_VALIDO` (RF-32, RF-81).
 */
export function operadoresAsignables(cuentas: readonly CuentaInterna[]): CuentaInterna[] {
  return cuentas
    .filter((cuenta) => cuenta.rol === 'OPERADOR_DISPONIBILIDAD' && cuenta.estado === 'ACTIVA')
    .sort((a, b) => a.nombre.localeCompare(b.nombre, 'es', { sensitivity: 'base' }));
}

/**
 * Cómo se llama un operador en la lista para elegirlo: su nombre. Si otro operador asignable se llama igual, se agrega el correo,
 * para que se puedan distinguir sin mostrar su identificador.
 */
export function etiquetaDeOperador(
  cuenta: CuentaInterna,
  asignables: readonly CuentaInterna[],
): string {
  const repetido = asignables.some(
    (otra) =>
      otra.id !== cuenta.id &&
      otra.nombre.trim().toLocaleLowerCase('es') === cuenta.nombre.trim().toLocaleLowerCase('es'),
  );
  return repetido ? `${cuenta.nombre} (${cuenta.correo})` : cuenta.nombre;
}

/** El operador que atiende a un prestador, con su nombre, y si su cuenta sigue activa. Null si el prestador no tiene o no se conoce. */
export function datosDelOperador(
  operadorId: string | null | undefined,
  cuentas: readonly CuentaInterna[],
): { readonly nombre: string; readonly activo: boolean } | null {
  if (!operadorId) {
    return null;
  }
  const cuenta = cuentas.find((otra) => otra.id === operadorId);
  return cuenta ? { nombre: cuenta.nombre, activo: cuenta.estado === 'ACTIVA' } : null;
}

/**
 * Si se puede enviar la asignación: se eligió un operador de los asignables y es distinto del que ya tiene el prestador. (El núcleo
 * acepta repetir el mismo y responde 200 sin cambiar nada, pero no vale la pena enviarlo.)
 */
export function puedeAsignar(
  operadorElegido: string | null | undefined,
  prestador: Pick<PrestadorAdmin, 'operadorId'>,
  asignables: readonly CuentaInterna[],
): boolean {
  return (
    !!operadorElegido &&
    operadorElegido !== prestador.operadorId &&
    asignables.some((cuenta) => cuenta.id === operadorElegido)
  );
}

/** Cuántos prestadores de la lista no tienen operador. */
export function contarSinOperador(prestadores: readonly PrestadorAdmin[]): number {
  return prestadores.filter((prestador) => !prestador.operadorId).length;
}

/** Lo que dice el resultado de una sincronización con el RNT (RF-76 a RF-78, QAS-03). */
export interface ResumenSincronizacion {
  /** Salió o volvió algún prestador del catálogo. */
  readonly huboCambios: boolean;
  /** El RNT dejó de responder: la sincronización se detuvo y cada prestador conservó su último estado. */
  readonly interrumpida: boolean;
  /** Algunos prestadores conservaron su último estado porque el RNT no respondió. */
  readonly conservaron: boolean;
}

export function resumenDeSincronizacion(resultado: SincronizacionRnt): ResumenSincronizacion {
  return {
    huboCambios: resultado.retirados + resultado.restituidos > 0,
    interrumpida: resultado.interrumpida,
    conservaron: resultado.sinRespuesta > 0,
  };
}
