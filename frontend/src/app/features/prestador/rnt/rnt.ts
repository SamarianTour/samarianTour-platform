import { ModalidadRecorrido, TipoPrestador } from '../../../core/api/modelos';
import {
  CategoriaPrestador,
  DatosTipoRequest,
  EstadoRnt,
  PrestadorPropio,
  RegistroPrestadorRequest,
} from '../../../core/api/modelos-nucleo';
import { tablaDeTextos } from '../../../core/idioma/tabla-de-textos';
import type { ClaveTexto } from '../../../core/idioma/textos';
import { aLista } from '../formulario-oferta/peticion-oferta';

/** Los tipos de prestador en el orden en que se ofrecen. */
export const TIPOS_DE_PRESTADOR: readonly TipoPrestador[] = [
  'AGENCIA_OPERADORA',
  'GUIA_INDEPENDIENTE',
  'ALOJAMIENTO',
  'RESTAURANTE',
];

/** Las modalidades que declara el guía independiente (RF-69). */
export const MODALIDADES_DEL_GUIA: readonly ModalidadRecorrido[] = ['CAMINATA', 'BICICLETA', 'KAYAK', 'OTRA'];

export const MAXIMO_EMPLEADOS = 100000;

const NUMERO_RNT = /^[0-9]{1,10}$/;
const IDIOMA = /^[a-z]{2}$/;

// --- Los valores del formulario ---

/** Lo que cambia con el tipo: el guía declara modalidades e idiomas y no empleados; los demás, empleados (RF-69). */
export interface ValoresTipo {
  readonly tipo: TipoPrestador | '';
  readonly numeroEmpleados: number | null;
  readonly modalidades: readonly ModalidadRecorrido[];
  /** Códigos de dos letras separados por comas. */
  readonly idiomas: string;
}

export interface ValoresRegistro extends ValoresTipo {
  readonly numeroRnt: string;
}

export const VALORES_TIPO_VACIOS: ValoresTipo = {
  tipo: '',
  numeroEmpleados: null,
  modalidades: [],
  idiomas: '',
};

export function esGuia(tipo: TipoPrestador | ''): boolean {
  return tipo === 'GUIA_INDEPENDIENTE';
}

// --- La validación ---

export type CampoRnt = 'numeroRnt' | 'tipo' | 'numeroEmpleados' | 'modalidades' | 'idiomas';
export type MotivoRnt =
  | 'numeroRequerido'
  | 'numeroInvalido'
  | 'tipoRequerido'
  | 'empleadosRequerido'
  | 'empleadosRango'
  | 'modalidadesRequerida'
  | 'idiomasRequerido'
  | 'idiomasInvalidos';

/** Los campos en el orden de la pantalla: el primero con error recibe el foco. */
export const CAMPOS_EN_ORDEN: readonly CampoRnt[] = ['numeroRnt', 'tipo', 'numeroEmpleados', 'modalidades', 'idiomas'];

/** Los idiomas escritos: en minúscula, sin repetir y sin vacíos. */
export function idiomasDe(texto: string): string[] {
  return [...new Set(aLista(texto).map((idioma) => idioma.toLowerCase()))];
}

/** El número de RNT: solo dígitos, de 1 a 10 (RegistroPrestadorRequest). */
export function validarNumeroRnt(numero: string): MotivoRnt | null {
  const limpio = numero.trim();
  if (!limpio) {
    return 'numeroRequerido';
  }
  return NUMERO_RNT.test(limpio) ? null : 'numeroInvalido';
}

/** Los campos que dependen del tipo. Los del otro tipo no se piden ni se envían. */
export function validarTipo(valores: ValoresTipo): Partial<Record<CampoRnt, MotivoRnt>> {
  const errores: Partial<Record<CampoRnt, MotivoRnt>> = {};
  if (!valores.tipo) {
    errores.tipo = 'tipoRequerido';
    return errores;
  }
  if (esGuia(valores.tipo)) {
    if (valores.modalidades.length === 0) {
      errores.modalidades = 'modalidadesRequerida';
    }
    const idiomas = idiomasDe(valores.idiomas);
    if (idiomas.length === 0) {
      errores.idiomas = 'idiomasRequerido';
    } else if (!idiomas.every((idioma) => IDIOMA.test(idioma))) {
      errores.idiomas = 'idiomasInvalidos';
    }
    return errores;
  }
  const empleados = valores.numeroEmpleados;
  if (empleados === null || Number.isNaN(empleados)) {
    errores.numeroEmpleados = 'empleadosRequerido';
  } else if (!Number.isInteger(empleados) || empleados < 1 || empleados > MAXIMO_EMPLEADOS) {
    errores.numeroEmpleados = 'empleadosRango';
  }
  return errores;
}

export function validarRegistro(valores: ValoresRegistro): Partial<Record<CampoRnt, MotivoRnt>> {
  const errores = validarTipo(valores);
  const numero = validarNumeroRnt(valores.numeroRnt);
  return numero ? { numeroRnt: numero, ...errores } : errores;
}

// --- Las peticiones ---

/** El cuerpo de `PUT /prestador/tipo`: solo viajan los campos del tipo elegido. */
export function construirTipo(valores: ValoresTipo): DatosTipoRequest {
  const tipo = valores.tipo as TipoPrestador;
  if (esGuia(tipo)) {
    return { tipo, modalidades: [...valores.modalidades], idiomas: idiomasDe(valores.idiomas) };
  }
  return { tipo, numeroEmpleados: valores.numeroEmpleados ?? undefined };
}

/** El cuerpo de `POST /prestador/registro`. */
export function construirRegistro(valores: ValoresRegistro): RegistroPrestadorRequest {
  return { numeroRnt: valores.numeroRnt.trim(), ...construirTipo(valores) };
}

/** Lo que el formulario del tipo muestra con los datos que ya tiene el prestador. */
export function valoresDeTipo(prestador: PrestadorPropio): ValoresTipo {
  const modalidades = (prestador.modalidades ?? []).filter((m): m is ModalidadRecorrido =>
    (MODALIDADES_DEL_GUIA as readonly string[]).includes(m),
  );
  return {
    tipo: prestador.tipo,
    numeroEmpleados: prestador.numeroEmpleados ?? null,
    modalidades,
    idiomas: (prestador.idiomas ?? []).join(', '),
  };
}

// --- Lo que se le dice al prestador ---

/** Qué pasó con el RNT que acaba de registrar o cambiar. */
export const CLAVE_RESULTADO_RNT: Readonly<Record<EstadoRnt, ClaveTexto>> = {
  ACTIVO: 'prestador.rnt.resultado.ACTIVO',
  VERIFICACION_PENDIENTE: 'prestador.rnt.resultado.VERIFICACION_PENDIENTE',
  NO_ENCONTRADO: 'prestador.rnt.resultado.NO_ENCONTRADO',
  INACTIVO: 'prestador.rnt.resultado.INACTIVO',
};

/** La categoría por tamaño que el núcleo saca del RNT (RF-72). */
export const CATEGORIA_PRESTADOR = tablaDeTextos<CategoriaPrestador>({
  GUIA_INDEPENDIENTE: 'prestador.rnt.categoria.GUIA_INDEPENDIENTE',
  MICRO_PEQUENO: 'prestador.rnt.categoria.MICRO_PEQUENO',
  MEDIANO: 'prestador.rnt.categoria.MEDIANO',
  GRANDE: 'prestador.rnt.categoria.GRANDE',
});
