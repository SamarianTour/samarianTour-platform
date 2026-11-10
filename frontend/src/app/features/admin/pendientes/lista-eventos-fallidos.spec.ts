import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EventoPagoFallido } from '../../../core/api/modelos-nucleo';
import { AvisosService } from '../../../core/avisos/avisos.service';
import { erroresInterceptor } from '../../../core/errores/errores.interceptor';
import { establecerIdioma } from '../../../core/idioma/idioma';
import { CLAVE_SESION, aSesionGuardada } from '../../../core/sesion/almacen-sesion';
import { respuestaDeSesion } from '../../../core/sesion/testing';
import { ListaEventosFallidos } from './lista-eventos-fallidos';

const FALLIDOS = '/api/nucleo/admin/pagos/eventos-fallidos';

function evento(id: string, extra: Partial<EventoPagoFallido> = {}): EventoPagoFallido {
  return {
    id,
    referencia: `ST-${id}`,
    transaccionId: `1234-${id}`,
    estado: 'APROBADA',
    monto: 300000,
    intentos: 5,
    ultimoError: 'DataAccessResourceFailureException: no se pudo conectar con la base de datos',
    recibidoEn: '2026-10-26T19:31:00',
    ...extra,
  };
}

describe('ListaEventosFallidos', () => {
  let fixture: ComponentFixture<ListaEventosFallidos>;
  let http: HttpTestingController;
  let mostrar: ReturnType<typeof vi.spyOn>;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const texto = () => (raiz().textContent ?? '').replace(/[  ]/g, ' ');
  const tarjetas = () => Array.from(raiz().querySelectorAll<HTMLElement>('li.elemento'));
  const boton = (tarjeta: number) => tarjetas()[tarjeta].querySelector<HTMLButtonElement>('.acciones button')!;

  function crear(eventos: EventoPagoFallido[]): void {
    fixture = TestBed.createComponent(ListaEventosFallidos);
    fixture.detectChanges();
    http.expectOne(FALLIDOS).flush(eventos);
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

  afterEach(() => http.verify());

  it('pide los eventos de pago fallidos', () => {
    fixture = TestBed.createComponent(ListaEventosFallidos);
    fixture.detectChanges();

    expect(http.expectOne(FALLIDOS).request.method).toBe('GET');
    http.match(() => true).forEach((p) => p.flush([]));
  });

  it('explica que el pago no se confirma hasta que el evento se procese y que hay que corregir la causa', () => {
    crear([evento('e1')]);

    expect(texto()).toContain('el pago no se confirma hasta que el evento se procese');
    expect(texto()).toContain('Reintenta solo después de corregir la causa');
    expect(texto()).toContain('en los siguientes 5 segundos');
  });

  it('muestra la transacción, la referencia, el monto, el estado en la pasarela, los fallos y cuándo se recibió', () => {
    crear([evento('e1')]);

    const t = tarjetas()[0];
    expect(t.querySelector('h3')?.textContent).toBe('Transacción 1234-e1');
    expect(t.querySelector('.monto')?.textContent?.replace(/[  ]/g, ' ')).toBe('$ 300.000');
    expect(t.textContent).toContain('Referencia ST-e1');
    expect(t.textContent).toContain('Estado en la pasarela: Aprobada');
    expect(t.textContent).toContain('5 fallos');
    expect(t.textContent).toContain('26 oct 2026');
  });

  it('un estado de la pasarela que no conoce se muestra tal cual', () => {
    crear([evento('e1', { estado: 'PENDIENTE_DE_BANCO' })]);

    expect(tarjetas()[0].textContent).toContain('Estado en la pasarela: PENDIENTE_DE_BANCO');
  });

  it('con un solo fallo usa el singular', () => {
    crear([evento('e1', { intentos: 1 })]);

    expect(tarjetas()[0].textContent).toContain('1 fallo');
    expect(tarjetas()[0].textContent).not.toContain('1 fallos');
  });

  it('muestra el último error, en una línea y, si hay más, completo aparte', () => {
    crear([
      evento('e1', { ultimoError: 'Error corto' }),
      evento('e2', { ultimoError: 'IllegalStateException: monto distinto\n\tat co.samarian.pagos.Procesador', recibidoEn: '2026-10-25T10:00:00' }),
    ]);

    expect(tarjetas()[0].querySelector('.respuesta__linea')?.textContent?.trim()).toBe('Error corto');
    expect(tarjetas()[0].querySelector('details')).toBeNull();
    expect(tarjetas()[1].querySelector('.respuesta__linea')?.textContent?.trim()).toBe(
      'IllegalStateException: monto distinto',
    );
    expect(tarjetas()[1].querySelector('details pre')?.textContent).toContain('co.samarian.pagos.Procesador');
  });

  it('si no quedó mensaje del error lo dice', () => {
    crear([evento('e1', { ultimoError: undefined })]);

    expect(tarjetas()[0].textContent).toContain('No quedó un mensaje del error.');
  });

  it('pone primero el recibido más reciente y cuenta cuántos hay', () => {
    crear([evento('vieja', { recibidoEn: '2026-10-20T10:00:00' }), evento('nueva', { recibidoEn: '2026-10-27T10:00:00' })]);

    expect(tarjetas().map((t) => t.textContent!.includes('1234-nueva'))).toEqual([true, false]);
    expect(texto()).toContain('2 eventos fallidos');
  });

  it('sin eventos fallidos lo dice', () => {
    crear([]);

    expect(tarjetas()).toHaveLength(0);
    expect(texto()).toContain('No hay eventos fallidos');
  });

  it('si la consulta falla avisa del error', () => {
    fixture = TestBed.createComponent(ListaEventosFallidos);
    fixture.detectChanges();
    http.expectOne(FALLIDOS).error(new ProgressEvent('error'));
    fixture.detectChanges();

    expect(raiz().querySelector('.alerta--error')).not.toBeNull();
  });

  describe('reintentar', () => {
    it('devuelve el evento a la cola con POST, lo quita de la lista y avisa', () => {
      crear([evento('e1'), evento('e2', { recibidoEn: '2026-10-25T10:00:00' })]);

      boton(0).click();
      fixture.detectChanges();
      expect(boton(0).textContent?.trim()).toBe('Reintentando…');
      const peticion = http.expectOne(`${FALLIDOS}/e1/reintento`);
      expect(peticion.request.method).toBe('POST');
      expect(peticion.request.body).toBeNull();
      peticion.flush({ ...evento('e1'), intentos: 0 });
      fixture.detectChanges();

      expect(tarjetas()).toHaveLength(1);
      expect(tarjetas()[0].textContent).toContain('1234-e2');
      expect(mostrar).toHaveBeenCalledWith(
        'Devolviste el evento a la cola. Se procesará en los siguientes 5 segundos.',
      );
    });

    it('el botón dice a qué transacción se refiere, y mientras reintenta no deja reintentar otro ni repetir', () => {
      crear([evento('e1'), evento('e2', { recibidoEn: '2026-10-25T10:00:00' })]);
      expect(boton(0).getAttribute('aria-label')).toBe('Reintentar el evento de la transacción 1234-e1');

      boton(0).click();
      boton(0).click();
      fixture.detectChanges();

      expect(http.match((p) => p.url.endsWith('/reintento'))).toHaveLength(1);
      expect(boton(1).disabled).toBe(true);
    });

    it.each(['EVENTO_NO_FALLIDO', 'EVENTO_NO_ENCONTRADO'])(
      '%s lo explica en la tarjeta y vuelve a leer la lista',
      (codigo) => {
        crear([evento('e1')]);

        boton(0).click();
        http
          .expectOne(`${FALLIDOS}/e1/reintento`)
          .flush({ codigo }, { status: codigo === 'EVENTO_NO_ENCONTRADO' ? 404 : 409, statusText: 'Error' });
        fixture.detectChanges();

        expect(tarjetas()[0].querySelector('.campo__error')).not.toBeNull();
        http.expectOne(FALLIDOS).flush([evento('e1')]);
      },
    );

    it('un error cualquiera se avisa en la tarjeta y deja reintentar', () => {
      crear([evento('e1')]);

      boton(0).click();
      http.expectOne(`${FALLIDOS}/e1/reintento`).error(new ProgressEvent('error'));
      fixture.detectChanges();

      expect(tarjetas()[0].querySelector('.campo__error')).not.toBeNull();
      expect(boton(0).disabled).toBe(false);
      expect(tarjetas()).toHaveLength(1);
    });
  });

  it('también se ve en inglés', () => {
    establecerIdioma('en', { recordar: false });
    crear([evento('e1', { intentos: 5 })]);

    const t = tarjetas()[0];
    expect(texto()).toContain('Failed payment events');
    expect(texto()).toContain('1 failed event');
    expect(t.querySelector('h3')?.textContent).toBe('Transaction 1234-e1');
    expect(t.textContent).toContain('Status in the payment gateway: Approved');
    expect(t.textContent).toContain('5 failures');
    expect(t.querySelector('.monto')?.textContent?.replace(/[  ]/g, ' ')).toBe('COP 300,000');
    expect(boton(0).textContent?.trim()).toBe('Retry');
  });
});
