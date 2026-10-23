import { TipoPrestador } from '../api/modelos';

/** Un prestador de los datos de demostración del catálogo. */
export interface PrestadorDemo {
  readonly id: string;
  readonly nombre: string;
  readonly tipo: TipoPrestador;
}

/**
 * IDENTIDAD PROVISIONAL: no hay login. Estos son los prestadores de los datos de demostración del backend
 * (DatosSantaMarta); el último tiene el RNT inactivo y sirve para mostrar el rechazo al publicar.
 * Se eligen en el selector "Usuario demo" de la cabecera, junto con los turistas demo. Cuando exista el JWT del
 * núcleo, esta lista y el selector desaparecen.
 */
export const PRESTADORES_DEMO: readonly PrestadorDemo[] = [
  { id: '10000000-0000-0000-0000-000000000001', nombre: 'Agencia', tipo: 'AGENCIA_OPERADORA' },
  { id: '20000000-0000-0000-0000-000000000002', nombre: 'Guía', tipo: 'GUIA_INDEPENDIENTE' },
  { id: '30000000-0000-0000-0000-000000000003', nombre: 'Hotel', tipo: 'ALOJAMIENTO' },
  { id: '40000000-0000-0000-0000-000000000004', nombre: 'Restaurante', tipo: 'RESTAURANTE' },
  { id: '50000000-0000-0000-0000-000000000005', nombre: 'Agencia sin RNT', tipo: 'AGENCIA_OPERADORA' },
];
