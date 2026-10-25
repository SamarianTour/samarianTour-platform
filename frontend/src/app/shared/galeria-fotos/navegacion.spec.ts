import { indiceCircular } from './navegacion';

describe('indiceCircular', () => {
  it('avanza y retrocede de a una foto', () => {
    expect(indiceCircular(1, 1, 5)).toBe(2);
    expect(indiceCircular(3, -1, 5)).toBe(2);
  });

  it('después de la última vuelve a la primera', () => {
    expect(indiceCircular(4, 1, 5)).toBe(0);
  });

  it('antes de la primera está la última', () => {
    expect(indiceCircular(0, -1, 5)).toBe(4);
  });

  it('con una sola foto se queda donde está', () => {
    expect(indiceCircular(0, 1, 1)).toBe(0);
    expect(indiceCircular(0, -1, 1)).toBe(0);
  });

  it('sin fotos devuelve cero', () => {
    expect(indiceCircular(0, 1, 0)).toBe(0);
  });

  it('da la vuelta aunque el desplazamiento sea mayor que el total', () => {
    expect(indiceCircular(1, 7, 5)).toBe(3);
    expect(indiceCircular(1, -7, 5)).toBe(4);
  });
});
