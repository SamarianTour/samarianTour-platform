import { Rol } from '../api/modelos-nucleo';
import { establecerIdioma } from '../idioma/idioma';
import { traducir } from '../idioma/traducir';
import { EnlaceNavegacion, enlaceActivo, enlacesDelRol } from './enlaces-por-rol';
import { INICIO_POR_ROL, rolQueExige } from './rutas-por-rol';

const ROLES: Rol[] = [
  'TURISTA',
  'PRESTADOR',
  'OPERADOR_DISPONIBILIDAD',
  'GESTOR_DESTINO',
  'ADMINISTRADOR',
];

/** Los textos de los enlaces tal como los ve quien tiene la interfaz en español. */
const textos = (rol: Rol | null) => enlacesDelRol(rol).map((enlace) => traducir(enlace.clave));

describe('enlacesDelRol', () => {
  it('sin sesión ofrece el catálogo y el camino para registrarse como prestador', () => {
    expect(textos(null)).toEqual(['Catálogo', 'Recomendaciones', 'Soy prestador']);
    expect(enlacesDelRol(null)[2]).toMatchObject({
      ruta: '/registro',
      consulta: { tipo: 'prestador' },
    });
  });

  it.each([
    ['TURISTA', ['Catálogo', 'Recomendaciones', 'Mis solicitudes', 'Mis opiniones']],
    ['PRESTADOR', ['Catálogo', 'Recomendaciones', 'Mis ofertas']],
    ['OPERADOR_DISPONIBILIDAD', ['Cola de solicitudes']],
    ['GESTOR_DESTINO', ['Indicadores']],
    ['ADMINISTRADOR', ['Administración']],
  ] as const)('el menú de %s', (rol, esperado) => {
    expect(textos(rol)).toEqual(esperado);
  });

  it('el menú también está en inglés', () => {
    establecerIdioma('en', { recordar: false });

    expect(textos(null)).toEqual(['Catalog', 'Recommendations', "I'm a provider"]);
    expect(textos('TURISTA')).toEqual(['Catalog', 'Recommendations', 'My requests', 'My reviews']);
    expect(textos('PRESTADOR')).toEqual(['Catalog', 'Recommendations', 'My offers']);
    expect(textos('ADMINISTRADOR')).toEqual(['Administration']);
  });

  it.each(ROLES)(
    'el enlace de la sección de un %s apunta a una ruta que ese rol puede abrir',
    (rol) => {
      for (const enlace of enlacesDelRol(rol)) {
        const exige = rolQueExige(enlace.ruta);
        expect(exige === null || exige === rol, `${enlace.clave} (${enlace.ruta})`).toBe(true);
      }
    },
  );

  it.each(['OPERADOR_DISPONIBILIDAD', 'GESTOR_DESTINO', 'ADMINISTRADOR'] as const)(
    'el %s tiene un enlace a su inicio',
    (rol) => {
      expect(enlacesDelRol(rol).map((enlace) => enlace.ruta)).toContain(INICIO_POR_ROL[rol]);
    },
  );

  it('el prestador llega a su inicio desde "Mis ofertas"', () => {
    expect(enlacesDelRol('PRESTADOR').map((enlace) => enlace.ruta)).toContain(
      INICIO_POR_ROL.PRESTADOR,
    );
  });
});

describe('Recomendaciones y alertas', () => {
  const recomendaciones = enlacesDelRol(null).find((enlace) => enlace.ruta === '/recomendaciones')!;

  it('el menú solo tiene "Recomendaciones": las alertas no tienen enlace propio', () => {
    for (const rol of [null, 'TURISTA', 'PRESTADOR'] as const) {
      const rutas = enlacesDelRol(rol).map((enlace) => enlace.ruta);
      expect(rutas).toContain('/recomendaciones');
      expect(rutas).not.toContain('/alertas');
    }
  });

  it('los roles internos no lo ven', () => {
    for (const rol of ['OPERADOR_DISPONIBILIDAD', 'GESTOR_DESTINO', 'ADMINISTRADOR'] as const) {
      expect(enlacesDelRol(rol).map((enlace) => enlace.ruta)).not.toContain('/recomendaciones');
    }
  });

  it('queda marcado en Recomendaciones y también en las alertas, que se alcanzan desde ahí', () => {
    expect(enlaceActivo(recomendaciones, '/recomendaciones')).toBe(true);
    expect(enlaceActivo(recomendaciones, '/alertas')).toBe(true);
    expect(enlaceActivo(recomendaciones, '/alertas?x=1')).toBe(true);
    expect(enlaceActivo(recomendaciones, '/')).toBe(false);
    expect(enlaceActivo(recomendaciones, '/ofertas/abc')).toBe(false);
  });

  it('es una página pública: ningún rol la exige', () => {
    expect(rolQueExige('/recomendaciones')).toBeNull();
    expect(rolQueExige('/alertas')).toBeNull();
  });
});

describe('enlaceActivo', () => {
  const catalogo: EnlaceNavegacion = enlacesDelRol(null)[0];
  const ofertas: EnlaceNavegacion = enlacesDelRol('PRESTADOR').find((enlace) => enlace.ruta === '/prestador/ofertas')!;
  const sinSecciones: EnlaceNavegacion = enlacesDelRol(null).find((enlace) => enlace.secciones.length === 0)!;

  it('el catálogo se marca en la raíz, con filtros y en la ficha de una oferta', () => {
    expect(enlaceActivo(catalogo, '/')).toBe(true);
    expect(enlaceActivo(catalogo, '/?zona=a01&page=2')).toBe(true);
    expect(enlaceActivo(catalogo, '/ofertas/abc')).toBe(true);
  });

  it('el catálogo no se marca en otras secciones', () => {
    expect(enlaceActivo(catalogo, '/prestador/ofertas')).toBe(false);
    expect(enlaceActivo(catalogo, '/mis-opiniones')).toBe(false);
    expect(enlaceActivo(catalogo, '/iniciar-sesion')).toBe(false);
  });

  it('una sección se marca en toda su rama', () => {
    expect(enlaceActivo(ofertas, '/prestador')).toBe(true);
    expect(enlaceActivo(ofertas, '/prestador/ofertas/nueva')).toBe(true);
    expect(enlaceActivo(ofertas, '/prestador/ofertas?page=2')).toBe(true);
  });

  it('no confunde una ruta que solo empieza parecido', () => {
    expect(enlaceActivo(ofertas, '/prestadores-destacados')).toBe(false);
  });

  it('un enlace sin secciones nunca se marca', () => {
    expect(enlaceActivo(sinSecciones, '/registro')).toBe(false);
  });
});
