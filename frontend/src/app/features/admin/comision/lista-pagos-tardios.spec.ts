import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PagoTardio } from '../../../core/api/modelos-nucleo';
import { AvisosService } from '../../../core/avisos/avisos.service';
import { erroresInterceptor } from '../../../core/errores/errores.interceptor';
import { establecerIdioma } from '../../../core/idioma/idioma';
import { CLAVE_SESION, aSesionGuardada } from '../../../core/sesion/almacen-sesion';
import { respuestaDeSesion } from '../../../core/sesion/testing';
import { ListaPagosTardios } from './lista-pagos-tardios';

const PAGOS = '/api/nucleo/admin/pagos-tardios';

function pago(id: string, monto: number, extra: Partial<PagoTardio> = {}): PagoTardio {
  return {
    id,
    solicitudId: '6f1c2a9e-4b7d-4c1e-9a52-3d8e0f7b1a20',
    referencia: `ST-${id}`,
    transaccionId: `1234-${id}`,
    monto,
    motivo: 'PAGO_TARDIO',
    registradoEn: '2026-10-26T19:31:00',
    avisado: true,
    ...extra,
  };
}

describe('ListaPagosTardios (P05.4)', () => {
  let fixture: ComponentFixture<ListaPagosTardios>;
  let http: HttpTestingController;
  let mostrar: ReturnType<typeof vi.spyOn>;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const texto = () => (raiz().textContent ?? '').replace(/[  ]/g, ' ');
  const filas = () => Array.from(raiz().querySelectorAll<HTMLElement>('tbody tr'));

  function crear(pagos: PagoTardio[]): void {
    fixture = TestBed.createComponent(ListaPagosTardios);
    fixture.detectChanges();
    http.expectOne(PAGOS).flush(pagos);
    fixture.detectChanges();
  }

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    establecerIdioma('es', { recordar: false });
    localStorage.setItem(
      CLAVE_SESION,
      JSON.stringify(aSesionGuardada(respuestaDeSesion({ rol: 'ADMINISTRADOR' }))),
    );
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([erroresInterceptor])), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    mostrar = vi.spyOn(TestBed.inject(AvisosService), 'mostrar');
  });

  /** Pone un portapapeles falso (o ninguno) en el navegador de pruebas, que no trae uno. */
  function definirPortapapeles(valor: { writeText: (texto: string) => Promise<void> } | undefined): void {
    Object.defineProperty(navigator, 'clipboard', { configurable: true, value: valor });
  }

  afterEach(() => {
    http.verify();
    vi.restoreAllMocks();
    Reflect.deleteProperty(navigator, 'clipboard');
  });

  it('pide los pagos tardíos', () => {
    fixture = TestBed.createComponent(ListaPagosTardios);
    fixture.detectChanges();

    expect(http.expectOne(PAGOS).request.method).toBe('GET');
    http.match(() => true).forEach((p) => p.flush([]));
  });

  it('explica que no se devuelven solos: se reembolsan a mano desde la pasarela', () => {
    crear([]);

    expect(texto()).toContain('El sistema no los devuelve');
    expect(texto()).toContain('reembólsalos a mano');
  });

  it('muestra la transacción, la referencia, la solicitud, el monto, el motivo y cuándo se registró', () => {
    crear([pago('a', 300000)]);

    const t = filas()[0].textContent!.replace(/[  ]/g, ' ');
    expect(t).toContain('Transacción 1234-a');
    expect(t).toContain('Referencia ST-a');
    expect(t).toContain('Ref. 6F1C2A9E');
    expect(t).toContain('$ 300.000');
    expect(t).toContain('Pago tardío');
    expect(t).toContain('vencida, cerrada o ya pagada');
    expect(t).toContain('26 oct 2026');
  });

  it('el monto distinto lo explica', () => {
    crear([pago('a', 250000, { motivo: 'MONTO_DISTINTO' })]);

    expect(filas()[0].textContent).toContain('Monto distinto');
    expect(filas()[0].textContent).toContain('otro monto');
  });

  it('ordena del más reciente al más antiguo y suma lo que falta devolver', () => {
    crear([
      pago('vieja', 100000, { registradoEn: '2026-10-20T10:00:00' }),
      pago('nueva', 300000, { registradoEn: '2026-10-27T10:00:00' }),
    ]);

    expect(filas().map((f) => f.textContent!.includes('1234-nueva'))).toEqual([true, false]);
    expect(raiz().querySelector('.total')?.textContent?.replace(/[  ]/g, ' ').trim()).toBe(
      'Por devolver: $ 400.000 en 2 pagos',
    );
  });

  it('con un solo pago usa el singular', () => {
    crear([pago('a', 300000)]);

    expect(raiz().querySelector('.total')?.textContent).toContain('en 1 pago');
    expect(raiz().querySelector('.total')?.textContent).not.toContain('1 pagos');
  });

  it('un pago del que todavía no se avisó por WhatsApp lo marca', () => {
    crear([pago('a', 1, { avisado: false }), pago('b', 1, { registradoEn: '2026-10-20T10:00:00' })]);

    expect(filas()[0].textContent).toContain('Sin avisar por WhatsApp');
    expect(filas()[1].textContent).not.toContain('Sin avisar');
  });

  it('sin pagos tardíos lo dice', () => {
    crear([]);

    expect(raiz().querySelector('table')).toBeNull();
    expect(texto()).toContain('No hay pagos tardíos');
  });

  it('si la consulta falla avisa del error', () => {
    fixture = TestBed.createComponent(ListaPagosTardios);
    fixture.detectChanges();
    http.expectOne(PAGOS).error(new ProgressEvent('error'));
    fixture.detectChanges();

    expect(raiz().querySelector('.alerta--error')).not.toBeNull();
  });

  describe('copiar el id de la transacción', () => {
    function boton(): HTMLButtonElement {
      return filas()[0].querySelector<HTMLButtonElement>('button')!;
    }

    it('el botón dice a qué transacción se refiere', () => {
      crear([pago('a', 1)]);

      expect(boton().getAttribute('aria-label')).toBe('Copiar el id de la transacción 1234-a');
    });

    it('copia el id al portapapeles y avisa', async () => {
      const escribir = vi.fn().mockResolvedValue(undefined);
      definirPortapapeles({ writeText: escribir });
      crear([pago('a', 1)]);

      boton().click();
      await Promise.resolve();

      expect(escribir).toHaveBeenCalledWith('1234-a');
      expect(mostrar).toHaveBeenCalledWith('Copiaste el id de la transacción.');
    });

    it('si el navegador no deja copiar, lo dice', async () => {
      const escribir = vi.fn().mockRejectedValue(new Error('denegado'));
      definirPortapapeles({ writeText: escribir });
      crear([pago('a', 1)]);

      boton().click();
      await Promise.resolve();
      await Promise.resolve();

      expect(mostrar).toHaveBeenCalledWith('No se pudo copiar. Selecciónalo y cópialo a mano.');
    });

    it('si no hay portapapeles, lo dice sin fallar', () => {
      definirPortapapeles(undefined);
      crear([pago('a', 1)]);

      boton().click();

      expect(mostrar).toHaveBeenCalledWith('No se pudo copiar. Selecciónalo y cópialo a mano.');
    });
  });

  it('también se ve en inglés', () => {
    establecerIdioma('en', { recordar: false });
    crear([pago('a', 300000), pago('b', 100000, { motivo: 'MONTO_DISTINTO', registradoEn: '2026-10-20T10:00:00' })]);

    expect(texto()).toContain('Late payments');
    expect(raiz().querySelector('.total')?.textContent?.replace(/[  ]/g, ' ').trim()).toBe(
      'To refund: COP 400,000 in 2 payments',
    );
    expect(filas()[0].textContent).toContain('Late payment');
    expect(filas()[1].textContent).toContain('Different amount');
  });
});
