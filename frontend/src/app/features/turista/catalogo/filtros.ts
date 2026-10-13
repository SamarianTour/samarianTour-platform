import { Params } from '@angular/router';
import { nivelEsfuerzoValues, tipoActividadValues, tipoOfertaValues } from '../../../core/api/catalogo-api';
import { ConsultaOfertas, NivelEsfuerzo, TipoActividad, TipoOferta } from '../../../core/api/modelos';

/** Ofertas por página en el catálogo. */
export const TAMANO_PAGINA = 12;

/** Los filtros del catálogo. Viven en los query params, así que la URL se puede compartir. */
export interface FiltrosCatalogo {
  readonly tipo: TipoOferta | null;
  readonly zonaId: string | null;
  readonly tipoActividad: TipoActividad | null;
  readonly precioMin: number | null;
  readonly precioMax: number | null;
  /** "Hasta el nivel": incluye los niveles menores (DC-25). */
  readonly nivelEsfuerzoMaximo: NivelEsfuerzo | null;
  readonly accesibleSillaRuedas: boolean;
  /** Empieza en 0, como en la API. */
  readonly pagina: number;
}

export const SIN_FILTROS: FiltrosCatalogo = {
  tipo: null,
  zonaId: null,
  tipoActividad: null,
  precioMin: null,
  precioMax: null,
  nivelEsfuerzoMaximo: null,
  accesibleSillaRuedas: false,
  pagina: 0,
};

/** Lo mínimo que se necesita de un ParamMap, para poder probar sin el enrutador. */
interface LectorDeParams {
  get(nombre: string): string | null;
}

/** Lee los filtros de la URL. Un valor desconocido o mal formado se ignora: una URL vieja no rompe la página. */
export function filtrosDesdeUrl(params: LectorDeParams): FiltrosCatalogo {
  return {
    tipo: valorDe(tipoOfertaValues, params.get('tipo')),
    zonaId: params.get('zonaId') || null,
    tipoActividad: valorDe(tipoActividadValues, params.get('tipoActividad')),
    precioMin: numero(params.get('precioMin')),
    precioMax: numero(params.get('precioMax')),
    nivelEsfuerzoMaximo: valorDe(nivelEsfuerzoValues, params.get('nivelEsfuerzoMaximo')),
    accesibleSillaRuedas: params.get('accesibleSillaRuedas') === 'true',
    pagina: Math.max(0, Math.trunc(numero(params.get('page')) ?? 0)),
  };
}

/** Query params de la URL del catálogo: solo los filtros con valor, para que la URL quede corta. */
export function urlDeFiltros(filtros: FiltrosCatalogo): Params {
  const params: Params = {};
  if (filtros.tipo) params['tipo'] = filtros.tipo;
  if (filtros.zonaId) params['zonaId'] = filtros.zonaId;
  if (filtros.tipoActividad) params['tipoActividad'] = filtros.tipoActividad;
  if (filtros.precioMin !== null) params['precioMin'] = filtros.precioMin;
  if (filtros.precioMax !== null) params['precioMax'] = filtros.precioMax;
  if (filtros.nivelEsfuerzoMaximo) params['nivelEsfuerzoMaximo'] = filtros.nivelEsfuerzoMaximo;
  if (filtros.accesibleSillaRuedas) params['accesibleSillaRuedas'] = true;
  if (filtros.pagina > 0) params['page'] = filtros.pagina;
  return params;
}

/** Parámetros de GET /ofertas para estos filtros: siempre llevan la página y el tamaño. */
export function consultaDeFiltros(filtros: FiltrosCatalogo): ConsultaOfertas {
  return {
    ...(filtros.tipo && { tipo: filtros.tipo }),
    ...(filtros.zonaId && { zonaId: filtros.zonaId }),
    ...(filtros.tipoActividad && { tipoActividad: filtros.tipoActividad }),
    ...(filtros.precioMin !== null && { precioMin: filtros.precioMin }),
    ...(filtros.precioMax !== null && { precioMax: filtros.precioMax }),
    ...(filtros.nivelEsfuerzoMaximo && { nivelEsfuerzoMaximo: filtros.nivelEsfuerzoMaximo }),
    ...(filtros.accesibleSillaRuedas && { accesibleSillaRuedas: true }),
    page: filtros.pagina,
    size: TAMANO_PAGINA,
  };
}

/** Indica si hay algún filtro puesto, sin contar la página. */
export function hayFiltros(filtros: FiltrosCatalogo): boolean {
  return Object.keys(urlDeFiltros({ ...filtros, pagina: 0 })).length > 0;
}

function valorDe<T extends string>(permitidos: readonly T[], valor: string | null): T | null {
  return permitidos.find((permitido) => permitido === valor) ?? null;
}

function numero(valor: string | null): number | null {
  if (valor === null || valor.trim() === '') {
    return null;
  }
  const convertido = Number(valor);
  return Number.isFinite(convertido) && convertido >= 0 ? convertido : null;
}
