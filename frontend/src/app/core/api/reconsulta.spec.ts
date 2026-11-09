import { reconsultas } from './reconsulta';

describe('reconsultas', () => {
  let cuenta: number;
  let suscripcion: { unsubscribe(): void };

  const estado = (valor: DocumentVisibilityState) =>
    Object.defineProperty(document, 'visibilityState', { value: valor, configurable: true });

  beforeEach(() => {
    cuenta = 0;
    suscripcion = reconsultas(document).subscribe(() => cuenta++);
  });

  afterEach(() => {
    suscripcion.unsubscribe();
    estado('visible');
  });

  it('emite al volver a la pestaña', () => {
    estado('visible');

    document.dispatchEvent(new Event('visibilitychange'));

    expect(cuenta).toBe(1);
  });

  it('no emite al salir de la pestaña', () => {
    estado('hidden');

    document.dispatchEvent(new Event('visibilitychange'));

    expect(cuenta).toBe(0);
  });

  it('emite al reconectarse', () => {
    window.dispatchEvent(new Event('online'));

    expect(cuenta).toBe(1);
  });

  it('no emite al perder la conexión', () => {
    window.dispatchEvent(new Event('offline'));

    expect(cuenta).toBe(0);
  });

  it('deja de escuchar al cancelar la suscripción', () => {
    suscripcion.unsubscribe();
    estado('visible');

    document.dispatchEvent(new Event('visibilitychange'));
    window.dispatchEvent(new Event('online'));

    expect(cuenta).toBe(0);
  });
});
