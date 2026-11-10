import type { components } from './recomendacion-api';

/**
 * Nombres cortos para los tipos generados desde contratos/recomendacion.openapi.yaml (npm run gen:api). Igual que modelos.ts con el
 * catálogo y modelos-nucleo.ts con el núcleo: aquí no se declara ningún tipo a mano.
 */
type Esquemas = components['schemas'];

/** Lo que busca el turista: el texto libre (hasta 500 caracteres) y los filtros, todos opcionales pero hace falta alguno (DR-02). */
export type ConsultaRecomendacion = Esquemas['ConsultaRecomendacion'];
/** De 3 a 5 ofertas explicadas, o de 0 a 2 con el aviso de pocas coincidencias (RF-26, RF-27). */
export type Recomendaciones = Esquemas['Recomendaciones'];
export type Recomendacion = Esquemas['Recomendacion'];
export type OfertaRecomendada = Esquemas['OfertaRecomendada'];
/** Quién eligió las recomendaciones; se le muestra al turista. */
export type MotorRecomendacion = Esquemas['MotorRecomendacion'];
export type CriterioAfinidad = Esquemas['CriterioAfinidad'];

/** Las alternativas a una oferta de una zona con alta afluencia (RF-29). */
export type Alternativas = Esquemas['Alternativas'];
export type ZonaOrigen = Esquemas['ZonaOrigen'];

/** Las condiciones activas del destino por zona (RF-28). */
export type Alerta = Esquemas['Alerta'];
export type AlertasZona = Esquemas['AlertasZona'];
export type ResumenAlertas = Esquemas['ResumenAlertas'];
export type ZonaConAlertas = Esquemas['ZonaConAlertas'];
export type TipoAlerta = Esquemas['TipoAlerta'];
export type NivelAlerta = Esquemas['NivelAlerta'];
