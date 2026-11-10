import { PerfilPublico, PerfilRequest } from '../../../core/api/modelos';

/** Lo que el formulario del perfil tiene escrito. Las redes sociales van una por línea. */
export interface ValoresPerfil {
  readonly nombreComercial: string;
  readonly descripcion: string;
  readonly horariosAtencion: string;
  readonly sitioWeb: string;
  readonly redesSociales: string;
}

export const VALORES_PERFIL_VACIOS: ValoresPerfil = {
  nombreComercial: '',
  descripcion: '',
  horariosAtencion: '',
  sitioWeb: '',
  redesSociales: '',
};

export type CampoPerfil = 'nombreComercial' | 'descripcion' | 'horariosAtencion' | 'sitioWeb' | 'redesSociales';
export type MotivoPerfil = 'descripcionRequerida' | 'sitioInvalido' | 'redesInvalidas';

/** Los campos en el orden de la pantalla: el primero con error recibe el foco. */
export const CAMPOS_PERFIL_EN_ORDEN: readonly CampoPerfil[] = [
  'nombreComercial',
  'descripcion',
  'horariosAtencion',
  'sitioWeb',
  'redesSociales',
];

/** Si es una dirección web con `http` o `https` y un servidor (el contrato pide una URL http o https). */
export function esUrlWeb(texto: string): boolean {
  try {
    const url = new URL(texto);
    return (url.protocol === 'http:' || url.protocol === 'https:') && url.hostname !== '';
  } catch {
    return false;
  }
}

/** Parte un texto en líneas y descarta los espacios y las líneas vacías. */
export function lineas(texto: string): string[] {
  return texto
    .split(/\r?\n/)
    .map((linea) => linea.trim())
    .filter((linea) => linea !== '');
}

/** Solo la descripción es obligatoria (DC-26); el sitio web y las redes, si se escriben, deben ser direcciones http o https. */
export function validarPerfil(valores: ValoresPerfil): Partial<Record<CampoPerfil, MotivoPerfil>> {
  const errores: Partial<Record<CampoPerfil, MotivoPerfil>> = {};
  if (valores.descripcion.trim() === '') {
    errores.descripcion = 'descripcionRequerida';
  }
  const sitio = valores.sitioWeb.trim();
  if (sitio !== '' && !esUrlWeb(sitio)) {
    errores.sitioWeb = 'sitioInvalido';
  }
  if (!lineas(valores.redesSociales).every(esUrlWeb)) {
    errores.redesSociales = 'redesInvalidas';
  }
  return errores;
}

/**
 * El cuerpo de `PUT /prestador/perfil`. Guardar reemplaza los datos del perfil, así que lo que se deja en blanco se omite y queda
 * vacío. Las fotos no viajan aquí: se suben y se quitan aparte y el perfil las conserva.
 */
export function construirPerfil(valores: ValoresPerfil): PerfilRequest {
  const nombre = valores.nombreComercial.trim();
  const horarios = valores.horariosAtencion.trim();
  const sitio = valores.sitioWeb.trim();
  return {
    ...(nombre && { nombreComercial: nombre }),
    descripcion: valores.descripcion.trim(),
    ...(horarios && { horariosAtencion: horarios }),
    ...(sitio && { sitioWeb: sitio }),
    redesSociales: lineas(valores.redesSociales),
  };
}

/** Lo que el formulario muestra con el perfil que ya tiene el prestador. */
export function valoresDePerfil(perfil: PerfilPublico): ValoresPerfil {
  return {
    nombreComercial: perfil.nombreComercial ?? '',
    descripcion: perfil.descripcion,
    horariosAtencion: perfil.horariosAtencion ?? '',
    sitioWeb: perfil.sitioWeb ?? '',
    redesSociales: (perfil.redesSociales ?? []).join('\n'),
  };
}
