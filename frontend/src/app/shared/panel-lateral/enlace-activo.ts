import type { ClaveTexto } from '../../core/idioma/textos';
import type { NombreIcono } from '../icono/icono';

/** Un enlace del menú lateral de un panel. */
export interface EnlacePanel {
  /** La clave del texto del enlace; la plantilla lo muestra con el pipe `t`, así que sigue el idioma. */
  readonly clave: ClaveTexto;
  readonly ruta: string;
  readonly icono: NombreIcono;
  /** Solo se marca en esa dirección exacta: el inicio del panel, que si no quedaría marcado en todas sus pantallas. */
  readonly exacto?: boolean;
}

/** Si la dirección actual es la del enlace o está dentro de ella. La dirección se compara sin la consulta ni el fragmento. */
export function enlacePanelActivo(enlace: Pick<EnlacePanel, 'ruta' | 'exacto'>, url: string): boolean {
  const ruta = url.split(/[?#]/)[0];
  return enlace.exacto
    ? ruta === enlace.ruta
    : ruta === enlace.ruta || ruta.startsWith(`${enlace.ruta}/`);
}
