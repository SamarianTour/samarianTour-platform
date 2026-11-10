import { TipoOferta } from '../../../core/api/modelos';
import { EnviarSolicitudRequest } from '../../../core/api/modelos-nucleo';

/** Lo que el turista escribe en el formulario. Los nombres son los del contrato (EnviarSolicitudRequest). */
export interface ValoresSolicitud {
  /** AAAA-MM-DD. */
  fechaInicio: string;
  /** HH:mm. Solo actividad y paquete. */
  horaSalida: string;
  /** HH:mm. Solo restaurante. */
  horaReserva: string;
  /** AAAA-MM-DD. Solo alojamiento. */
  fechaSalida: string;
  /** Solo alojamiento. */
  numeroUnidades: number | null;
  personas: number | null;
  nota: string;
}

export const VALORES_VACIOS: ValoresSolicitud = {
  fechaInicio: '',
  horaSalida: '',
  horaReserva: '',
  fechaSalida: '',
  numeroUnidades: null,
  personas: null,
  nota: '',
};

export type CampoSolicitud = keyof ValoresSolicitud;

/** Por qué un campo no es válido. El texto de cada motivo lo pone la pantalla, en el idioma vigente. */
export type MotivoDeError =
  | 'requerido'
  | 'formato'
  /** La fecha es anterior al mínimo: la solicitud se hace con 2 días de anticipación (RF-30). */
  | 'pasada'
  /** La fecha de salida no es posterior a la de entrada. */
  | 'antesDeInicio'
  /** La hora de salida no es una de las que ofrece la oferta. */
  | 'noOfrecida'
  | 'entero'
  | 'minimo'
  /** Más personas que la capacidad máxima de la oferta. */
  | 'maximo'
  | 'largo';

export type ErroresSolicitud = Partial<Record<CampoSolicitud, MotivoDeError>>;

/** Lo que se necesita saber de la oferta y del momento para validar. */
export interface ContextoSolicitud {
  readonly tipo: TipoOferta;
  readonly capacidadMaxima: number;
  /** Las horas de salida de la oferta (actividad y paquete). */
  readonly horariosSalida: readonly string[];
  /** El momento del envío, en milisegundos. */
  readonly ahora: number;
}

export const NOTA_MAXIMA = 500;
/** La fecha de inicio debe ser al menos 2 días calendario después del envío (RF-30). */
export const DIAS_DE_ANTICIPACION = 2;

const FECHA = /^\d{4}-\d{2}-\d{2}$/;
const HORA = /^([01]\d|2[0-3]):[0-5]\d$/;

/** Los campos que cada tipo de oferta pide además de la fecha de inicio, las personas y la nota (RF-30). */
const CAMPOS_DEL_TIPO: Readonly<Record<TipoOferta, readonly CampoSolicitud[]>> = {
  ACTIVIDAD: ['horaSalida'],
  PAQUETE: ['horaSalida'],
  ALOJAMIENTO: ['fechaSalida', 'numeroUnidades'],
  RESTAURANTE: ['horaReserva'],
};

/** Los campos que el formulario muestra para un tipo de oferta, en el orden en que se llenan. */
export function camposDe(tipo: TipoOferta): readonly CampoSolicitud[] {
  return ['fechaInicio', ...CAMPOS_DEL_TIPO[tipo], 'personas', 'nota'];
}

/** AAAA-MM-DD de un momento, en la hora de Colombia (donde ocurre el servicio y donde el núcleo cuenta los días). */
export function fechaEnColombia(momento: number): string {
  return new Intl.DateTimeFormat('en-CA', {
    timeZone: 'America/Bogota',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(new Date(momento));
}

/** Suma días a una fecha AAAA-MM-DD, sin depender de la zona horaria del navegador. */
export function sumarDias(fecha: string, dias: number): string {
  const [anio, mes, dia] = fecha.split('-').map(Number);
  return new Date(Date.UTC(anio, mes - 1, dia + dias)).toISOString().slice(0, 10);
}

/** La primera fecha que se puede pedir: 2 días calendario después de hoy, en la hora de Colombia. */
export function fechaMinima(ahora: number): string {
  return sumarDias(fechaEnColombia(ahora), DIAS_DE_ANTICIPACION);
}

/** Las noches entre la entrada y la salida de un alojamiento; 0 si alguna fecha no es válida o no hay noches. */
export function nochesEntre(inicio: string, salida: string): number {
  if (!esFechaValida(inicio) || !esFechaValida(salida)) {
    return 0;
  }
  const dias = (Date.parse(salida) - Date.parse(inicio)) / 86_400_000;
  return Math.max(0, Math.round(dias));
}

/** Una fecha AAAA-MM-DD que existe en el calendario (rechaza, por ejemplo, el 31 de febrero). */
export function esFechaValida(fecha: string): boolean {
  if (!FECHA.test(fecha)) {
    return false;
  }
  const [anio, mes, dia] = fecha.split('-').map(Number);
  const real = new Date(Date.UTC(anio, mes - 1, dia));
  return (
    real.getUTCFullYear() === anio && real.getUTCMonth() === mes - 1 && real.getUTCDate() === dia
  );
}

/** Las horas que manda la API pueden traer segundos ("07:00:00"); el formulario usa HH:mm. */
export function normalizarHora(hora: string): string {
  return hora.slice(0, 5);
}

/**
 * Valida los valores como lo hace el núcleo (RF-30, RF-31) para avisar antes de enviar. Devuelve el primer motivo de cada
 * campo con error; un objeto vacío significa que se puede enviar. El núcleo vuelve a validar y manda sus errores.
 */
export function validarSolicitud(
  valores: ValoresSolicitud,
  contexto: ContextoSolicitud,
): ErroresSolicitud {
  const errores: { -readonly [C in CampoSolicitud]?: MotivoDeError } = {};
  const campos = camposDe(contexto.tipo);
  const minima = fechaMinima(contexto.ahora);

  if (campos.includes('fechaInicio')) {
    if (!valores.fechaInicio) {
      errores.fechaInicio = 'requerido';
    } else if (!esFechaValida(valores.fechaInicio)) {
      errores.fechaInicio = 'formato';
    } else if (valores.fechaInicio < minima) {
      errores.fechaInicio = 'pasada';
    }
  }

  if (campos.includes('horaSalida')) {
    const hora = valores.horaSalida;
    const ofrecidas = contexto.horariosSalida.map(normalizarHora);
    if (!hora) {
      errores.horaSalida = 'requerido';
    } else if (!HORA.test(hora)) {
      errores.horaSalida = 'formato';
    } else if (ofrecidas.length > 0 && !ofrecidas.includes(hora)) {
      errores.horaSalida = 'noOfrecida';
    }
  }

  if (campos.includes('horaReserva')) {
    if (!valores.horaReserva) {
      errores.horaReserva = 'requerido';
    } else if (!HORA.test(valores.horaReserva)) {
      errores.horaReserva = 'formato';
    }
  }

  if (campos.includes('fechaSalida')) {
    if (!valores.fechaSalida) {
      errores.fechaSalida = 'requerido';
    } else if (!esFechaValida(valores.fechaSalida)) {
      errores.fechaSalida = 'formato';
    } else if (esFechaValida(valores.fechaInicio) && valores.fechaSalida <= valores.fechaInicio) {
      errores.fechaSalida = 'antesDeInicio';
    }
  }

  if (campos.includes('numeroUnidades')) {
    const motivo = motivoDeCantidad(valores.numeroUnidades);
    if (motivo) {
      errores.numeroUnidades = motivo;
    }
  }

  const motivoPersonas = motivoDeCantidad(valores.personas);
  if (motivoPersonas) {
    errores.personas = motivoPersonas;
  } else if ((valores.personas ?? 0) > contexto.capacidadMaxima) {
    errores.personas = 'maximo';
  }

  if (valores.nota.trim().length > NOTA_MAXIMA) {
    errores.nota = 'largo';
  }
  return errores;
}

function motivoDeCantidad(valor: number | null): MotivoDeError | null {
  if (valor === null || Number.isNaN(valor)) {
    return 'requerido';
  }
  if (!Number.isInteger(valor)) {
    return 'entero';
  }
  return valor < 1 ? 'minimo' : null;
}

/**
 * Arma el cuerpo de POST /turista/solicitudes. Solo viajan los campos del tipo de oferta: el núcleo rechaza uno que no le
 * corresponde (400), así que lo escrito para otro tipo no sale. La nota viaja solo si tiene texto.
 */
export function aPeticion(
  valores: ValoresSolicitud,
  ofertaId: string,
  tipo: TipoOferta,
): EnviarSolicitudRequest {
  const nota = valores.nota.trim();
  const peticion: EnviarSolicitudRequest = {
    ofertaId,
    fechaInicio: valores.fechaInicio,
    personas: valores.personas ?? 0,
    ...(nota && { nota }),
  };
  switch (tipo) {
    case 'ACTIVIDAD':
    case 'PAQUETE':
      return { ...peticion, horaSalida: valores.horaSalida };
    case 'ALOJAMIENTO':
      return {
        ...peticion,
        fechaSalida: valores.fechaSalida,
        numeroUnidades: valores.numeroUnidades ?? 0,
      };
    case 'RESTAURANTE':
      return { ...peticion, horaReserva: valores.horaReserva };
  }
}

/**
 * El precio de referencia de lo pedido. [SUPUESTO] La oferta trae un precio de referencia con su unidad (DC-22): por persona
 * en actividad, paquete y restaurante, y por noche y unidad en alojamiento. El total es informativo: lo confirma el operador.
 * Devuelve null si faltan datos para calcularlo.
 */
export function precioDeReferencia(
  tipo: TipoOferta,
  precioUnitario: number,
  valores: Pick<ValoresSolicitud, 'personas' | 'fechaInicio' | 'fechaSalida' | 'numeroUnidades'>,
): number | null {
  if (tipo === 'ALOJAMIENTO') {
    const noches = nochesEntre(valores.fechaInicio, valores.fechaSalida);
    const unidades = valores.numeroUnidades ?? 0;
    return noches > 0 && unidades > 0 ? precioUnitario * noches * unidades : null;
  }
  const personas = valores.personas ?? 0;
  return personas > 0 ? precioUnitario * personas : null;
}
