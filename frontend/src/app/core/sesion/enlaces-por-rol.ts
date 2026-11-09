import { Rol } from '../api/modelos-nucleo';

/** Un enlace de la navegación de la cabecera. */
export interface EnlaceNavegacion {
  readonly texto: string;
  readonly ruta: string;
  readonly consulta?: Readonly<Record<string, string>>;
  /** Direcciones que marcan el enlace como la sección actual: iguales a una de estas o dentro de ellas. */
  readonly secciones: readonly string[];
}

const CATALOGO: EnlaceNavegacion = { texto: 'Catálogo', ruta: '/', secciones: ['/', '/ofertas'] };

/** Lo que ve quien no ha iniciado sesión. "Soy prestador" lleva a registrarse como prestador. */
const SIN_SESION: readonly EnlaceNavegacion[] = [
  CATALOGO,
  { texto: 'Soy prestador', ruta: '/registro', consulta: { tipo: 'prestador' }, secciones: [] },
];

/**
 * Las secciones de cada rol. El turista y el prestador además exploran el catálogo; los roles internos (operador,
 * gestor, administrador) trabajan solo en su sección.
 */
const POR_ROL: Record<Rol, readonly EnlaceNavegacion[]> = {
  TURISTA: [
    CATALOGO,
    { texto: 'Mis solicitudes', ruta: '/turista/solicitudes', secciones: ['/turista'] },
    { texto: 'Mis opiniones', ruta: '/mis-opiniones', secciones: ['/mis-opiniones'] },
  ],
  PRESTADOR: [
    CATALOGO,
    { texto: 'Mis ofertas', ruta: '/prestador/ofertas', secciones: ['/prestador'] },
  ],
  OPERADOR_DISPONIBILIDAD: [
    { texto: 'Cola de solicitudes', ruta: '/operador', secciones: ['/operador'] },
  ],
  GESTOR_DESTINO: [{ texto: 'Indicadores', ruta: '/gestor', secciones: ['/gestor'] }],
  ADMINISTRADOR: [{ texto: 'Administración', ruta: '/admin', secciones: ['/admin'] }],
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
