import { AvisoOperador } from '../../core/api/modelos-nucleo';
import type { ClaveTexto } from '../../core/idioma/textos';
import { normalizarHora } from '../turista/solicitudes/peticion-solicitud';

/** Lo que se muestra de un aviso: los datos de la solicitud que lo produjo, ya limpios. */
export interface ResumenAviso {
  readonly nombreOferta: string | null;
  /** AAAA-MM-DD. */
  readonly fechaInicio: string | null;
  /** HH:mm, de la hora de salida (o de la reserva en un restaurante). */
  readonly hora: string | null;
  readonly personas: number | null;
}

/** El valor de un dato del aviso, o null si falta o está vacío (los datos llegan todos como texto). */
function dato(aviso: AvisoOperador, clave: string): string | null {
  const valor = aviso.datos?.[clave]?.trim();
  return valor ? valor : null;
}

/**
 * Los datos de la solicitud dentro de un aviso. [SUPUESTO] El contrato describe `datos` como un mapa de texto sin fijar sus
 * claves; para `NUEVA_SOLICITUD` el ejemplo trae `nombreOferta`, `fechaInicio`, `horaSalida`, `personas` y `estado`. Si falta
 * alguna se omite, no se inventa: el enlace lleva a la solicitud, que sí muestra todo.
 */
export function resumenDeAviso(aviso: AvisoOperador): ResumenAviso {
  const hora = dato(aviso, 'horaSalida') ?? dato(aviso, 'horaReserva');
  const personas = Number(dato(aviso, 'personas'));
  return {
    nombreOferta: dato(aviso, 'nombreOferta'),
    fechaInicio: dato(aviso, 'fechaInicio'),
    hora: hora ? normalizarHora(hora) : null,
    personas: Number.isInteger(personas) && personas > 0 ? personas : null,
  };
}

/** El título del aviso según su tipo; el operador solo recibe `NUEVA_SOLICITUD`, y cualquier otro tipo es un aviso genérico. */
export function claveDeTipoDeAviso(tipo: AvisoOperador['tipo']): ClaveTexto {
  return tipo === 'NUEVA_SOLICITUD'
    ? 'operador.avisos.tipo.NUEVA_SOLICITUD'
    : 'operador.avisos.tipo.otro';
}
