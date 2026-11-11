import { Rol } from '../api/modelos-nucleo';
import type { ClaveTexto } from '../idioma/textos';

/** Un enlace de la navegación de la cabecera. */
export interface EnlaceNavegacion {
  /** La clave del texto del enlace; la plantilla lo muestra con el pipe `t`, así que sigue el idioma. */
  readonly clave: ClaveTexto;
  readonly ruta: string;
  readonly consulta?: Readonly<Record<string, string>>;
  /** Direcciones que marcan el enlace como la sección actual: iguales a una de estas o dentro de ellas. */
  readonly secciones: readonly string[];
}

const CATALOGO: EnlaceNavegacion = {
  clave: 'app.nav.catalogo',
  ruta: '/catalogo',
  secciones: ['/catalogo', '/ofertas'],
};

/**
 * P08.1. Las alertas del destino (`/alertas`) no tienen enlace propio en el menú: se llega a ellas desde Recomendaciones y desde la
 * sección de alertas de la ficha de una oferta, así que esa sección también marca este enlace.
 */
const RECOMENDACIONES: EnlaceNavegacion = {
  clave: 'app.nav.recomendaciones',
  ruta: '/recomendaciones',
  secciones: ['/recomendaciones', '/alertas'],
};

/** Lo que ve quien no ha iniciado sesión. "Soy prestador" lleva a registrarse como prestador. */
const SIN_SESION: readonly EnlaceNavegacion[] = [
  CATALOGO,
  RECOMENDACIONES,
  {
    clave: 'app.nav.soyPrestador',
    ruta: '/registro',
    consulta: { tipo: 'prestador' },
    secciones: [],
  },
];

/**
 * Las secciones de cada rol. El turista y el prestador además exploran el catálogo; los roles internos (operador,
 * gestor, administrador) trabajan solo en su sección.
 */
const POR_ROL: Record<Rol, readonly EnlaceNavegacion[]> = {
  TURISTA: [
    CATALOGO,
    RECOMENDACIONES,
    { clave: 'app.nav.misSolicitudes', ruta: '/turista/solicitudes', secciones: ['/turista'] },
    { clave: 'app.nav.misOpiniones', ruta: '/mis-opiniones', secciones: ['/mis-opiniones'] },
  ],
  PRESTADOR: [
    CATALOGO,
    RECOMENDACIONES,
    { clave: 'app.nav.misOfertas', ruta: '/prestador/ofertas', secciones: ['/prestador'] },
  ],
  OPERADOR_DISPONIBILIDAD: [{ clave: 'app.nav.cola', ruta: '/operador', secciones: ['/operador'] }],
  GESTOR_DESTINO: [{ clave: 'app.nav.indicadores', ruta: '/gestor', secciones: ['/gestor'] }],
  ADMINISTRADOR: [{ clave: 'app.nav.administracion', ruta: '/admin', secciones: ['/admin'] }],
};

/** Los enlaces de la cabecera para el rol dado, o para quien no tiene sesión (null). */
export function enlacesDelRol(rol: Rol | null): readonly EnlaceNavegacion[] {
  return rol ? POR_ROL[rol] : SIN_SESION;
}

/** Si la dirección actual está dentro de la sección del enlace. La raíz solo coincide consigo misma. */
export function enlaceActivo(enlace: EnlaceNavegacion, url: string): boolean {
  const ruta = url.split(/[?#]/)[0];
  return enlace.secciones.some((seccion) =>
    seccion === '/' ? ruta === '/' : ruta === seccion || ruta.startsWith(`${seccion}/`),
  );
}
