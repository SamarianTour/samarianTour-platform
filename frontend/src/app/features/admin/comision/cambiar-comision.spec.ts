import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Comision, CuentaInterna } from '../../../core/api/modelos-nucleo';
import { AvisosService } from '../../../core/avisos/avisos.service';
import { erroresInterceptor } from '../../../core/errores/errores.interceptor';
import { establecerIdioma } from '../../../core/idioma/idioma';
import { CLAVE_SESION, aSesionGuardada } from '../../../core/sesion/almacen-sesion';
import { respuestaDeSesion } from '../../../core/sesion/testing';
import { CambiarComision } from './cambiar-comision';

const COMISION = '/api/nucleo/admin/comision';
const CUENTAS = '/api/nucleo/admin/cuentas-internas';

const VIGENTE: Comision = {
  porcentaje: 5.5,
  vigenteDesde: '2026-10-27T15:00:00',
  historial: [
    { porcentaje: 4, vigenteDesde: '2026-01-01T00:00:00' },
    { porcentaje: 5.5, vigenteDesde: '2026-10-27T15:00:00', administradorId: 'adm-1' },
    { porcentaje: 6, vigenteDesde: '2026-06-01T09:00:00', administradorId: 'adm-desconocido-123' },
  ],
};
const INICIAL: Comision = {
  porcentaje: 4,
  vigenteDesde: '2026-01-01T00:00:00',
  historial: [{ porcentaje: 4, vigenteDesde: '2026-01-01T00:00:00' }],
};
const CUENTAS_DEMO = [
  { id: 'adm-1', rol: 'ADMINISTRADOR', nombre: 'Marta Ríos', correo: 'marta@samariantour.co', estado: 'ACTIVA' },
] as CuentaInterna[];

describe('CambiarComision (P05.4)', () => {
  let fixture: ComponentFixture<CambiarComision>;
  let http: HttpTestingController;
  let mostrar: ReturnType<typeof vi.spyOn>;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const texto = () => (raiz().textContent ?? '').replace(/[  ]/g, ' ');
  const campo = () => raiz().querySelector<HTMLInputElement>('#comision-porcentaje')!;
  const botonCambiar = () => raiz().querySelector<HTMLButtonElement>('form button[type="submit"]')!;
  const filas = () => Array.from(raiz().querySelectorAll<HTMLElement>('tbody tr'));

  function abrir(): void {
    fixture = TestBed.createComponent(CambiarComision);
    fixture.detectChanges();
  }

  function crear(comision: Comision = VIGENTE, cuentas: CuentaInterna[] = CUENTAS_DEMO): void {
    abrir();
    http.expectOne(COMISION).flush(comision);
    http.expectOne(CUENTAS).flush(cuentas);
    fixture.detectChanges();
  }

  function escribir(valor: string): void {
    campo().value = valor;
    campo().dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  function pedirCambio(): void {
    botonCambiar().click();
    fixture.detectChanges();
  }

  beforeAll(() => {
    // jsdom no trae el comportamiento del <dialog>: se imita lo mínimo que usa el componente.
    HTMLDialogElement.prototype.showModal ??= function (this: HTMLDialogElement) {
      this.setAttribute('open', '');
    };
    HTMLDialogElement.prototype.close ??= function (this: HTMLDialogElement) {
      this.removeAttribute('open');
      this.dispatchEvent(new Event('close'));
    };
  });

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

  describe('la comisión vigente', () => {
    it('pide la comisión y las cuentas (para ponerle nombre a quien la cambió)', () => {
      abrir();

      expect(http.expectOne(COMISION).request.method).toBe('GET');
      expect(http.expectOne(CUENTAS).request.method).toBe('GET');
      http.match(() => true).forEach((p) => p.flush([]));
    });

    it('muestra el porcentaje vigente, desde cuándo y que un cambio no es retroactivo', () => {
      crear();

      expect(raiz().querySelector('.vigente__valor')?.textContent?.trim()).toBe('5,50 %');
      expect(texto()).toContain('Vigente desde');
      expect(texto()).toContain('27 oct 2026');
      expect(texto()).toContain('Los pagos ya confirmados conservan el porcentaje que se les aplicó');
    });

    it('si la consulta falla avisa del error', () => {
      abrir();
      http.expectOne(COMISION).error(new ProgressEvent('error'));
      http.expectOne(CUENTAS).flush([]);
      fixture.detectChanges();

      expect(raiz().querySelector('.alerta--error')).not.toBeNull();
      expect(raiz().querySelector('form')).toBeNull();
    });
  });

  describe('el historial', () => {
    it('va del cambio más reciente al más antiguo y marca cuál está vigente', () => {
      crear();

      expect(filas().map((f) => f.querySelector('td')?.textContent?.replace(/\s+/g, ' ').trim())).toEqual([
        '5,50 % Vigente',
        '6,00 %',
        '4,00 %',
      ]);
    });

    it('dice quién hizo cada cambio, con el nombre de su cuenta', () => {
      crear();

      expect(filas()[0].textContent).toContain('Marta Ríos');
    });

    it('si no conoce la cuenta muestra su código, y el cambio inicial no trae autor', () => {
      crear();

      expect(filas()[1].textContent).toContain('Administrador ADM-DESC');
      expect(filas()[2].textContent).toContain('Valor inicial');
    });

    it('si las cuentas no se pueden leer, el historial igual se ve con el código', () => {
      abrir();
      http.expectOne(COMISION).flush(VIGENTE);
      http.expectOne(CUENTAS).error(new ProgressEvent('error'));
      fixture.detectChanges();

      expect(filas()[0].textContent).toContain('Administrador ADM-1');
      expect(raiz().querySelector('.alerta--error')).toBeNull();
    });
  });

  describe('cambiar el porcentaje', () => {
    it('sin escribir nada no pide confirmación y señala lo que falta', () => {
      crear();

      pedirCambio();

      expect(raiz().querySelector('dialog')?.hasAttribute('open')).toBe(false);
      expect(texto()).toContain('Escribe el nuevo porcentaje.');
      expect(campo().getAttribute('aria-invalid')).toBe('true');
      expect(document.activeElement).toBe(campo());
    });

    it.each([
      ['100', 'El porcentaje debe estar entre 0 y menos de 100.'],
      ['-1', 'El porcentaje debe estar entre 0 y menos de 100.'],
      ['5.555', 'Usa hasta dos decimales.'],
      ['5.5', 'Ese ya es el porcentaje vigente. Escribe uno distinto.'],
    ])('el valor %s se rechaza antes de enviar: %s', (valor, mensaje) => {
      crear();
      escribir(valor);

      pedirCambio();

      expect(raiz().querySelector('dialog')?.hasAttribute('open')).toBe(false);
      expect(raiz().querySelector('.campo__error')?.textContent).toContain(mensaje);
    });

    it('mientras se escribe un porcentaje válido muestra un ejemplo aproximado: lo descuenta el prestador, no se suma al turista', () => {
      crear(INICIAL, []);

      escribir('5.5');

      expect(raiz().querySelector('.ejemplo')?.textContent?.replace(/\s+/g, ' ')).toContain(
        'en un pago de $ 100.000, la comisión es $ 5.500 y el prestador recibe $ 94.500',
      );
    });

    it('no muestra el ejemplo si el valor no es válido', () => {
      crear(INICIAL, []);

      escribir('120');

      expect(raiz().querySelector('.ejemplo')).toBeNull();
    });

    it('un valor válido pide confirmar, diciendo de cuánto a cuánto pasa y desde cuándo rige', () => {
      crear();
      escribir('6.25');

      pedirCambio();

      const dialogo = raiz().querySelector('dialog')!;
      expect(dialogo.hasAttribute('open')).toBe(true);
      expect(dialogo.textContent).toContain('¿Cambiar la comisión a 6,25 %?');
      expect(dialogo.textContent).toContain('Pasa de 5,50 % a 6,25 %');
      expect(dialogo.textContent).toContain('los ya confirmados no cambian');
      http.expectNone((p) => p.method === 'PUT');
    });

    it('si se cancela no cambia nada', () => {
      crear();
      escribir('6.25');
      pedirCambio();

      raiz().querySelector<HTMLButtonElement>('dialog .boton--secundario')!.click();
      fixture.detectChanges();

      http.expectNone((p) => p.method === 'PUT');
      expect(campo().value).toBe('6.25');
    });

    it('al confirmar envía el porcentaje con PUT, muestra el nuevo valor, limpia el campo, avisa y vuelve a leer el historial', () => {
      crear();
      escribir('6.25');
      pedirCambio();
      raiz().querySelector<HTMLButtonElement>('dialog .boton--peligro')!.click();
      fixture.detectChanges();

      const peticion = http.expectOne((p) => p.url === COMISION && p.method === 'PUT');
      expect(peticion.request.body).toEqual({ porcentaje: 6.25 });
      peticion.flush({ ...VIGENTE, porcentaje: 6.25, vigenteDesde: '2026-11-10T10:00:00' });
      fixture.detectChanges();

      expect(raiz().querySelector('.vigente__valor')?.textContent?.trim()).toBe('6,25 %');
      expect(campo().value).toBe('');
      expect(mostrar).toHaveBeenCalledWith('La comisión ahora es de 6,25 %.');
      // El historial trae ahora el cambio completo: se vuelve a leer.
      http.expectOne((p) => p.url === COMISION && p.method === 'GET').flush(VIGENTE);
      http.expectOne(CUENTAS).flush(CUENTAS_DEMO);
    });

    it('el porcentaje se envía con exactamente las centésimas escritas', () => {
      crear();
      escribir('0.07');
      pedirCambio();
      raiz().querySelector<HTMLButtonElement>('dialog .boton--peligro')!.click();

      const peticion = http.expectOne((p) => p.method === 'PUT');
      expect(peticion.request.body).toEqual({ porcentaje: 0.07 });
      peticion.flush({ ...VIGENTE, porcentaje: 0.07 });
      http.expectOne((p) => p.url === COMISION && p.method === 'GET').flush(VIGENTE);
      http.expectOne(CUENTAS).flush([]);
    });

    it('si el núcleo marca el porcentaje como inválido lo dice en el campo', () => {
      crear();
      escribir('6.25');
      pedirCambio();
      raiz().querySelector<HTMLButtonElement>('dialog .boton--peligro')!.click();

      http
        .expectOne((p) => p.method === 'PUT')
        .flush(
          { codigo: 'DATO_INVALIDO', detail: 'porcentaje: debe ser menor que 100' },
          { status: 400, statusText: 'Bad Request' },
        );
      fixture.detectChanges();

      expect(raiz().querySelector('#comision-error')?.textContent).toContain('debe ser menor que 100');
      expect(botonCambiar().disabled).toBe(false);
    });

    it('un error cualquiera se avisa y deja reintentar', () => {
      crear();
      escribir('6.25');
      pedirCambio();
      raiz().querySelector<HTMLButtonElement>('dialog .boton--peligro')!.click();

      http.expectOne((p) => p.method === 'PUT').error(new ProgressEvent('error'));
      fixture.detectChanges();

      expect(raiz().querySelector('form .alerta--error')).not.toBeNull();
      expect(botonCambiar().disabled).toBe(false);
      expect(raiz().querySelector('.vigente__valor')?.textContent?.trim()).toBe('5,50 %');
    });
  });

  describe('en inglés', () => {
    it('todo cambia al idioma, incluidos el porcentaje y el diálogo', () => {
      establecerIdioma('en', { recordar: false });
      crear();

      expect(texto()).toContain('Commission');
      expect(raiz().querySelector('.vigente__valor')?.textContent?.trim()).toBe('5.50 %');
      expect(texto()).toContain('Change history');
      expect(texto()).toContain('Initial value');

      escribir('6.25');
      pedirCambio();
      expect(raiz().querySelector('dialog')?.textContent).toContain('Change the commission to 6.25%?');
    });
  });
});
