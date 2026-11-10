import { CuentaCobro, CuentaCobroRequest, TipoCuentaCobro } from '../../../core/api/modelos-nucleo';
import { tablaDeTextos } from '../../../core/idioma/tabla-de-textos';

/** Los tipos de cuenta en el orden en que se ofrecen. */
export const TIPOS_DE_CUENTA: readonly TipoCuentaCobro[] = ['AHORROS', 'CORRIENTE', 'NEQUI', 'DAVIPLATA'];

/** El nombre de cada tipo de cuenta en el idioma vigente. */
export const TIPO_CUENTA_COBRO = tablaDeTextos<TipoCuentaCobro>({
  AHORROS: 'prestador.cobro.tipo.AHORROS',
  CORRIENTE: 'prestador.cobro.tipo.CORRIENTE',
  NEQUI: 'prestador.cobro.tipo.NEQUI',
  DAVIPLATA: 'prestador.cobro.tipo.DAVIPLATA',
});

/**
 * [SUPUESTO] El contrato recibe la entidad como texto libre (hasta 100 caracteres), sin una lista cerrada. Se sugieren los bancos
 * más usados del país y el prestador puede escribir otro. Son nombres propios: no se traducen.
 */
export const BANCOS_SUGERIDOS: readonly string[] = [
  'Bancolombia',
  'Banco de Bogotá',
  'Davivienda',
  'BBVA',
  'Banco de Occidente',
  'Banco Popular',
  'Banco Caja Social',
  'Banco AV Villas',
  'Scotiabank Colpatria',
  'Itaú',
  'Banco Agrario',
  'Bancoomeva',
];

const MAXIMO_TEXTO = 100;
const NUMERO_BANCO = /^\d{6,20}$/;
const CELULAR = /^3\d{9}$/;

/** Nequi y Daviplata son billeteras: usan como número el celular y no piden entidad. */
export function esBilletera(tipo: TipoCuentaCobro | ''): boolean {
  return tipo === 'NEQUI' || tipo === 'DAVIPLATA';
}

/** Solo las cuentas bancarias piden la entidad (el banco). */
export function pideEntidad(tipo: TipoCuentaCobro | ''): boolean {
  return tipo === 'AHORROS' || tipo === 'CORRIENTE';
}

// --- Los valores del formulario ---

export interface ValoresCuenta {
  readonly tipo: TipoCuentaCobro | '';
  readonly entidad: string;
  /** El número completo. Solo se escribe: nunca se muestra, ni se guarda, ni sale en un mensaje. */
  readonly numero: string;
  readonly titular: string;
}

export const VALORES_CUENTA_VACIOS: ValoresCuenta = { tipo: '', entidad: '', numero: '', titular: '' };

/** Quita los espacios y los guiones con que la gente separa un número: "4011 2233-4401" → "401122334401". */
export function normalizarNumero(texto: string): string {
  return texto.replace(/[\s-]/g, '');
}

// --- La validación ---

export type CampoCuenta = 'tipo' | 'entidad' | 'numero' | 'titular';
export type MotivoCuenta =
  | 'tipoRequerido'
  | 'entidadRequerida'
  | 'entidadLarga'
  | 'numeroRequerido'
  | 'numeroBancoInvalido'
  | 'numeroCelularInvalido'
  | 'titularRequerido'
  | 'titularLargo';

/** Los campos en el orden de la pantalla: el primero con error recibe el foco. */
export const CAMPOS_CUENTA_EN_ORDEN: readonly CampoCuenta[] = ['tipo', 'entidad', 'numero', 'titular'];

/**
 * Los errores de lo escrito. Ningún motivo lleva el valor: así ningún mensaje puede mostrar el número de la cuenta.
 */
export function validarCuenta(valores: ValoresCuenta): Partial<Record<CampoCuenta, MotivoCuenta>> {
  const errores: Partial<Record<CampoCuenta, MotivoCuenta>> = {};
  if (!valores.tipo) {
    errores.tipo = 'tipoRequerido';
  }
  if (pideEntidad(valores.tipo)) {
    const entidad = valores.entidad.trim();
    if (!entidad) {
      errores.entidad = 'entidadRequerida';
    } else if (entidad.length > MAXIMO_TEXTO) {
      errores.entidad = 'entidadLarga';
    }
  }
  const numero = normalizarNumero(valores.numero);
  if (!numero) {
    errores.numero = 'numeroRequerido';
  } else if (valores.tipo && !(esBilletera(valores.tipo) ? CELULAR : NUMERO_BANCO).test(numero)) {
    errores.numero = esBilletera(valores.tipo) ? 'numeroCelularInvalido' : 'numeroBancoInvalido';
  }
  const titular = valores.titular.trim();
  if (!titular) {
    errores.titular = 'titularRequerido';
  } else if (titular.length > MAXIMO_TEXTO) {
    errores.titular = 'titularLargo';
  }
  return errores;
}

// --- La petición y lo que se muestra ---

/** El cuerpo de `PUT /prestador/cuenta-cobro`. Una billetera no envía entidad. */
export function construirCuenta(valores: ValoresCuenta): CuentaCobroRequest {
  const tipo = valores.tipo as TipoCuentaCobro;
  return {
    tipo,
    ...(pideEntidad(tipo) && { entidad: valores.entidad.trim() }),
    numero: normalizarNumero(valores.numero),
    titular: valores.titular.trim(),
  };
}

/**
 * Lo que el formulario muestra al cambiar la cuenta vigente: todo menos el número, que el núcleo no devuelve completo y que hay que
 * escribir otra vez.
 */
export function valoresDeCuenta(cuenta: CuentaCobro): ValoresCuenta {
  return {
    tipo: cuenta.tipo,
    entidad: pideEntidad(cuenta.tipo) ? cuenta.entidad : '',
    numero: '',
    titular: cuenta.titular,
  };
}

/** Los últimos dígitos del número enmascarado ("******0101" → "0101"), para decirlos en voz alta a un lector de pantalla. */
export function ultimosDigitos(enmascarado: string): string {
  return /\d+$/.exec(enmascarado)?.[0] ?? '';
}
