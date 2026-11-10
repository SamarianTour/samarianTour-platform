import { Route } from '@angular/router';
import { routes } from './app.routes';
import { TEXTOS } from './core/idioma/textos';
import { RUTAS_CUENTAS } from './features/cuentas/cuentas.routes';
import { RUTAS_OPERADOR } from './features/operador/operador.routes';
import { RUTAS_PRESTADOR } from './features/prestador/prestador.routes';
import { RUTAS_TURISTA } from './features/turista/turista.routes';

/**
 * El `title` de una ruta es una clave de texto, no el texto. Si alguien escribe un título suelto o una clave que no
 * existe, la pestaña del navegador mostraría ese texto sin traducir: aquí se detecta.
 */
function recorrer(rutas: readonly Route[], ruta = ''): { ruta: string; route: Route }[] {
  return rutas.flatMap((r) => {
    const aqui = [ruta, r.path].filter(Boolean).join('/');
    return [{ ruta: aqui, route: r }, ...recorrer(r.children ?? [], aqui)];
  });
}

const TODAS = [
  ...recorrer(routes),
  ...recorrer(RUTAS_CUENTAS),
  ...recorrer(RUTAS_TURISTA, '(turista)'),
  ...recorrer(RUTAS_PRESTADOR, 'prestador'),
  ...recorrer(RUTAS_OPERADOR, 'operador'),
];

describe('títulos y textos de las rutas', () => {
  const conTitulo = TODAS.filter(({ route }) => typeof route.title === 'string');

  it('hay rutas con título', () => {
    expect(conTitulo.length).toBeGreaterThan(10);
  });

  it.each(conTitulo.map(({ ruta, route }) => [ruta || '/', route.title as string] as const))(
    'la ruta %s tiene como título una clave que existe en español e inglés (%s)',
    (_ruta, clave) => {
      expect(clave in TEXTOS.es, `${clave} no está en español`).toBe(true);
      expect(clave in TEXTOS.en, `${clave} no está en inglés`).toBe(true);
    },
  );

  it('el nombre y la descripción de las pantallas "próximamente" son claves que existen', () => {
    const claves = TODAS.flatMap(({ route }) => {
      const datos = route.data as { nombre?: string; descripcion?: string } | undefined;
      return datos ? [datos.nombre, datos.descripcion].filter((c): c is string => !!c) : [];
    });

    expect(claves.length).toBeGreaterThan(0);
    for (const clave of claves) {
      expect(clave in TEXTOS.es, `${clave} no está en español`).toBe(true);
      expect(clave in TEXTOS.en, `${clave} no está en inglés`).toBe(true);
    }
  });
});
