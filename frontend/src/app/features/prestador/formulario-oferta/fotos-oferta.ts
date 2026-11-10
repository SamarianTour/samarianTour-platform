import { Foto } from '../../../core/api/modelos';
import type { ClaveTexto } from '../../../core/idioma/textos';
import { traducir } from '../../../core/idioma/traducir';

/** Una oferta lleva de 1 a 6 fotos (DC-47). */
export const MINIMO_FOTOS = 1;
export const MAXIMO_FOTOS = 6;

/** Una foto pesa como máximo 5 MB (DC-39). */
export const MAXIMO_BYTES = 5 * 1024 * 1024;

/** Formatos que acepta la API (DC-39). */
export const TIPOS_FOTO = ['image/jpeg', 'image/png', 'image/webp'];

/** Lo único que se mira de un archivo; así las pruebas no necesitan un File de verdad. */
export type DatosArchivo = Pick<File, 'name' | 'size' | 'type'>;

export interface ClasificacionArchivos<T extends DatosArchivo> {
  readonly aceptados: T[];
  /** Un aviso por cada archivo que no se sube, con su nombre y el motivo. */
  readonly avisos: string[];
}

/**
 * Motivo por el que un archivo no sirve, o null si puede subirse. Es solo una ayuda para avisar antes de enviar:
 * la API vuelve a validar y reconoce el formato por el contenido, no por lo que diga el navegador.
 */
export function motivoDeRechazo(archivo: DatosArchivo): string | null {
  // Algunos sistemas no informan el tipo: en ese caso decide la API.
  if (archivo.type !== '' && !TIPOS_FOTO.includes(archivo.type)) {
    return traducir('prestador.fotos.motivoFormato');
  }
  if (archivo.size === 0) {
    return traducir('prestador.fotos.motivoVacio');
  }
  if (archivo.size > MAXIMO_BYTES) {
    return traducir('prestador.fotos.motivoPesa');
  }
  return null;
}

/**
 * Los textos del selector que dependen de de qué son las fotos ("la oferta tiene 3 de 6 fotos", "el perfil tiene…"). El selector lo
 * usan las ofertas y el perfil público; los demás textos son los mismos.
 */
export interface TextosFotos {
  readonly leyenda: ClaveTexto;
  readonly ayuda: ClaveTexto;
  readonly alt: ClaveTexto;
  readonly altPortada: ClaveTexto;
  readonly agregada: ClaveTexto;
  readonly quitada: ClaveTexto;
  readonly quitadaSinFotos: ClaveTexto;
  readonly motivoMaximo: ClaveTexto;
}

export const TEXTOS_FOTOS_OFERTA: TextosFotos = {
  leyenda: 'prestador.fotos.leyenda',
  ayuda: 'prestador.fotos.ayuda',
  alt: 'prestador.fotos.alt',
  altPortada: 'prestador.fotos.altPortada',
  agregada: 'prestador.fotos.agregada',
  quitada: 'prestador.fotos.quitada',
  quitadaSinFotos: 'prestador.fotos.quitadaSinFotos',
  motivoMaximo: 'prestador.fotos.motivoMaximo',
};

/** Cuántas fotos más caben en la oferta. */
export function cuposLibres(cantidad: number): number {
  return Math.max(0, MAXIMO_FOTOS - cantidad);
}

/**
 * Separa lo que el prestador eligió en lo que se sube y lo que se descarta. Respeta el orden en que eligió los
 * archivos y, si no caben todos, deja fuera los últimos.
 */
export function clasificarArchivos<T extends DatosArchivo>(
  archivos: readonly T[],
  cupos: number,
  motivoMaximo: ClaveTexto = 'prestador.fotos.motivoMaximo',
): ClasificacionArchivos<T> {
  const aceptados: T[] = [];
  const avisos: string[] = [];
  for (const archivo of archivos) {
    const motivo = motivoDeRechazo(archivo);
    if (motivo) {
      avisos.push(traducir('prestador.fotos.noSubido', { nombre: archivo.name, motivo }));
    } else if (aceptados.length >= cupos) {
      avisos.push(avisoDeMaximo(archivo.name, motivoMaximo));
    } else {
      aceptados.push(archivo);
    }
  }
  return { aceptados, avisos };
}

/** "No se subió «x»: una oferta lleva máximo 6 fotos." en el idioma vigente. */
export function avisoDeMaximo(nombre: string, motivoMaximo: ClaveTexto = 'prestador.fotos.motivoMaximo'): string {
  return traducir('prestador.fotos.noSubido', {
    nombre,
    motivo: traducir(motivoMaximo, { maximo: MAXIMO_FOTOS }),
  });
}

/** Agrega una foto al final. Si ya está o la lista está llena, devuelve la lista como estaba. */
export function agregar(fotos: readonly Foto[], nueva: Foto): Foto[] {
  if (fotos.length >= MAXIMO_FOTOS || fotos.some((foto) => foto.id === nueva.id)) {
    return [...fotos];
  }
  return [...fotos, nueva];
}

export function quitar(fotos: readonly Foto[], id: string): Foto[] {
  return fotos.filter((foto) => foto.id !== id);
}

/** Mueve una foto un lugar hacia el principio (-1) o hacia el final (1). En los extremos no cambia nada. */
export function mover(fotos: readonly Foto[], id: string, desplazamiento: -1 | 1): Foto[] {
  const origen = fotos.findIndex((foto) => foto.id === id);
  const destino = origen + desplazamiento;
  const copia = [...fotos];
  if (origen === -1 || destino < 0 || destino >= copia.length) {
    return copia;
  }
  [copia[origen], copia[destino]] = [copia[destino], copia[origen]];
  return copia;
}
