import type { components, operations } from './catalogo-api';
import type { components as componentesNucleo } from './nucleo-api';
import type { components as componentesRecomendacion } from './recomendacion-api';

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
/** Los códigos de error de los tres servicios: catálogo, núcleo y recomendación. */
export type CodigoError =
  | Problema['codigo']
  | componentesNucleo['schemas']['Problema']['codigo']
  | componentesRecomendacion['schemas']['Problema']['codigo'];

/** Parámetros de GET /ofertas, tal como los define el contrato. */
export type ConsultaOfertas = NonNullable<operations['buscarOfertas']['parameters']['query']>;

/** El contenido de una versión de oferta como lo envió el prestador, para que el administrador la revise (RF-79). */
export type ContenidoVersion = Esquemas['ContenidoVersion'];
/** Una oferta que espera la decisión del administrador, con su versión pendiente y, si es una edición, la publicada. */
export type OfertaPendiente = Esquemas['OfertaPendiente'];
export type PaginaOfertasPendientes = Esquemas['PaginaOfertasPendientes'];
/** Una opinión que espera la decisión del administrador; no lleva al turista (RF-80). */
export type OpinionPendiente = Esquemas['OpinionPendiente'];
export type PaginaOpinionesPendientes = Esquemas['PaginaOpinionesPendientes'];
