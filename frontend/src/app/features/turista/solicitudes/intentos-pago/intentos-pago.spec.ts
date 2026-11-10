import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EstadoIntento, IntentoPago } from '../../../../core/api/modelos-nucleo';
import { establecerIdioma } from '../../../../core/idioma/idioma';
import { IntentosPago } from './intentos-pago';

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

describe('IntentosPago (P04.2)', () => {
  let fixture: ComponentFixture<IntentosPago>;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const texto = () => (raiz().textContent ?? '').replace(/[  ]/g, ' ');
  const filas = () => Array.from(raiz().querySelectorAll<HTMLElement>('li.intento'));

  function crear(intentos: IntentoPago[]): void {
    fixture = TestBed.createComponent(IntentosPago);
    fixture.componentRef.setInput('intentos', intentos);
    fixture.detectChanges();
  }

  beforeEach(() => establecerIdioma('es', { recordar: false }));

  it('muestra cada intento del primero al último, con su estado', () => {
    crear([intento(1, 'RECHAZADO'), intento(2, 'ERROR'), intento(3, 'ABANDONADO')]);

    expect(filas()).toHaveLength(3);
    expect(filas().map((f) => f.querySelector('strong')?.textContent)).toEqual([
      'Intento 1',
      'Intento 2',
      'Intento 3',
    ]);
    expect(filas().map((f) => f.querySelector('.etiqueta')?.textContent?.trim())).toEqual([
      'Rechazado',
      'Con error',
      'Abandonado',
    ]);
  });

  it('un rechazo dice que no se cobró nada', () => {
    crear([intento(1, 'RECHAZADO')]);

    expect(filas()[0].textContent).toContain('La pasarela rechazó la transacción. No se te cobró nada.');
    expect(filas()[0].querySelector('.etiqueta')?.className).toContain('etiqueta--peligro');
  });

  it('un error explica que puede reintentar y que la confirmación del banco puede tardar', () => {
    crear([intento(1, 'ERROR')]);

    expect(filas()[0].textContent).toContain(
      'Hubo un error al procesar el pago. Puedes intentarlo de nuevo. Si ves un cobro en tu banco, la confirmación puede tardar unos minutos.',
    );
  });

  it('uno abandonado aclara que no cuenta como intento', () => {
    crear([intento(1, 'ABANDONADO')]);

    expect(filas()[0].textContent).toContain('No se completó el pago en 30 minutos. No cuenta como un intento.');
  });

  it('muestra el monto, cuándo empezó, cuándo se resolvió y la referencia de la transacción', () => {
    crear([
      intento(1, 'APROBADO', {
        transaccionId: '1234-1610641025-49201',
        resueltoEn: '2026-10-26T17:06:10',
      }),
    ]);

    expect(texto()).toContain('$ 300.000');
    expect(texto()).toContain('Iniciado');
    expect(texto()).toContain('Resuelto');
    expect(texto()).toContain('Referencia 1234-1610641025-49201');
  });

  it('un intento en curso no muestra "resuelto" ni referencia todavía', () => {
    crear([intento(1, 'EN_CURSO')]);

    expect(texto()).not.toContain('Resuelto');
    expect(texto()).not.toContain('Referencia');
    expect(filas()[0].textContent).toContain('Esperando la confirmación de la pasarela.');
  });

  it('también está en inglés', () => {
    establecerIdioma('en', { recordar: false });
    crear([intento(1, 'RECHAZADO'), intento(2, 'ERROR')]);

    expect(texto()).toContain('Payment attempts');
    expect(texto()).toContain('Attempt 1');
    expect(texto()).toContain('Rejected');
    expect(texto()).toContain('Failed');
    expect(texto()).toContain('You were not charged.');
    expect(texto()).toContain('COP 300,000');
  });
});
