import { INICIO_POR_ROL, destinoTrasIniciarSesion, rolQueExige, rutaSegura } from './rutas-por-rol';

describe('INICIO_POR_ROL', () => {
  it('lleva a cada rol a su inicio', () => {
    expect(INICIO_POR_ROL).toEqual({
      TURISTA: '/',
      PRESTADOR: '/prestador/ofertas',
      OPERADOR_DISPONIBILIDAD: '/operador',
      GESTOR_DESTINO: '/gestor',
      ADMINISTRADOR: '/admin',
    });
  });
});

describe('rolQueExige', () => {
  it.each([
    ['/prestador', 'PRESTADOR'],
    ['/prestador/ofertas/nueva', 'PRESTADOR'],
    ['/prestador/ofertas?page=2', 'PRESTADOR'],
    ['/turista/solicitudes/abc', 'TURISTA'],
    ['/mis-opiniones', 'TURISTA'],
    ['/operador/solicitudes', 'OPERADOR_DISPONIBILIDAD'],
    ['/gestor', 'GESTOR_DESTINO'],
    ['/admin/zonas#arriba', 'ADMINISTRADOR'],
  ])('%s es de %s', (url, rol) => {
    expect(rolQueExige(url)).toBe(rol);
  });

  it.each([
    '/',
    '/ofertas/abc',
    '/mis-datos',
    '/prestadores-destacados',
    '/administracion',
    '/registro',
  ])('%s es de cualquiera', (url) => {
    expect(rolQueExige(url)).toBeNull();
  });
});

describe('rutaSegura', () => {
  it.each([
    '/',
    '/ofertas/abc',
    '/prestador/ofertas?page=2',
    '/turista/solicitudes/3f2b#estado',
    '/mis-datos',
    '/?zona=a00000000000000000000007',
  ])('acepta la ruta interna %s', (url) => {
    expect(rutaSegura(url)).toBe(url);
  });

  it.each([
    ['otro sitio', 'https://malo.com'],
    ['sin esquema pero con dominio', '//malo.com'],
    ['barra invertida', '/\\malo.com'],
    ['barra invertida más adentro', '/ofertas\\..\\malo'],
    ['javascript', 'javascript:alert(1)'],
    ['sin barra inicial', 'ofertas/abc'],
    ['con salto de línea', '/ofertas\n//malo.com'],
    ['vacía', ''],
    ['demasiado larga', `/${'a'.repeat(600)}`],
  ])('rechaza %s', (_motivo, url) => {
    expect(rutaSegura(url)).toBeNull();
  });

  it.each([null, undefined])('rechaza %s', (url) => {
    expect(rutaSegura(url)).toBeNull();
  });

  it.each([
    '/iniciar-sesion',
    '/iniciar-sesion?volver=%2F',
    '/registro',
    '/recuperar-contrasena',
    '/restablecer-contrasena?token=x',
  ])('rechaza la pantalla de acceso %s', (url) => {
    expect(rutaSegura(url)).toBeNull();
  });

  it('no confunde una ruta que solo empieza parecido con una pantalla de acceso', () => {
    expect(rutaSegura('/registro-de-ofertas')).toBe('/registro-de-ofertas');
  });
});

describe('destinoTrasIniciarSesion', () => {
  it('vuelve a la página que se quería ver si es de cualquiera', () => {
    expect(destinoTrasIniciarSesion('/ofertas/abc', 'TURISTA')).toBe('/ofertas/abc');
    expect(destinoTrasIniciarSesion('/mis-datos', 'PRESTADOR')).toBe('/mis-datos');
  });

  it('vuelve a una sección que le corresponde a su rol', () => {
    expect(destinoTrasIniciarSesion('/prestador/ofertas/nueva', 'PRESTADOR')).toBe(
      '/prestador/ofertas/nueva',
    );
    expect(destinoTrasIniciarSesion('/mis-opiniones', 'TURISTA')).toBe('/mis-opiniones');
  });

  it('si la sección es de otro rol, va al inicio de su rol', () => {
    expect(destinoTrasIniciarSesion('/prestador/ofertas', 'TURISTA')).toBe('/');
    expect(destinoTrasIniciarSesion('/admin', 'OPERADOR_DISPONIBILIDAD')).toBe('/operador');
  });

  it('sin dirección de regreso, o con una insegura, va al inicio de su rol', () => {
    expect(destinoTrasIniciarSesion(undefined, 'PRESTADOR')).toBe('/prestador/ofertas');
    expect(destinoTrasIniciarSesion('https://malo.com', 'GESTOR_DESTINO')).toBe('/gestor');
    expect(destinoTrasIniciarSesion('/iniciar-sesion', 'ADMINISTRADOR')).toBe('/admin');
  });
});
