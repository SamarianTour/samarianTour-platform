import { NivelAlerta, ResumenAlertas, TipoAlerta, ZonaConAlertas } from '../../../core/api/modelos-recomendacion';
import type { ClaveTexto } from '../../../core/idioma/textos';

/** De menor a mayor gravedad, igual que el contrato. */
const ORDEN_DE_NIVEL: Readonly<Record<NivelAlerta, number>> = { AMARILLA: 1, NARANJA: 2, ROJA: 3 };

/** El nombre del nivel en palabras: el color solo nunca basta para saber qué tan grave es (WCAG 1.4.1). */
export const CLAVE_DE_NIVEL: Readonly<Record<NivelAlerta, ClaveTexto>> = {
  AMARILLA: 'recomendacion.alerta.nivel.AMARILLA',
  NARANJA: 'recomendacion.alerta.nivel.NARANJA',
  ROJA: 'recomendacion.alerta.nivel.ROJA',
};

export const CLAVE_DE_TIPO: Readonly<Record<TipoAlerta, ClaveTexto>> = {
  HIDROLOGICA: 'recomendacion.alerta.tipo.HIDROLOGICA',
  LLUVIAS: 'recomendacion.alerta.tipo.LLUVIAS',
  DESLIZAMIENTO: 'recomendacion.alerta.tipo.DESLIZAMIENTO',
  OLEAJE: 'recomendacion.alerta.tipo.OLEAJE',
  CIERRE: 'recomendacion.alerta.tipo.CIERRE',
  OTRA: 'recomendacion.alerta.tipo.OTRA',
};

/** El nivel más grave de las alertas de una zona; sirve para ordenar y para el resumen. */
export function nivelMaximo(zona: Pick<ZonaConAlertas, 'alertas'>): NivelAlerta {
  return zona.alertas.reduce<NivelAlerta>(
    (maximo, alerta) => (ORDEN_DE_NIVEL[alerta.nivel] > ORDEN_DE_NIVEL[maximo] ? alerta.nivel : maximo),
    'AMARILLA',
  );
}

/**
 * Las zonas con alertas, las de nivel más grave primero. El servicio las entrega en el orden de su id (que para el turista no
 * significa nada); a igual nivel se conserva ese orden. [SUPUESTO] Ordenar por gravedad es decisión del frontend: así lo más
 * urgente queda arriba.
 */
export function zonasPorGravedad(resumen: ResumenAlertas): ZonaConAlertas[] {
  return resumen.zonas
    .map((zona, posicion) => ({ zona, posicion }))
    .sort(
      (a, b) =>
        ORDEN_DE_NIVEL[nivelMaximo(b.zona)] - ORDEN_DE_NIVEL[nivelMaximo(a.zona)] || a.posicion - b.posicion,
    )
    .map(({ zona }) => zona);
}

/** Cuántas alertas hay en total, para el resumen de la página. */
export function totalDeAlertas(resumen: ResumenAlertas): number {
  return resumen.zonas.reduce((suma, zona) => suma + zona.alertas.length, 0);
}
