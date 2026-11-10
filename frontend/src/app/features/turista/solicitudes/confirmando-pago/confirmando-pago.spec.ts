import { ComponentFixture, TestBed } from '@angular/core/testing';
import { establecerIdioma } from '../../../../core/idioma/idioma';
import { ConfirmandoPago } from './confirmando-pago';

const AHORA = Date.parse('2026-10-26T15:00:00Z');

describe('ConfirmandoPago', () => {
  let fixture: ComponentFixture<ConfirmandoPago>;
  let consultas: number;
  let visibilidad: DocumentVisibilityState;

  const texto = () => (fixture.nativeElement.textContent ?? '') as string;

  function crear(): void {
    fixture = TestBed.createComponent(ConfirmandoPago);
    consultas = 0;
    fixture.componentInstance.consultar.subscribe(() => consultas++);
    fixture.detectChanges();
  }

  beforeEach(() => {
    vi.useFakeTimers({
      now: AHORA,
      toFake: ['Date', 'setTimeout', 'clearTimeout', 'setInterval', 'clearInterval'],
    });
    establecerIdioma('es', { recordar: false });
    visibilidad = 'visible';
    vi.spyOn(document, 'visibilityState', 'get').mockImplementation(() => visibilidad);
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.useRealTimers();
  });

  it('dice que está confirmando el pago, sin afirmar que se pagó', () => {
    crear();

    expect(texto()).toContain('Confirmando tu pago');
    expect(texto()).toContain('no hace falta volver a pagar');
    expect(texto()).toContain('Si ves un cobro en tu banco, la confirmación puede tardar unos minutos.');
    expect(texto()).not.toMatch(/pago confirmado|pagado/i);
  });

  it('se anuncia a los lectores de pantalla como un estado', () => {
    crear();

    expect(fixture.nativeElement.querySelector('[role="status"]')).not.toBeNull();
  });

  it('no consulta antes de tiempo', () => {
    crear();

    vi.advanceTimersByTime(2_999);

    expect(consultas).toBe(0);
  });

  it('consulta cada 3 segundos durante los primeros 2 minutos', () => {
    crear();

    vi.advanceTimersByTime(3_000);
    expect(consultas).toBe(1);
    vi.advanceTimersByTime(3_000);
    expect(consultas).toBe(2);
    vi.advanceTimersByTime(114_000);
    // 3 s × 40 = 120 s: 40 consultas en los primeros 2 minutos.
    expect(consultas).toBe(40);
  });

  it('pasados los 2 minutos consulta cada 15 segundos', () => {
    crear();
    vi.advanceTimersByTime(120_000);
    const hechas = consultas;

    vi.advanceTimersByTime(14_999);
    expect(consultas).toBe(hechas);
    vi.advanceTimersByTime(1);
    expect(consultas).toBe(hechas + 1);
    vi.advanceTimersByTime(30_000);
    expect(consultas).toBe(hechas + 3);
  });

  it('con la pestaña oculta no consulta, y al volver a verla sigue', () => {
    crear();
    visibilidad = 'hidden';

    vi.advanceTimersByTime(30_000);
    expect(consultas).toBe(0);

    visibilidad = 'visible';
    vi.advanceTimersByTime(3_000);
    expect(consultas).toBe(1);
  });

  it('al quitarse de la pantalla deja de consultar', () => {
    crear();
    vi.advanceTimersByTime(3_000);
    expect(consultas).toBe(1);

    fixture.destroy();
    vi.advanceTimersByTime(60_000);

    expect(consultas).toBe(1);
  });

  it('también está en inglés', () => {
    establecerIdioma('en', { recordar: false });
    crear();

    expect(texto()).toContain('Confirming your payment');
    expect(texto()).toContain('you do not need to pay again');
  });
});
