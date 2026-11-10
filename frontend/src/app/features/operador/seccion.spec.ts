import { seccionDelOperador } from './seccion';

describe('seccionDelOperador', () => {
  it.each([
    ['/operador', 'cola'],
    ['/operador?page=2', 'cola'],
    ['/operador/solicitudes/6f1c2a9e', 'cola'],
    ['/operador/historial', 'historial'],
    ['/operador/historial?estado=ATENDIDA&page=1', 'historial'],
    ['/operador/avisos', 'avisos'],
    ['/operador/avisos?page=3', 'avisos'],
  ] as const)('%s marca %s', (url, esperada) => {
    expect(seccionDelOperador(url)).toBe(esperada);
  });

  it('no confunde una ruta que solo empieza parecido', () => {
    expect(seccionDelOperador('/operador/historial-viejo')).toBe('cola');
  });
});
