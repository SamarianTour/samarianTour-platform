import type { components, operations } from './catalogo-api';

/**
 * Nombres cortos para los tipos generados desde contratos/catalogo.openapi.yaml (npm run gen:api).
 * Aquí no se declara ningún tipo a mano: si el contrato cambia, cambian estos alias.
 */
type Esquemas = components['schemas'];

export type TipoOferta = Esquemas['TipoOferta'];
export type TipoPrestador = Esquemas['TipoPrestador'];
export type TipoActividad = Esquemas['TipoActividad'];
export type NivelEsfuerzo = Esquemas['NivelEsfuerzo'];
export type ModalidadRecorrido = Esquemas['ModalidadRecorrido'];
export type EstadoModeracion = Esquemas['EstadoModeracion'];
export type Coordenada = Esquemas['Coordenada'];
export type OfertaResumen = Esquemas['OfertaResumen'];
export type PaginaOfertas = Esquemas['PaginaOfertas'];
export type FichaOferta = Esquemas['FichaOferta'];
export type Foto = Esquemas['Foto'];
export type Zona = Esquemas['Zona'];
export type PerfilPublico = Esquemas['PerfilPublico'];
export type OfertaRequest = Esquemas['OfertaRequest'];
export type OfertaPrestador = Esquemas['OfertaPrestador'];
export type PaginaOfertasPrestador = Esquemas['PaginaOfertasPrestador'];
export type VersionOferta = Esquemas['Version'];
export type EstadoOpinion = Esquemas['EstadoOpinion'];
export type OpinionRequest = Esquemas['OpinionRequest'];
export type MiOpinion = Esquemas['MiOpinion'];
export type PaginaMisOpiniones = Esquemas['PaginaMisOpiniones'];
export type OpinionPublica = Esquemas['OpinionPublica'];
export type ResumenOpiniones = Esquemas['ResumenOpiniones'];
export type OpinionesOferta = Esquemas['OpinionesOferta'];
export type Problema = Esquemas['Problema'];
export type CodigoError = Problema['codigo'];

/** Parámetros de GET /ofertas, tal como los define el contrato. */
export type ConsultaOfertas = NonNullable<operations['buscarOfertas']['parameters']['query']>;
