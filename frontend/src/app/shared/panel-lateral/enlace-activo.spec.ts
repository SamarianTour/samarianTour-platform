import { enlacePanelActivo } from './enlace-activo';

describe('enlacePanelActivo', () => {
  const inicio = { ruta: '/admin', exacto: true };
  const zonas = { ruta: '/admin/zonas' };

  it('el inicio del panel solo se marca en su dirección exacta', () => {
    expect(enlacePanelActivo(inicio, '/admin')).toBe(true);
    expect(enlacePanelActivo(inicio, '/admin?pagina=2')).toBe(true);
    expect(enlacePanelActivo(inicio, '/admin/zonas')).toBe(false);
    expect(enlacePanelActivo(inicio, '/admin/moderacion')).toBe(false);
  });

  it('una sección se marca en su dirección y dentro de ella', () => {
    expect(enlacePanelActivo(zonas, '/admin/zonas')).toBe(true);
    expect(enlacePanelActivo(zonas, '/admin/zonas?estado=inactivas')).toBe(true);
    expect(enlacePanelActivo(zonas, '/admin/zonas/nueva')).toBe(true);
    expect(enlacePanelActivo(zonas, '/admin/zonas#formulario')).toBe(true);
  });

  it('no confunde una dirección que solo empieza parecido', () => {
    expect(enlacePanelActivo(zonas, '/admin/zonas-viejas')).toBe(false);
    expect(enlacePanelActivo(zonas, '/admin/cuentas')).toBe(false);
  });
});
