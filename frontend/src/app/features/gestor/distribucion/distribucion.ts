import { LOCALE, idiomaActual } from '../../../core/idioma/idioma';
import { DistribucionCategoria, ParticipacionCategoria } from '../../../core/api/modelos-nucleo';
import { tablaDeTextos } from '../../../core/idioma/tabla-de-textos';
import { conversion } from '../embudo/embudo';

export type Categoria = ParticipacionCategoria['categoria'];

/**
 * El nombre de cada categoría de prestador. Las cuatro son las mismas del panel del prestador (por el tamaño de su RNT, RF-72);
 * `SIN_CATEGORIA` solo aparece si hay solicitudes anteriores cuyo prestador no se encontró.
 */
export const NOMBRE_DE_CATEGORIA = tablaDeTextos<Categoria>({
  GRANDE: 'prestador.rnt.categoria.GRANDE',
  MEDIANO: 'prestador.rnt.categoria.MEDIANO',
  MICRO_PEQUENO: 'prestador.rnt.categoria.MICRO_PEQUENO',
  GUIA_INDEPENDIENTE: 'prestador.rnt.categoria.GUIA_INDEPENDIENTE',
  SIN_CATEGORIA: 'gestor.distribucion.categoria.SIN_CATEGORIA',
});

/** De los prestadores más grandes a los más pequeños, y al final los que no se pudieron clasificar. */
export const ORDEN_DE_CATEGORIAS: readonly Categoria[] = [
  'GRANDE',
  'MEDIANO',
  'MICRO_PEQUENO',
  'GUIA_INDEPENDIENTE',
  'SIN_CATEGORIA',
];

/** Una categoría con lo que se muestra de ella. `null` es un dato que el núcleo no entrega: un conteo de 1 o 2 (RET-03). */
export interface FilaCategoria {
  readonly categoria: Categoria;
  /** Las solicitudes enviadas por prestadores de la categoría; `null` si fueron 1 o 2. */
  readonly solicitudes: number | null;
  /** Sobre el total de solicitudes, con un decimal, tal como lo calcula el núcleo. */
  readonly porcentaje: number | null;
  /** Las que llegaron a pagarse; `null` si fueron 1 o 2. */
  readonly pagadas: number | null;
  /** Qué parte de las solicitudes de la categoría se pagó, con un decimal; `null` si falta alguna cantidad o no hubo solicitudes. */
  readonly tasaDePago: number | null;
}

/**
 * Las categorías en el orden de `ORDEN_DE_CATEGORIAS`, con su tasa de pago. El porcentaje sobre el total es el del núcleo; la tasa
 * de pago se calcula aquí solo cuando las solicitudes y las pagadas se conocen y hubo solicitudes. Un conteo omitido sigue omitido.
 */
export function filasDeDistribucion(distribucion: DistribucionCategoria): FilaCategoria[] {
  const posicion = (categoria: Categoria) => {
    const i = ORDEN_DE_CATEGORIAS.indexOf(categoria);
    return i === -1 ? ORDEN_DE_CATEGORIAS.length : i;
  };
  return [...distribucion.categorias]
    .sort((a, b) => posicion(a.categoria) - posicion(b.categoria))
    .map((fila) => {
      const solicitudes = fila.solicitudes ?? null;
      const pagadas = fila.pagadas ?? null;
      return {
        categoria: fila.categoria,
        solicitudes,
        porcentaje: fila.porcentaje ?? null,
        pagadas,
        tasaDePago: conversion(pagadas, solicitudes),
      };
    });
}

/** Las categorías con más solicitudes entre las que se conocen (más de cero). Si hay un empate salen todas. */
export function categoriasMayores(filas: readonly FilaCategoria[]): Categoria[] {
  const mayor = filas.reduce((maximo, fila) => Math.max(maximo, fila.solicitudes ?? 0), 0);
  return mayor > 0 ? filas.filter((fila) => fila.solicitudes === mayor).map((fila) => fila.categoria) : [];
}

export interface CifrasDistribucion {
  /** Todas las solicitudes del periodo; `null` si son 1 o 2. */
  readonly total: number | null;
  /** Las categorías que tuvieron solicitudes; una de conteo omitido (1 o 2) cuenta, porque tuvo. */
  readonly categoriasConSolicitudes: number;
  readonly mayores: Categoria[];
}

/**
 * Las cifras de arriba de la pantalla. El total es el del núcleo: no se suman las categorías, porque las de 1 o 2 solicitudes no
 * traen su conteo y la suma saldría de menos.
 */
export function cifrasDeDistribucion(distribucion: DistribucionCategoria, filas: readonly FilaCategoria[]): CifrasDistribucion {
  return {
    total: distribucion.total ?? null,
    categoriasConSolicitudes: filas.filter((fila) => fila.solicitudes !== 0).length,
    mayores: categoriasMayores(filas),
  };
}

/** Si el periodo no tuvo ninguna solicitud (un cero explícito del núcleo, no un total omitido). */
export function distribucionVacia(distribucion: DistribucionCategoria): boolean {
  return distribucion.total === 0;
}

/** Varios nombres como una lista legible según el idioma: "A y B" en español, "A and B" en inglés. */
export function listaDeNombres(nombres: readonly string[]): string {
  return new Intl.ListFormat(LOCALE[idiomaActual()], { style: 'long', type: 'conjunction' }).format(nombres);
}
