import { CuentaInterna, CuentaInternaRequest, Rol } from '../../core/api/modelos-nucleo';
import { tablaDeTextos } from '../../core/idioma/tabla-de-textos';

/** Los tres roles de las cuentas internas: administrador, operador de disponibilidad y gestor del destino. */
export type RolInterno = CuentaInternaRequest['rol'];

/**
 * Los roles que se pueden crear desde el panel: operador de disponibilidad y gestor del destino. El contrato permite crear también
 * administradores, pero es un poder delicado: las cuentas de administrador solo se consultan aquí.
 */
export const ROLES_CREABLES: readonly RolInterno[] = ['OPERADOR_DISPONIBILIDAD', 'GESTOR_DESTINO'];

export const NOMBRE_MAXIMO = 100;
export const CORREO_MAXIMO = 254;

/** El nombre de cada estado de una cuenta interna, en el idioma vigente. */
export const ETIQUETA_ESTADO_CUENTA = tablaDeTextos<CuentaInterna['estado']>({
  ACTIVA: 'admin.cuentas.estado.ACTIVA',
  DESACTIVADA: 'admin.cuentas.estado.DESACTIVADA',
});

/** El color de la etiqueta de cada estado: una clase de las etiquetas globales (siempre con texto, no solo color). */
export const CLASE_DE_ESTADO_CUENTA: Readonly<Record<CuentaInterna['estado'], string>> = {
  ACTIVA: 'etiqueta--exito',
  DESACTIVADA: 'etiqueta--neutra',
};

// --- Crear una cuenta ---

export interface ValoresCuenta {
  readonly rol: string;
  readonly nombre: string;
  readonly correo: string;
}

export type CampoCuenta = keyof ValoresCuenta;
export type MotivoCuenta =
  | 'rolRequerido'
  | 'nombreRequerido'
  | 'nombreLargo'
  | 'correoRequerido'
  | 'correoFormato'
  | 'correoLargo';

const CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/**
 * Valida como lo hace el núcleo para avisar antes de enviar: el rol debe ser uno de los que se pueden crear, el nombre es obligatorio
 * (hasta 100 caracteres) y el correo también (hasta 254, con forma de correo). Devuelve el primer motivo de cada campo con error;
 * un objeto vacío significa que se puede enviar. El núcleo vuelve a validar y avisa si el correo ya está registrado.
 */
export function validarCuenta(valores: ValoresCuenta): Partial<Record<CampoCuenta, MotivoCuenta>> {
  const errores: Partial<Record<CampoCuenta, MotivoCuenta>> = {};

  if (!ROLES_CREABLES.some((rol) => rol === valores.rol)) {
    errores.rol = 'rolRequerido';
  }

  const nombre = valores.nombre.trim();
  if (nombre === '') {
    errores.nombre = 'nombreRequerido';
  } else if (nombre.length > NOMBRE_MAXIMO) {
    errores.nombre = 'nombreLargo';
  }

  const correo = valores.correo.trim();
  if (correo === '') {
    errores.correo = 'correoRequerido';
  } else if (correo.length > CORREO_MAXIMO) {
    errores.correo = 'correoLargo';
  } else if (!CORREO.test(correo)) {
    errores.correo = 'correoFormato';
  }
  return errores;
}

/** El cuerpo de `POST /admin/cuentas-internas`, con el nombre y el correo sin espacios en los extremos. */
export function construirCuenta(valores: ValoresCuenta): CuentaInternaRequest {
  return {
    rol: valores.rol as RolInterno,
    nombre: valores.nombre.trim(),
    correo: valores.correo.trim(),
  };
}

// --- Gestionar las cuentas de la lista ---

/** Qué se puede hacer con una cuenta: las de administrador solo se consultan. */
export function accionDeCuenta(cuenta: Pick<CuentaInterna, 'rol' | 'estado'>): 'desactivar' | 'reactivar' | null {
  if (cuenta.rol === 'ADMINISTRADOR') {
    return null;
  }
  return cuenta.estado === 'ACTIVA' ? 'desactivar' : 'reactivar';
}

export type FiltroCuentas = 'todas' | RolInterno;

export const FILTROS_CUENTAS: readonly FiltroCuentas[] = [
  'todas',
  'OPERADOR_DISPONIBILIDAD',
  'GESTOR_DESTINO',
  'ADMINISTRADOR',
];

/** El filtro de la dirección (`?rol=GESTOR_DESTINO`); un valor desconocido, o ninguno, muestra todas las cuentas. */
export function filtroDeCuentas(valor: string | null): FiltroCuentas {
  return FILTROS_CUENTAS.find((filtro) => filtro === valor && filtro !== 'todas') ?? 'todas';
}

/** Las cuentas del filtro, en el mismo orden (de la más antigua a la más reciente). */
export function filtrarCuentas(cuentas: readonly CuentaInterna[], filtro: FiltroCuentas): CuentaInterna[] {
  return filtro === 'todas' ? [...cuentas] : cuentas.filter((cuenta) => cuenta.rol === filtro);
}

