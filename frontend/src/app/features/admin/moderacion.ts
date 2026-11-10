import { ContenidoVersion, OfertaPendiente, Zona } from '../../core/api/modelos';
import {
  DecisionModerar,
  DecisionOfertaRequest,
  DecisionOpinionRequest,
} from '../../core/api/modelos-nucleo';
import type { ClaveTexto } from '../../core/idioma/textos';
import { traducir } from '../../core/idioma/traducir';
import { MODALIDAD, NIVEL_ESFUERZO, TIPO_ACTIVIDAD } from '../../shared/etiquetas';
import { DuracionPipe } from '../../shared/duracion.pipe';
import { PrecioPipe } from '../../shared/precio.pipe';

/** Las dos colas de moderación del administrador. */
export type TipoCola = 'ofertas' | 'opiniones';

/** La cola de la dirección (`?tipo=opiniones`); cualquier otro valor, o ninguno, abre la de ofertas. */
export function tipoDeCola(valor: string | null): TipoCola {
  return valor === 'opiniones' ? 'opiniones' : 'ofertas';
}

/** El motivo de un rechazo admite hasta 1000 caracteres y se le notifica al prestador o al turista (contrato). */
export const MOTIVO_MAXIMO = 1000;

export type ErrorDeDecision = 'motivoRequerido' | 'motivoLargo';

/**
 * Valida la decisión como lo hace el núcleo: aprobar no necesita nada, y rechazar exige un motivo (que se notifica al prestador
 * o al turista, DN-08) de hasta 1000 caracteres. Devuelve el problema, o null si se puede enviar.
 */
export function validarDecision(decision: DecisionModerar, motivo: string): ErrorDeDecision | null {
  if (decision === 'APROBAR') {
    return null;
  }
  const texto = motivo.trim();
  if (texto === '') {
    return 'motivoRequerido';
  }
  return texto.length > MOTIVO_MAXIMO ? 'motivoLargo' : null;
}

/** El cuerpo de la decisión sobre una versión de oferta. El motivo solo viaja al rechazar. */
export function decisionDeOferta(
  numeroVersion: number,
  decision: DecisionModerar,
  motivo: string,
): DecisionOfertaRequest {
  return {
    numeroVersion,
    decision,
    ...(decision === 'RECHAZAR' ? { motivo: motivo.trim() } : {}),
  };
}

/** El cuerpo de la decisión sobre una opinión. El motivo solo viaja al rechazar. */
export function decisionDeOpinion(decision: DecisionModerar, motivo: string): DecisionOpinionRequest {
  return { decision, ...(decision === 'RECHAZAR' ? { motivo: motivo.trim() } : {}) };
}

/** El núcleo no aprueba una oferta si su prestador no tiene el RNT activo (DN-35): solo se puede rechazar. */
export function puedeAprobar(oferta: Pick<OfertaPendiente, 'rntPrestadorActivo'>): boolean {
  return oferta.rntPrestadorActivo;
}

// --- El contenido de una versión, para revisarlo ---

type Campo = keyof ContenidoVersion;

/** Una línea del contenido de una versión: qué es, su valor ya escrito y si cambió frente a la versión publicada. */
export interface FilaVersion {
  readonly campo: Campo;
  readonly clave: ClaveTexto;
  readonly valor: string;
  /** Texto largo: ocupa todo el ancho. */
  readonly ancho: boolean;
  readonly cambiado: boolean;
}

/**
 * Qué campos son distintos entre la versión pendiente y la publicada. Si no hay versión publicada (es una oferta nueva) no hay
 * nada con qué comparar y el conjunto queda vacío. Las fotos se comparan por su orden y su identificador.
 */
export function camposCambiados(
  pendiente: ContenidoVersion,
  publicada: ContenidoVersion | null | undefined,
): ReadonlySet<Campo> {
  const cambiados = new Set<Campo>();
  if (!publicada) {
    return cambiados;
  }
  const campos = new Set<Campo>([
    ...(Object.keys(pendiente) as Campo[]),
    ...(Object.keys(publicada) as Campo[]),
  ]);
  for (const campo of campos) {
    const nueva = campo === 'fotos' ? pendiente.fotos.map((f) => f.id) : pendiente[campo];
    const vieja = campo === 'fotos' ? publicada.fotos.map((f) => f.id) : publicada[campo];
    if (JSON.stringify(nueva) !== JSON.stringify(vieja)) {
      cambiados.add(campo);
    }
  }
  return cambiados;
}

const NOMBRE_DE_IDIOMA: Readonly<Record<string, ClaveTexto>> = {
  es: 'turista.idioma.es',
  en: 'turista.idioma.en',
};

const vacio = (valor: unknown): boolean =>
  valor === undefined || valor === null || valor === '' || (Array.isArray(valor) && valor.length === 0);

/**
 * El contenido de la versión como una lista de líneas para revisar, en un orden fijo y solo con los campos que trae (los que no
 * corresponden al tipo de oferta se omiten). La zona sale de la lista de zonas; si no se encuentra, se deja su identificador.
 * `publicada` marca qué líneas cambiaron en una edición.
 */
export function filasDeVersion(
  contenido: ContenidoVersion,
  publicada: ContenidoVersion | null | undefined,
  zonas: readonly Zona[],
): FilaVersion[] {
  const cambiados = camposCambiados(contenido, publicada);
  const precio = new PrecioPipe();
  const duracion = new DuracionPipe();

  const lineas: [Campo, ClaveTexto, () => string, boolean?][] = [
    ['nombre', 'admin.moderacion.campo.nombre', () => contenido.nombre],
    ['descripcion', 'admin.moderacion.campo.descripcion', () => contenido.descripcion, true],
    [
      'precioReferencia',
      'admin.moderacion.campo.precio',
      () => precio.transform(contenido.precioReferencia, contenido.unidadPrecio),
    ],
    [
      'zonaId',
      'admin.moderacion.campo.zona',
      () => zonas.find((zona) => zona.id === contenido.zonaId)?.nombre ?? contenido.zonaId,
    ],
    ['capacidadMaxima', 'admin.moderacion.campo.capacidad', () => String(contenido.capacidadMaxima)],
    ['horarios', 'admin.moderacion.campo.horarios', () => contenido.horarios ?? ''],
    ['comoLlegar', 'admin.moderacion.campo.comoLlegar', () => contenido.comoLlegar ?? '', true],
    [
      'ubicacion',
      'admin.moderacion.campo.ubicacion',
      () => `${contenido.ubicacion?.latitud}, ${contenido.ubicacion?.longitud}`,
    ],
    [
      'tipoActividad',
      'admin.moderacion.campo.tipoActividad',
      () => (contenido.tipoActividad ? TIPO_ACTIVIDAD[contenido.tipoActividad] : ''),
    ],
    [
      'nivelEsfuerzo',
      'admin.moderacion.campo.nivelEsfuerzo',
      () => (contenido.nivelEsfuerzo ? NIVEL_ESFUERZO[contenido.nivelEsfuerzo] : ''),
    ],
    [
      'accesibleSillaRuedas',
      'admin.moderacion.campo.accesible',
      () => traducir(contenido.accesibleSillaRuedas ? 'admin.moderacion.si' : 'admin.moderacion.no'),
    ],
    [
      'horariosSalida',
      'admin.moderacion.campo.horariosSalida',
      () => (contenido.horariosSalida ?? []).map((hora) => hora.slice(0, 5)).join(', '),
    ],
    ['puntoEncuentro', 'admin.moderacion.campo.puntoEncuentro', () => contenido.puntoEncuentro ?? ''],
    ['duracionMinutos', 'admin.moderacion.campo.duracion', () => duracion.transform(contenido.duracionMinutos)],
    [
      'modalidad',
      'admin.moderacion.campo.modalidad',
      () => (contenido.modalidad ? MODALIDAD[contenido.modalidad] : ''),
    ],
    [
      'idiomasAtencion',
      'admin.moderacion.campo.idiomas',
      () =>
        (contenido.idiomasAtencion ?? [])
          .map((codigo) => (NOMBRE_DE_IDIOMA[codigo] ? traducir(NOMBRE_DE_IDIOMA[codigo]) : codigo))
          .join(', '),
    ],
    [
      'actividadIds',
      'admin.moderacion.campo.actividades',
      () => traducir('admin.moderacion.actividades', { n: contenido.actividadIds?.length ?? 0 }),
    ],
    ['tipoUnidad', 'admin.moderacion.campo.tipoUnidad', () => contenido.tipoUnidad ?? ''],
  ];

  return lineas
    .filter(([campo]) => !vacio(contenido[campo]))
    .map(([campo, clave, valor, ancho]) => ({
      campo,
      clave,
      valor: valor(),
      ancho: !!ancho,
      cambiado: cambiados.has(campo),
    }));
}
