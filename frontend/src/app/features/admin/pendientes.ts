import { AvisoBandeja, EventoPagoFallido } from '../../core/api/modelos-nucleo';
import { tablaDeTextos } from '../../core/idioma/tabla-de-textos';
import type { ClaveTexto } from '../../core/idioma/textos';
import { traducir } from '../../core/idioma/traducir';

/** A qué módulo del catálogo va un aviso: ofertas u opiniones. */
export const ETIQUETA_DESTINO_AVISO = tablaDeTextos<AvisoBandeja['destino']>({
  OFERTAS: 'admin.pendientes.destino.OFERTAS',
  OPINIONES: 'admin.pendientes.destino.OPINIONES',
});

/** Los tipos de aviso que se conocen (DN-06, RF-13, RF-60, RF-77, RF-79, RF-80); el contrato dice "y otros". */
const CLAVE_DE_TIPO: Readonly<Record<string, ClaveTexto>> = {
  MODERACION: 'admin.pendientes.tipo.MODERACION',
  MODERACION_OPINION: 'admin.pendientes.tipo.MODERACION_OPINION',
  HABILITACION: 'admin.pendientes.tipo.HABILITACION',
  HABILITACION_OPINION: 'admin.pendientes.tipo.HABILITACION_OPINION',
  SUPRESION_TURISTA: 'admin.pendientes.tipo.SUPRESION_TURISTA',
  SUPRESION_PRESTADOR: 'admin.pendientes.tipo.SUPRESION_PRESTADOR',
};

/**
 * El nombre de un tipo de aviso en el idioma vigente. [SUPUESTO] El contrato no cierra la lista de tipos: uno conocido se traduce y
 * uno que no se conoce se muestra tal cual llega, para que no se pierda. Sin tipo, se dice solo que es un aviso.
 */
export function etiquetaDeTipoDeAviso(tipo: string | null | undefined): string {
  if (!tipo) {
    return traducir('admin.pendientes.tipo.ninguno');
  }
  const clave = CLAVE_DE_TIPO[tipo];
  return clave ? traducir(clave) : tipo;
}

/** Los estados de una transacción que la pasarela informa y que se conocen; el resto se muestra tal cual. */
const CLAVE_DE_ESTADO_PASARELA: Readonly<Record<string, ClaveTexto>> = {
  APROBADA: 'admin.pendientes.estadoPago.APROBADA',
  RECHAZADA: 'admin.pendientes.estadoPago.RECHAZADA',
};

/** El estado de la transacción en la pasarela ("APROBADA", "RECHAZADA", ...), traducido si se conoce y tal cual si no. */
export function etiquetaDeEstadoDePago(estado: string): string {
  const clave = CLAVE_DE_ESTADO_PASARELA[estado.toUpperCase()];
  return clave ? traducir(clave) : estado;
}

/** Los avisos del más reciente al más antiguo, sin modificar el original. */
export function ordenarAvisos(avisos: readonly AvisoBandeja[]): AvisoBandeja[] {
  return [...avisos].sort((a, b) => Date.parse(b.creadoEn) - Date.parse(a.creadoEn));
}

/** Los eventos del recibido más reciente al más antiguo, sin modificar el original. */
export function ordenarEventos(eventos: readonly EventoPagoFallido[]): EventoPagoFallido[] {
  return [...eventos].sort((a, b) => Date.parse(b.recibidoEn) - Date.parse(a.recibidoEn));
}

/** Solo un aviso rechazado se puede reenviar; el núcleo responde 409 `AVISO_NO_RECHAZADO` con cualquier otro estado. */
export function puedeReenviar(aviso: Pick<AvisoBandeja, 'estado'>): boolean {
  return aviso.estado === 'RECHAZADO';
}

/**
 * La primera línea de un mensaje de error técnico, recortada: lo que se ve de un vistazo en la lista, sin ocultar el texto completo
 * (que se muestra aparte). Vacía si no hay mensaje.
 */
export function primeraLinea(texto: string | null | undefined, maximo = 140): string {
  const linea = (texto ?? '').split(/\r?\n/).find((parte) => parte.trim() !== '')?.trim() ?? '';
  return linea.length > maximo ? `${linea.slice(0, maximo - 1).trimEnd()}…` : linea;
}

/** Si el mensaje de error tiene más que su primera línea, para ofrecer verlo completo. */
export function tieneMasQueLaPrimeraLinea(texto: string | null | undefined, maximo = 140): boolean {
  const completo = (texto ?? '').trim();
  return completo !== '' && completo !== primeraLinea(texto, maximo);
}
