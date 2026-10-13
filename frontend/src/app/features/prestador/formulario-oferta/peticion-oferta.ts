import {
  Coordenada,
  FichaOferta,
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
 */
export function aPeticion(valores: ValoresOferta, ubicacion: Coordenada | null): OfertaRequest {
  const horarios = valores.horarios.trim();
  const comoLlegar = valores.comoLlegar.trim();
  const comun: OfertaRequest = {
    tipo: valores.tipo,
    nombre: valores.nombre.trim(),
    descripcion: valores.descripcion.trim(),
    precioReferencia: valores.precioReferencia ?? 0,
    zonaId: valores.zonaId,
    capacidadMaxima: valores.capacidadMaxima ?? 0,
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
 * Valores del formulario a partir de la ficha pública, para editar.
 * [SUPUESTO] La API no devuelve el contenido completo de una oferta propia: se usa su versión publicada.
 */
export function valoresDeFicha(ficha: FichaOferta): { valores: ValoresOferta; ubicacion: Coordenada | null } {
  const horas = ficha.actividad?.horariosSalida ?? ficha.paquete?.horariosSalida ?? [];
  return {
    valores: {
      tipo: ficha.tipo,
      nombre: ficha.nombre,
      descripcion: ficha.descripcion,
      precioReferencia: ficha.precioReferencia,
      horarios: ficha.horarios ?? '',
      zonaId: ficha.zona?.id ?? '',
      comoLlegar: ficha.comoLlegar ?? '',
      capacidadMaxima: ficha.capacidadMaxima,
      accesibleSillaRuedas: ficha.tipo !== 'PAQUETE' && ficha.accesibleSillaRuedas,
      tipoActividad: ficha.actividad?.tipoActividad ?? '',
      nivelEsfuerzo: ficha.actividad?.nivelEsfuerzo ?? '',
      // La API puede enviar la hora con segundos; el formulario usa HH:mm.
      horariosSalida: horas.map((hora) => hora.slice(0, 5)).join(', '),
      puntoEncuentro: ficha.actividad?.puntoEncuentro ?? '',
      duracionMinutos: ficha.actividad?.duracionMinutos ?? ficha.paquete?.duracionMinutos ?? null,
      modalidad: ficha.actividad?.modalidad ?? '',
      idiomasAtencion: (ficha.actividad?.idiomasAtencion ?? []).join(', '),
      actividadIds: ficha.paquete?.actividades.map((actividad) => actividad.id) ?? [],
      tipoUnidad: ficha.alojamiento?.tipoUnidad ?? '',
    },
    ubicacion: ficha.ubicacion ?? null,
  };
}
