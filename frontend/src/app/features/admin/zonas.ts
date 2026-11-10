import { Zona, ZonaEdicion, ZonaRequest } from '../../core/api/modelos';

/** Lo que el formulario guarda de una zona. La latitud y la longitud pueden estar vacías mientras se escribe. */
export interface ValoresZona {
  readonly nombre: string;
  readonly descripcion: string;
  readonly latitud: number | null;
  readonly longitud: number | null;
  readonly areaProtegida: boolean;
  readonly altaAfluencia: boolean;
  /** Una buena práctica por línea. */
  readonly buenasPracticas: string;
  readonly condicionesVisita: string;
}

export const VALORES_ZONA_VACIOS: ValoresZona = {
  nombre: '',
  descripcion: '',
  latitud: null,
  longitud: null,
  areaProtegida: false,
  altaAfluencia: false,
  buenasPracticas: '',
  condicionesVisita: '',
};

export type CampoZona = 'nombre' | 'descripcion' | 'latitud' | 'longitud';
export type MotivoZona =
  | 'nombreRequerido'
  | 'descripcionRequerida'
  | 'latitudRequerida'
  | 'latitudRango'
  | 'longitudRequerida'
  | 'longitudRango';

const esNumero = (valor: number | null | undefined): valor is number =>
  typeof valor === 'number' && Number.isFinite(valor);

/**
 * Valida como lo hace el catálogo para avisar antes de enviar: nombre, descripción y ubicación son obligatorios (HU-ADM-06), la
 * latitud va de -90 a 90 y la longitud de -180 a 180. Devuelve el primer motivo de cada campo con error; un objeto vacío significa
 * que se puede enviar. El catálogo vuelve a validar y avisa si el nombre ya existe.
 */
export function validarZona(valores: ValoresZona): Partial<Record<CampoZona, MotivoZona>> {
  const errores: Partial<Record<CampoZona, MotivoZona>> = {};

  if (valores.nombre.trim() === '') {
    errores.nombre = 'nombreRequerido';
  }
  if (valores.descripcion.trim() === '') {
    errores.descripcion = 'descripcionRequerida';
  }
  if (!esNumero(valores.latitud)) {
    errores.latitud = 'latitudRequerida';
  } else if (valores.latitud < -90 || valores.latitud > 90) {
    errores.latitud = 'latitudRango';
  }
  if (!esNumero(valores.longitud)) {
    errores.longitud = 'longitudRequerida';
  } else if (valores.longitud < -180 || valores.longitud > 180) {
    errores.longitud = 'longitudRango';
  }
  return errores;
}

/** Las buenas prácticas del texto del formulario: una por línea, sin espacios en los extremos y sin líneas en blanco. */
export function lineasABuenasPracticas(texto: string): string[] {
  return texto
    .split(/\r?\n/)
    .map((linea) => linea.trim())
    .filter((linea) => linea !== '');
}

/** El texto del formulario para unas buenas prácticas: una por línea. */
export function buenasPracticasALineas(practicas: readonly string[] | null | undefined): string {
  return (practicas ?? []).join('\n');
}

/**
 * El cuerpo de `POST /admin/zonas`. Los textos van sin espacios en los extremos y las condiciones de visita solo si se escribieron.
 * Las marcas se mandan siempre (en false si no están marcadas): editar reemplaza todo lo que no se envía.
 */
export function construirZona(valores: ValoresZona): ZonaRequest {
  const condiciones = valores.condicionesVisita.trim();
  return {
    nombre: valores.nombre.trim(),
    descripcion: valores.descripcion.trim(),
    ubicacion: { latitud: valores.latitud ?? 0, longitud: valores.longitud ?? 0 },
    areaProtegida: valores.areaProtegida,
    altaAfluencia: valores.altaAfluencia,
    buenasPracticas: lineasABuenasPracticas(valores.buenasPracticas),
    ...(condiciones ? { condicionesVisita: condiciones } : {}),
  };
}

/** El cuerpo de `PUT /admin/zonas/{id}`: todos los datos de la zona y si está activa. */
export function construirEdicion(valores: ValoresZona, activa: boolean): ZonaEdicion {
  return { ...construirZona(valores), activa };
}

/** Lo que el formulario muestra al editar una zona. */
export function valoresDeZona(zona: Zona): ValoresZona {
  return {
    nombre: zona.nombre,
    descripcion: zona.descripcion ?? '',
    latitud: zona.ubicacion?.latitud ?? null,
    longitud: zona.ubicacion?.longitud ?? null,
    areaProtegida: zona.areaProtegida,
    altaAfluencia: zona.altaAfluencia,
    buenasPracticas: buenasPracticasALineas(zona.buenasPracticas),
    condicionesVisita: zona.condicionesVisita ?? '',
  };
}

/**
 * El cuerpo para inactivar o reactivar una zona SIN tocar sus datos. Como editar reemplaza todo, hay que mandar la zona completa con
 * `activa` cambiada. Devuelve null si a la zona le falta algún dato obligatorio (descripción o ubicación): en ese caso no se puede
 * reenviar tal cual y hay que completarla editándola.
 */
export function peticionDesdeZona(zona: Zona, activa: boolean): ZonaEdicion | null {
  const valores = valoresDeZona(zona);
  if (Object.keys(validarZona(valores)).length > 0) {
    return null;
  }
  return construirEdicion(valores, activa);
}

// --- La lista ---

export type FiltroZonas = 'todas' | 'activas' | 'inactivas';
export const FILTROS_ZONAS: readonly FiltroZonas[] = ['todas', 'activas', 'inactivas'];

/** El filtro de la dirección (`?estado=inactivas`); un valor desconocido, o ninguno, muestra todas las zonas. */
export function filtroDeZonas(valor: string | null): FiltroZonas {
  return FILTROS_ZONAS.find((filtro) => filtro === valor && filtro !== 'todas') ?? 'todas';
}

/** Las zonas del filtro, en el mismo orden. */
export function filtrarZonas(zonas: readonly Zona[], filtro: FiltroZonas): Zona[] {
  switch (filtro) {
    case 'activas':
      return zonas.filter((zona) => zona.activa);
    case 'inactivas':
      return zonas.filter((zona) => !zona.activa);
    default:
      return [...zonas];
  }
}

/** Las zonas por nombre, sin distinguir mayúsculas ni tildes, como las entrega el catálogo. No modifica la lista. */
export function ordenarZonasPorNombre(zonas: readonly Zona[]): Zona[] {
  return [...zonas].sort((a, b) => a.nombre.localeCompare(b.nombre, 'es', { sensitivity: 'base' }));
}
