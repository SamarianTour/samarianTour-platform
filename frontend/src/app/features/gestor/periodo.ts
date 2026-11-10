import { ActivatedRoute } from '@angular/router';
import { Observable, distinctUntilChanged, map } from 'rxjs';
import { fechaEnColombia, sumarDias } from '../turista/solicitudes/peticion-solicitud';

/** El periodo de los indicadores: dos fechas AAAA-MM-DD, las dos incluidas, en hora de Colombia. */
export interface Periodo {
  readonly desde: string;
  readonly hasta: string;
}

/** El núcleo acepta periodos de hasta 366 días (`desde` y `hasta` incluidos). */
export const MAXIMO_DIAS = 366;

/** Los periodos que se eligen con un solo clic, en el orden en que se ofrecen. */
export const ATAJOS = ['ultimos7', 'ultimos30', 'ultimos90', 'esteMes', 'mesAnterior'] as const;
export type Atajo = (typeof ATAJOS)[number];

const FECHA = /^\d{4}-\d{2}-\d{2}$/;

/** Si es una fecha AAAA-MM-DD que existe en el calendario (no el 31 de febrero). */
export function esFecha(texto: string | null | undefined): texto is string {
  if (!texto || !FECHA.test(texto)) {
    return false;
  }
  const [anio, mes, dia] = texto.split('-').map(Number);
  const fecha = new Date(Date.UTC(anio, mes - 1, dia));
  return fecha.getUTCFullYear() === anio && fecha.getUTCMonth() === mes - 1 && fecha.getUTCDate() === dia;
}

/** Hoy en la hora de Colombia, donde el núcleo cuenta los días: no la del navegador. */
export function hoyEnColombia(ahora: number = Date.now()): string {
  return fechaEnColombia(ahora);
}

/** Cuántos días tiene el periodo, contando el primero y el último. */
export function diasDelPeriodo({ desde, hasta }: Periodo): number {
  const milisegundos = (fecha: string) => {
    const [anio, mes, dia] = fecha.split('-').map(Number);
    return Date.UTC(anio, mes - 1, dia);
  };
  return Math.round((milisegundos(hasta) - milisegundos(desde)) / 86_400_000) + 1;
}

/** El periodo de un atajo, contado desde hoy. "Este mes" va del día 1 a hoy; "Mes anterior", del día 1 al último día del mes pasado. */
export function periodoDeAtajo(atajo: Atajo, hoy: string): Periodo {
  switch (atajo) {
    case 'ultimos7':
      return { desde: sumarDias(hoy, -6), hasta: hoy };
    case 'ultimos30':
      return { desde: sumarDias(hoy, -29), hasta: hoy };
    case 'ultimos90':
      return { desde: sumarDias(hoy, -89), hasta: hoy };
    case 'esteMes':
      return { desde: `${hoy.slice(0, 8)}01`, hasta: hoy };
    case 'mesAnterior': {
      const primeroDeEsteMes = `${hoy.slice(0, 8)}01`;
      const ultimoDelPasado = sumarDias(primeroDeEsteMes, -1);
      return { desde: `${ultimoDelPasado.slice(0, 8)}01`, hasta: ultimoDelPasado };
    }
  }
}

/** Lo que se consulta cuando la dirección no trae periodo: los últimos 30 días, como el núcleo. */
export function periodoPorDefecto(hoy: string): Periodo {
  return periodoDeAtajo('ultimos30', hoy);
}

/** Cuál atajo corresponde exactamente a este periodo, para marcarlo; `null` si fue escrito a mano. */
export function atajoActivo(periodo: Periodo, hoy: string): Atajo | null {
  return ATAJOS.find((atajo) => {
    const propio = periodoDeAtajo(atajo, hoy);
    return propio.desde === periodo.desde && propio.hasta === periodo.hasta;
  }) ?? null;
}

export type MotivoPeriodo = 'desdeRequerida' | 'hastaRequerida' | 'desdeMayor' | 'hastaFutura' | 'demasiadoLargo';

/**
 * Por qué un periodo escrito no se puede consultar, o `null` si sirve. Repite lo que el núcleo exige (máximo 366 días) para avisar
 * antes de enviar.
 * [SUPUESTO] El contrato no dice qué pasa con una fecha final futura; no tiene sentido pedir días que no han ocurrido, así que se
 * avisa antes de enviar.
 */
export function validarPeriodo(desde: string, hasta: string, hoy: string): MotivoPeriodo | null {
  if (!esFecha(desde)) {
    return 'desdeRequerida';
  }
  if (!esFecha(hasta)) {
    return 'hastaRequerida';
  }
  if (desde > hasta) {
    return 'desdeMayor';
  }
  if (hasta > hoy) {
    return 'hastaFutura';
  }
  return diasDelPeriodo({ desde, hasta }) > MAXIMO_DIAS ? 'demasiadoLargo' : null;
}

/**
 * El periodo que dicen los parámetros `desde` y `hasta` de la dirección. Si faltan, uno de los dos está mal escrito o el periodo no
 * es válido, se usa el de por defecto: una dirección vieja o a medias no debe romper la pantalla.
 */
export function periodoDeParametros(desde: string | null, hasta: string | null, hoy: string): Periodo {
  if (desde !== null && hasta !== null && validarPeriodo(desde, hasta, hoy) === null) {
    return { desde, hasta };
  }
  return periodoPorDefecto(hoy);
}

/**
 * El periodo de la dirección de una pestaña, cada vez que cambia: las tres vistas lo leen igual. Solo emite si el periodo cambió de
 * verdad, así un cambio de pestaña o de otro parámetro no repite la consulta.
 */
export function periodoDeLaRuta(ruta: Pick<ActivatedRoute, 'queryParamMap'>, ahora: () => number = Date.now): Observable<Periodo> {
  return ruta.queryParamMap.pipe(
    map((parametros) => periodoDeParametros(parametros.get('desde'), parametros.get('hasta'), hoyEnColombia(ahora()))),
    distinctUntilChanged((a, b) => a.desde === b.desde && a.hasta === b.hasta),
  );
}

/** Los parámetros de la dirección y de la consulta para un periodo. */
export function parametrosDe(periodo: Periodo): { desde: string; hasta: string } {
  return { desde: periodo.desde, hasta: periodo.hasta };
}
