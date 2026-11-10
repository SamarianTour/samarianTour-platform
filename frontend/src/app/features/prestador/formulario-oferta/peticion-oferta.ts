import {
  ContenidoVersion,
  Coordenada,
  Foto,
  ModalidadRecorrido,
  NivelEsfuerzo,
  OfertaRequest,
  TipoActividad,
  TipoOferta,
} from '../../../core/api/modelos';

/** Lo que el prestador escribe en el formulario. Los nombres son los del contrato (OfertaRequest). */
export interface ValoresOferta {
  tipo: TipoOferta;
  nombre: string;
  descripcion: string;
  precioReferencia: number | null;
  horarios: string;
  zonaId: string;
  comoLlegar: string;
  capacidadMaxima: number | null;
  accesibleSillaRuedas: boolean;
  tipoActividad: TipoActividad | '';
  nivelEsfuerzo: NivelEsfuerzo | '';
  /** Horas separadas por comas: "07:30, 14:00". */
  horariosSalida: string;
  puntoEncuentro: string;
  duracionMinutos: number | null;
  modalidad: ModalidadRecorrido | '';
  /** Códigos de idioma separados por comas: "es, en". */
  idiomasAtencion: string;
  actividadIds: string[];
  tipoUnidad: string;
}

/** Parte un texto separado por comas y descarta los espacios y los elementos vacíos. */
export function aLista(texto: string): string[] {
  return texto
    .split(',')
    .map((parte) => parte.trim())
    .filter((parte) => parte !== '');
}

/**
 * Arma el cuerpo de POST y PUT /prestador/ofertas. Solo viajan los campos del tipo elegido y los opcionales
 * que tienen valor; así lo que se escribió para otro tipo no llega a la API.
 *
 * @param fotos identificadores de las fotos de la oferta (DC-47)
 */
export function aPeticion(valores: ValoresOferta, ubicacion: Coordenada | null, fotos: string[]): OfertaRequest {
  const horarios = valores.horarios.trim();
  const comoLlegar = valores.comoLlegar.trim();
  const comun: OfertaRequest = {
    tipo: valores.tipo,
    nombre: valores.nombre.trim(),
    descripcion: valores.descripcion.trim(),
    precioReferencia: valores.precioReferencia ?? 0,
    zonaId: valores.zonaId,
    capacidadMaxima: valores.capacidadMaxima ?? 0,
    fotos,
    // La accesibilidad de un paquete no se envía: sale de sus actividades (RF-17).
    accesibleSillaRuedas: valores.tipo !== 'PAQUETE' && valores.accesibleSillaRuedas,
    ...(horarios && { horarios }),
    ...(ubicacion && { ubicacion }),
    ...(comoLlegar && { comoLlegar }),
  };

  switch (valores.tipo) {
    case 'ACTIVIDAD': {
      const puntoEncuentro = valores.puntoEncuentro.trim();
      const idiomasAtencion = aLista(valores.idiomasAtencion);
      return {
        ...comun,
        ...(valores.tipoActividad && { tipoActividad: valores.tipoActividad }),
        ...(valores.nivelEsfuerzo && { nivelEsfuerzo: valores.nivelEsfuerzo }),
        horariosSalida: aLista(valores.horariosSalida),
        ...(puntoEncuentro && { puntoEncuentro }),
        ...(valores.duracionMinutos !== null && { duracionMinutos: valores.duracionMinutos }),
        ...(valores.modalidad && { modalidad: valores.modalidad }),
        ...(idiomasAtencion.length > 0 && { idiomasAtencion }),
      };
    }
    case 'PAQUETE':
      return {
        ...comun,
        actividadIds: valores.actividadIds,
        horariosSalida: aLista(valores.horariosSalida),
        ...(valores.duracionMinutos !== null && { duracionMinutos: valores.duracionMinutos }),
      };
    case 'ALOJAMIENTO': {
      const tipoUnidad = valores.tipoUnidad.trim();
      return { ...comun, ...(tipoUnidad && { tipoUnidad }) };
    }
    case 'RESTAURANTE':
      return comun;
  }
}

/**
 * Valores del formulario a partir del contenido de una versión de la oferta propia (DC-50), para editarla. El contenido trae
 * todos los campos que envió el prestador, también en una versión que aún no está publicada o que fue rechazada.
 * Las fotos salen completas (con su miniatura) para mostrarlas en el selector; a la API solo viajan sus ids.
 */
export function valoresDeContenido(
  tipo: TipoOferta,
  contenido: ContenidoVersion,
): {
  valores: ValoresOferta;
  ubicacion: Coordenada | null;
  fotos: Foto[];
} {
  return {
    valores: {
      tipo,
      nombre: contenido.nombre,
      descripcion: contenido.descripcion,
      precioReferencia: contenido.precioReferencia,
      horarios: contenido.horarios ?? '',
      zonaId: contenido.zonaId,
      comoLlegar: contenido.comoLlegar ?? '',
      capacidadMaxima: contenido.capacidadMaxima,
      // La accesibilidad de un paquete se calcula con sus actividades: no se escribe.
      accesibleSillaRuedas: tipo !== 'PAQUETE' && (contenido.accesibleSillaRuedas ?? false),
      tipoActividad: contenido.tipoActividad ?? '',
      nivelEsfuerzo: contenido.nivelEsfuerzo ?? '',
      // La API puede enviar la hora con segundos; el formulario usa HH:mm.
      horariosSalida: (contenido.horariosSalida ?? []).map((hora) => hora.slice(0, 5)).join(', '),
      puntoEncuentro: contenido.puntoEncuentro ?? '',
      duracionMinutos: contenido.duracionMinutos ?? null,
      modalidad: contenido.modalidad ?? '',
      idiomasAtencion: (contenido.idiomasAtencion ?? []).join(', '),
      actividadIds: [...(contenido.actividadIds ?? [])],
      tipoUnidad: contenido.tipoUnidad ?? '',
    },
    ubicacion: contenido.ubicacion ?? null,
    fotos: [...contenido.fotos],
  };
}
