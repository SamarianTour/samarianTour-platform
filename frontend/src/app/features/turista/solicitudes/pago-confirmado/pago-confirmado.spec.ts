import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { EstadoIntento, IntentoPago, SolicitudTurista } from '../../../../core/api/modelos-nucleo';
import { establecerIdioma } from '../../../../core/idioma/idioma';
import { PagoConfirmado } from './pago-confirmado';

const PAGADA = {
  id: '6f1c2a9e-4b7d-4c1e-9a52-3d8e0f7b1a20',
  oferta: { id: 'b1', tipo: 'ACTIVIDAD', nombre: 'Caminata a Marinka', zonaId: 'a05' },
  fechaInicio: '2026-10-28',
  personas: 2,
  estado: 'PAGADA',
  condiciones: { precioBase: 300000, politicaCancelacion: 'Sin reembolso' },
  creadaEn: '2026-10-26T14:00:00Z',
  actualizadaEn: '2026-10-26T17:06:10Z',
  historial: [],
} as SolicitudTurista;

function intento(numero: number, estado: EstadoIntento, extra: Partial<IntentoPago> = {}): IntentoPago {
  return {
    id: `i${numero}`,
    numero,
    estado,
    referencia: `ST-${numero}`,
    montoCentavos: 30000000,
    creadoEn: '2026-10-26T17:05:00',
    ...extra,
  };
}

describe('PagoConfirmado', () => {
  let fixture: ComponentFixture<PagoConfirmado>;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const texto = () => (raiz().textContent ?? '').replace(/[  ]/g, ' ');

  function crear(intentos: IntentoPago[] = [], enfocar = false): void {
    fixture = TestBed.createComponent(PagoConfirmado);
    fixture.componentRef.setInput('solicitud', PAGADA);
    fixture.componentRef.setInput('intentos', intentos);
    fixture.componentRef.setInput('enfocar', enfocar);
    fixture.detectChanges();
  }

  beforeEach(() => {
    establecerIdioma('es', { recordar: false });
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
  });

  it('confirma el pago con el total pagado', () => {
    crear();

    expect(texto()).toContain('Pago confirmado');
    expect(texto()).toContain('Tu cupo está asegurado');
    expect(texto()).toContain('Total pagado$ 300.000');
  });

  it('muestra la referencia de la transacción y la fecha del intento aprobado (RF-52)', () => {
    crear([
      intento(1, 'RECHAZADO'),
      intento(2, 'APROBADO', { transaccionId: '1234-1610641025-49201', resueltoEn: '2026-10-26T17:06:10' }),
    ]);

    expect(texto()).toContain('Referencia de la transacción1234-1610641025-49201');
    expect(texto()).toContain('Fecha del pago');
  });

  it('sin intentos todavía no inventa una referencia', () => {
    crear();

    expect(texto()).not.toContain('Referencia de la transacción');
  });

  it('ofrece ver mis solicitudes y seguir explorando', () => {
    crear();

    const enlaces = Array.from(raiz().querySelectorAll<HTMLAnchorElement>('a.boton')).map((a) => [
      a.textContent?.trim(),
      a.getAttribute('href'),
    ]);
    expect(enlaces).toEqual([
      ['Ver mis solicitudes', '/turista/solicitudes'],
      ['Seguir explorando', '/catalogo'],
    ]);
  });

  it('si el turista estaba esperando la confirmación, lleva el foco al título', () => {
    crear([], true);

    expect(document.activeElement?.id).toBe('confirmado-titulo');
  });

  it('si abre una solicitud ya pagada, no le roba el foco', () => {
    crear([], false);

    expect(document.activeElement?.id).not.toBe('confirmado-titulo');
  });

  it('también está en inglés', () => {
    establecerIdioma('en', { recordar: false });
    crear([intento(1, 'APROBADO', { transaccionId: 'T-1' })]);

    expect(texto()).toContain('Payment confirmed');
    expect(texto()).toContain('Total paid');
    expect(texto()).toContain('Transaction reference');
    expect(texto()).toContain('COP 300,000');
  });
});
