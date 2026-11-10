import { NivelEsfuerzo, TipoActividad, TipoOferta } from '../../core/api/modelos';
import { ConsultaRecomendacion } from '../../core/api/modelos-recomendacion';

/** El texto libre del turista puede tener hasta 500 caracteres, sin contar los espacios de los extremos (DR-02). */
export const MAXIMO_TEXTO = 500;

/** Los filtros que acepta el servicio de recomendación. Todos son opcionales. */
export interface FiltrosRecomendacion {
  readonly tiposOferta: readonly TipoOferta[];
  readonly tiposActividad: readonly TipoActividad[];
  readonly zonaId: string;
  /** Filtro duro: si está marcado, solo ofertas accesibles en silla de ruedas. */
  readonly accesibleSillaRuedas: boolean;
  /** Filtro duro: "hasta el nivel", incluye los menores. */
  readonly nivelEsfuerzoMaximo: NivelEsfuerzo | '';
  /** Filtro duro, en pesos colombianos. */
  readonly precioMaximo: number | null;
}

export const SIN_FILTROS: FiltrosRecomendacion = {
  tiposOferta: [],
  tiposActividad: [],
  zonaId: '',
  accesibleSillaRuedas: false,
  nivelEsfuerzoMaximo: '',
  precioMaximo: null,
};

/** Si hay algún filtro elegido. */
export function hayFiltros(filtros: FiltrosRecomendacion): boolean {
  return (
    filtros.tiposOferta.length > 0 ||
    filtros.tiposActividad.length > 0 ||
    filtros.zonaId !== '' ||
    filtros.accesibleSillaRuedas ||
    filtros.nivelEsfuerzoMaximo !== '' ||
    filtros.precioMaximo !== null
  );
}

export type MotivoConsulta = 'precioInvalido' | 'textoLargo' | 'vacia';

/**
 * Por qué una consulta no se puede enviar, o `null` si sirve. Repite lo que el servicio exige para avisar antes de enviar: hace falta
 * el texto o al menos un filtro, el texto tiene hasta 500 caracteres y el precio máximo es mayor que cero.
 */
export function validarConsulta(texto: string, filtros: FiltrosRecomendacion): MotivoConsulta | null {
  const limpio = texto.trim();
  if (filtros.precioMaximo !== null && !(filtros.precioMaximo > 0)) {
    return 'precioInvalido';
  }
  if (limpio.length > MAXIMO_TEXTO) {
    return 'textoLargo';
  }
  return limpio === '' && !hayFiltros(filtros) ? 'vacia' : null;
}

/**
 * El cuerpo de `POST /recomendaciones`, sin el idioma (lo agrega el servicio). Solo viaja lo que se escribió o se eligió: sin espacios
 * en los extremos del texto, sin listas vacías y sin los filtros apagados.
 */
export function construirConsulta(texto: string, filtros: FiltrosRecomendacion): Omit<ConsultaRecomendacion, 'idioma'> {
  const limpio = texto.trim();
  return {
    ...(limpio !== '' && { texto: limpio }),
    ...(filtros.tiposOferta.length > 0 && { tiposOferta: [...filtros.tiposOferta] }),
    ...(filtros.tiposActividad.length > 0 && { tiposActividad: [...filtros.tiposActividad] }),
    ...(filtros.zonaId !== '' && { zonaId: filtros.zonaId }),
    ...(filtros.accesibleSillaRuedas && { accesibleSillaRuedas: true }),
    ...(filtros.nivelEsfuerzoMaximo !== '' && { nivelEsfuerzoMaximo: filtros.nivelEsfuerzoMaximo }),
    ...(filtros.precioMaximo !== null && { precioMaximo: filtros.precioMaximo }),
  };
}
