import { EtapaEmbudo, Embudo } from '../../../core/api/modelos-nucleo';
import { ANCHO_MENOS_DE_TRES } from '../demanda/demanda';

export type NombreEtapa = EtapaEmbudo['etapa'];

/** Las etapas en el orden del embudo: cada solicitud enviada puede llegar a ser atendida, luego confirmada y luego pagada. */
export const ETAPAS: readonly NombreEtapa[] = ['ENVIADAS', 'ATENDIDAS', 'CONFIRMADAS', 'PAGADAS'];

/** Una etapa del embudo con todo lo que se muestra de ella. `null` es un dato que el núcleo no entrega (RET-03). */
export interface EtapaCalculada {
  readonly etapa: NombreEtapa;
  /** Cuántas solicitudes llegaron a la etapa; `null` si fueron 1 o 2. */
  readonly solicitudes: number | null;
  /** Sobre las enviadas, con un decimal, tal como lo calcula el núcleo; `null` si no hay. */
  readonly porcentaje: number | null;
  /** Qué parte de las de la etapa anterior llegó a esta, con un decimal; `null` si no se puede calcular sin inventar nada. */
  readonly deLaAnterior: number | null;
}

/**
 * Las etapas con su conversión a partir de la anterior. El porcentaje sobre las enviadas es el del núcleo; el de la etapa anterior
 * se calcula aquí, pero solo cuando las dos cantidades se conocen y la anterior no es cero. Si alguna fue omitida (1 o 2
 * solicitudes) no se estima: queda en `null`. Siempre salen las cuatro etapas, en orden, aunque el núcleo no mande alguna.
 */
export function etapasCalculadas(etapas: readonly EtapaEmbudo[]): EtapaCalculada[] {
  const calculadas: EtapaCalculada[] = [];
  for (const nombre of ETAPAS) {
    const dato = etapas.find((etapa) => etapa.etapa === nombre);
    const solicitudes = dato?.solicitudes ?? null;
    const anterior = calculadas[calculadas.length - 1];
    calculadas.push({
      etapa: nombre,
      solicitudes,
      porcentaje: dato?.porcentaje ?? null,
      deLaAnterior: anterior ? conversion(solicitudes, anterior.solicitudes) : null,
    });
  }
  return calculadas;
}

/** Qué porcentaje de `anterior` es `actual`, con un decimal; `null` si falta alguno o la anterior es cero. */
export function conversion(actual: number | null, anterior: number | null): number | null {
  if (actual === null || anterior === null || anterior <= 0) {
    return null;
  }
  return Math.round((actual / anterior) * 1000) / 10;
}

/** El ancho de la barra de una etapa, de 0 a 100: es el porcentaje sobre las enviadas. Un conteo omitido lleva la marca rayada. */
export function anchoDeEtapa(etapa: EtapaCalculada): number {
  if (etapa.solicitudes === null) {
    return ANCHO_MENOS_DE_TRES;
  }
  if (etapa.solicitudes <= 0 || etapa.porcentaje === null) {
    return 0;
  }
  return Math.min(100, Math.max(3, Math.round(etapa.porcentaje)));
}

export interface CifrasEmbudo {
  readonly enviadas: number | null;
  readonly pagadas: number | null;
  /** El porcentaje de las enviadas que se pagaron; `null` si no hay. */
  readonly conversionTotal: number | null;
}

/** Las tres cifras de arriba de la pantalla, de las etapas del total. */
export function cifrasDeEmbudo(etapas: readonly EtapaCalculada[]): CifrasEmbudo {
  const de = (nombre: NombreEtapa) => etapas.find((etapa) => etapa.etapa === nombre);
  return {
    enviadas: de('ENVIADAS')?.solicitudes ?? null,
    pagadas: de('PAGADAS')?.solicitudes ?? null,
    conversionTotal: de('PAGADAS')?.porcentaje ?? null,
  };
}

/** Si no se envió ninguna solicitud (un cero, no un conteo omitido): no hay nada que dibujar. */
export function embudoVacio(etapas: readonly EtapaCalculada[]): boolean {
  return etapas[0]?.solicitudes === 0;
}

/** Los embudos por tipo de oferta, cada uno con sus etapas calculadas, en el orden del núcleo. */
export function embudosPorTipo(embudo: Embudo): { tipoOferta: Embudo['porTipo'][number]['tipoOferta']; etapas: EtapaCalculada[] }[] {
  return embudo.porTipo.map((tipo) => ({ tipoOferta: tipo.tipoOferta, etapas: etapasCalculadas(tipo.etapas) }));
}
