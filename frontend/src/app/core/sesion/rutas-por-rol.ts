import { Rol } from '../api/modelos-nucleo';

/** La pantalla a la que va cada rol después de iniciar sesión. */
export const INICIO_POR_ROL: Record<Rol, string> = {
  TURISTA: '/',
  PRESTADOR: '/prestador/ofertas',
  OPERADOR_DISPONIBILIDAD: '/operador',
  GESTOR_DESTINO: '/gestor',
  ADMINISTRADOR: '/admin',
};

/** Las secciones que solo puede ver un rol. Todo lo demás (el catálogo, la ficha, las cuentas) es de cualquiera. */
const SECCIONES_POR_ROL: readonly { readonly prefijo: string; readonly rol: Rol }[] = [
  { prefijo: '/prestador', rol: 'PRESTADOR' },
  { prefijo: '/turista', rol: 'TURISTA' },
  { prefijo: '/mis-opiniones', rol: 'TURISTA' },
  { prefijo: '/operador', rol: 'OPERADOR_DISPONIBILIDAD' },
  { prefijo: '/gestor', rol: 'GESTOR_DESTINO' },
  { prefijo: '/admin', rol: 'ADMINISTRADOR' },
];

/** Pantallas de acceso: volver a ellas después de entrar dejaría al usuario donde no quiere estar. */
const PANTALLAS_DE_ACCESO = [
  '/iniciar-sesion',
  '/registro',
  '/recuperar-contrasena',
  '/restablecer-contrasena',
];

const ORIGEN_FALSO = 'http://interno.invalido';

function rutaSinConsulta(url: string): string {
  return url.split(/[?#]/)[0];
}

function empiezaEn(url: string, prefijo: string): boolean {
  const ruta = rutaSinConsulta(url);
  return ruta === prefijo || ruta.startsWith(`${prefijo}/`);
}

/** El rol que exige una dirección, o null si es de cualquiera. */
export function rolQueExige(url: string): Rol | null {
  return SECCIONES_POR_ROL.find(({ prefijo }) => empiezaEn(url, prefijo))?.rol ?? null;
}

/**
 * Una dirección a la que es seguro volver después de iniciar sesión: solo rutas de esta misma aplicación. Rechaza
 * direcciones de otros sitios (`https://…`, `//…`, `/\…`, `javascript:…`) para que un enlace de la forma
 * `/iniciar-sesion?volver=https://otro.sitio` no pueda mandar al usuario fuera, y las pantallas de acceso.
 */
export function rutaSegura(url: string | null | undefined): string | null {
  if (typeof url !== 'string' || url.length === 0 || url.length > 500) {
    return null;
  }
  if (!url.startsWith('/') || url.startsWith('//') || /[\\\u0000-\u001f]/.test(url)) {
    return null;
  }
  try {
    if (new URL(url, ORIGEN_FALSO).origin !== ORIGEN_FALSO) {
      return null;
    }
  } catch {
    return null;
  }
  return PANTALLAS_DE_ACCESO.some((pantalla) => empiezaEn(url, pantalla)) ? null : url;
}

/**
 * A dónde ir después de iniciar sesión: a la página que se quería ver, si es segura y le corresponde a su rol; si no,
 * al inicio de su rol. Volver a una sección de otro rol solo mostraría "no tienes permiso".
 */
export function destinoTrasIniciarSesion(volver: string | null | undefined, rol: Rol): string {
  const ruta = rutaSegura(volver);
  if (ruta) {
    const exige = rolQueExige(ruta);
    if (exige === null || exige === rol) {
      return ruta;
    }
  }
  return INICIO_POR_ROL[rol];
}
