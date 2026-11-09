import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Cuenta, Rol } from '../../../core/api/modelos-nucleo';
import { establecerIdioma } from '../../../core/idioma/idioma';
import { AvisosService } from '../../../core/avisos/avisos.service';
import { erroresInterceptor } from '../../../core/errores/errores.interceptor';
import { CLAVE_SESION, aSesionGuardada } from '../../../core/sesion/almacen-sesion';
import { SesionService } from '../../../core/sesion/sesion.service';
import { respuestaDeSesion } from '../../../core/sesion/testing';
import { MisDatos } from './mis-datos';

const BASE = '/api/nucleo';
const POLITICA = {
  version: 'v1',
  fechaPublicacion: '2026-10-28',
  urlTexto: `${BASE}/politica-datos/v1`,
};

describe('MisDatos', () => {
  let fixture: ComponentFixture<MisDatos>;
  let http: HttpTestingController;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const texto = () => raiz().textContent ?? '';
  const campo = (id: string) => raiz().querySelector<HTMLInputElement>(`#${id}`)!;
  const boton = (contenido: string) =>
    Array.from(raiz().querySelectorAll<HTMLButtonElement>('button')).find((b) =>
      b.textContent?.includes(contenido),
    )!;

  /**
   * Abre la pantalla con la sesión del rol dado y responde lo que pide al abrirse: sus datos y la política vigente.
   * `delServidor` cambia lo que el servidor devuelve respecto a lo que la sesión guardaba.
   */
  function crear(
    rol: Rol = 'TURISTA',
    delServidor: Partial<Cuenta> = {},
    verificar?: string,
  ): void {
    const sesion = respuestaDeSesion({ rol });
    localStorage.setItem(CLAVE_SESION, JSON.stringify(aSesionGuardada(sesion)));
    fixture = TestBed.createComponent(MisDatos);
    if (verificar) {
      fixture.componentRef.setInput('verificar', verificar);
    }
    fixture.detectChanges();
    http.expectOne(`${BASE}/cuenta`).flush({ ...sesion.cuenta, ...delServidor });
    http.expectOne(`${BASE}/politica-datos/vigente`).flush(POLITICA);
    fixture.detectChanges();
  }

  function escribir(id: string, valor: string): void {
    campo(id).value = valor;
    campo(id).dispatchEvent(new Event('input'));
  }

  function editar(): void {
    boton('Editar mis datos').click();
    fixture.detectChanges();
  }

  function guardar(): void {
    raiz().querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    fixture.detectChanges();
  }

  beforeAll(() => {
    // jsdom no trae el comportamiento del <dialog>: se imita lo mínimo que usa el visor de la política.
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
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([erroresInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  describe('los datos', () => {
    it('muestra nombre, correo, WhatsApp verificado y tipo de cuenta de un turista', () => {
      crear('TURISTA');

      expect(raiz().querySelector('h1')?.textContent).toContain('Mis datos y derechos del titular');
      expect(texto()).toContain('Laura');
      expect(texto()).toContain('laura@demo.samariantour.co');
      expect(texto()).toContain('+573105550007');
      expect(texto()).toContain('Verificado');
      expect(texto()).toContain('Turista');
      expect(texto()).toContain('El correo no se puede cambiar.');
    });

    it('un prestador no ve WhatsApp ni verificación', () => {
      crear('PRESTADOR');

      const datos = raiz().querySelector('dl.datos')!.textContent;
      expect(datos).toContain('Prestador');
      expect(datos).not.toContain('WhatsApp');
      expect(raiz().querySelector('app-verificar-whatsapp')).toBeNull();
    });

    it.each(['OPERADOR_DISPONIBILIDAD', 'GESTOR_DESTINO', 'ADMINISTRADOR'] as const)(
      'un %s también ve y puede editar sus datos',
      (rol) => {
        crear(rol);

        expect(raiz().querySelector('#editar')).not.toBeNull();
      },
    );

    it('se actualiza con lo que dice el servidor, que puede diferir de lo que guardó la sesión', () => {
      crear('TURISTA', { whatsappVerificado: false });

      expect(texto()).toContain('Sin verificar');
      expect(raiz().querySelector('app-verificar-whatsapp')).not.toBeNull();
    });

    it('un turista con el WhatsApp verificado no ve el panel de verificación', () => {
      crear('TURISTA');

      expect(raiz().querySelector('app-verificar-whatsapp')).toBeNull();
    });

    it('si no se pueden cargar, lo dice y permite reintentar', () => {
      const sesion = respuestaDeSesion({ rol: 'TURISTA' });
      fixture = TestBed.createComponent(MisDatos);
      localStorage.setItem(CLAVE_SESION, JSON.stringify(aSesionGuardada(sesion)));
      fixture.detectChanges();
      http.expectOne(`${BASE}/cuenta`).error(new ProgressEvent('error'));
      http.expectOne(`${BASE}/politica-datos/vigente`).flush(POLITICA);
      fixture.detectChanges();

      expect(raiz().querySelector('[role="alert"]')?.textContent).toContain(
        'No pudimos conectar con el servidor',
      );
      boton('Intentar de nuevo').click();
      http.expectOne(`${BASE}/cuenta`).flush(sesion.cuenta);
    });

    it('con ?verificar=whatsapp lleva el foco al código', () => {
      crear('TURISTA', { whatsappVerificado: false }, 'whatsapp');

      expect(document.activeElement).toBe(campo('codigo'));
    });
  });

  describe('editar', () => {
    it('abre el formulario con los datos actuales y el correo de solo lectura', () => {
      crear('TURISTA');

      editar();

      expect(campo('nombre').value).toBe('Laura');
      expect(campo('whatsapp').value).toBe('+573105550007');
      expect(campo('correo-fijo').readOnly).toBe(true);
    });

    it('un prestador edita solo el nombre', () => {
      crear('PRESTADOR');

      editar();

      expect(campo('nombre')).not.toBeNull();
      expect(raiz().querySelector('#whatsapp')).toBeNull();
    });

    it('no guarda un nombre vacío ni un WhatsApp sin indicativo', () => {
      crear('TURISTA');
      editar();

      escribir('nombre', '   ');
      escribir('whatsapp', '3105550007');
      guardar();

      http.expectNone(`${BASE}/cuenta`);
      expect(texto()).toContain('Escribe tu nombre.');
      expect(texto()).toContain('con el indicativo del país');
      expect(document.activeElement).toBe(campo('nombre'));
    });

    it('si no cambió nada, cierra sin molestar al servidor', () => {
      crear('TURISTA');
      editar();

      guardar();

      http.expectNone(`${BASE}/cuenta`);
      expect(raiz().querySelector('form.edicion')).toBeNull();
    });

    it('guarda el nombre nuevo, actualiza la sesión y lo confirma', () => {
      crear('TURISTA');
      editar();
      escribir('nombre', '  Laura Gómez Ruiz ');

      guardar();

      const peticion = http.expectOne(`${BASE}/cuenta`);
      expect(peticion.request.method).toBe('PUT');
      expect(peticion.request.body).toEqual({
        nombre: 'Laura Gómez Ruiz',
        whatsapp: '+573105550007',
      });
      const sesion = TestBed.inject(SesionService);
      peticion.flush({ ...sesion.cuenta()!, nombre: 'Laura Gómez Ruiz' });
      fixture.detectChanges();

      expect(sesion.cuenta()?.nombre).toBe('Laura Gómez Ruiz');
      expect(texto()).toContain('Laura Gómez Ruiz');
      expect(raiz().querySelector('form.edicion')).toBeNull();
      expect(TestBed.inject(AvisosService).avisos()[0].texto).toBe('Guardamos tus datos.');
      expect(document.activeElement).toBe(raiz().querySelector('#editar'));
    });

    it('un prestador envía solo el nombre', () => {
      crear('PRESTADOR');
      editar();
      escribir('nombre', 'Hotel Sierra Nevada');

      guardar();

      const peticion = http.expectOne(`${BASE}/cuenta`);
      expect(peticion.request.body).toEqual({ nombre: 'Hotel Sierra Nevada' });
      peticion.flush({ ...TestBed.inject(SesionService).cuenta()!, nombre: 'Hotel Sierra Nevada' });
    });

    it('al cambiar el WhatsApp lo envía normalizado, avisa del código nuevo y pide verificarlo', () => {
      crear('TURISTA');
      editar();
      escribir('whatsapp', '+57 300 123 4567');

      guardar();

      const peticion = http.expectOne(`${BASE}/cuenta`);
      expect(peticion.request.body).toEqual({ nombre: 'Laura', whatsapp: '+573001234567' });
      peticion.flush({
        ...TestBed.inject(SesionService).cuenta()!,
        whatsapp: '+573001234567',
        whatsappVerificado: false,
      });
      fixture.detectChanges();

      expect(TestBed.inject(AvisosService).avisos()[0].texto).toContain('código por WhatsApp');
      expect(raiz().querySelector('app-verificar-whatsapp')).not.toBeNull();
    });

    it('mientras espera deshabilita el botón y no envía dos veces', () => {
      crear('TURISTA');
      editar();
      escribir('nombre', 'Otro nombre');

      guardar();
      guardar();

      expect(raiz().querySelector<HTMLButtonElement>('button[type="submit"]')!.disabled).toBe(true);
      http
        .expectOne(`${BASE}/cuenta`)
        .flush({ ...TestBed.inject(SesionService).cuenta()!, nombre: 'Otro nombre' });
    });

    it('si la API rechaza un campo, lo muestra en ese campo', () => {
      crear('TURISTA');
      editar();
      escribir('whatsapp', '+573001234567');

      guardar();
      http
        .expectOne(`${BASE}/cuenta`)
        .flush(
          { status: 400, codigo: 'DATO_INVALIDO', detail: 'whatsapp: el número no es válido' },
          { status: 400, statusText: 'x' },
        );
      fixture.detectChanges();

      expect(raiz().querySelector('#whatsapp-error')?.textContent).toContain(
        'El número no es válido',
      );
      expect(raiz().querySelector('form.edicion')).not.toBeNull();
      expect(document.activeElement).toBe(campo('whatsapp'));
    });

    it('si otra operación cambió los datos a la vez, lo explica con palabras de la cuenta', () => {
      crear('TURISTA');
      editar();
      escribir('nombre', 'Otro nombre');

      guardar();
      http
        .expectOne(`${BASE}/cuenta`)
        .flush({ status: 409, codigo: 'CONFLICTO_CONCURRENCIA' }, { status: 409, statusText: 'x' });
      fixture.detectChanges();

      expect(raiz().querySelector('[role="alert"]')?.textContent).toContain(
        'Tus datos cambiaron mientras los editabas',
      );
      expect(raiz().querySelector('[role="alert"]')?.textContent).not.toContain('oferta');
    });

    it('cancelar descarta los cambios y devuelve el foco al botón de editar', () => {
      crear('TURISTA');
      editar();
      escribir('nombre', 'Otro nombre');

      boton('Cancelar').click();
      fixture.detectChanges();

      expect(raiz().querySelector('form.edicion')).toBeNull();
      expect(texto()).toContain('Laura');
      expect(texto()).not.toContain('Otro nombre');
    });
  });

  describe('derechos del titular', () => {
    it('acceso y rectificación tienen una acción; la rectificación abre la edición', () => {
      crear('TURISTA');

      expect(boton('Ver mis datos')).toBeDefined();
      boton('Actualizar').click();
      fixture.detectChanges();

      expect(raiz().querySelector('form.edicion')).not.toBeNull();
    });

    it.each(['TURISTA', 'PRESTADOR'] as const)(
      'la supresión de un %s lleva a la sección de eliminar la cuenta',
      (rol) => {
        crear(rol);

        boton('Solicitar').click();

        expect(document.activeElement?.id).toBe('eliminar-titulo');
        expect(raiz().querySelector('app-eliminar-cuenta .boton--peligro')).not.toBeNull();
      },
    );

    it.each(['OPERADOR_DISPONIBILIDAD', 'GESTOR_DESTINO', 'ADMINISTRADOR'] as const)(
      'la supresión de un %s se explica: su cuenta interna la gestiona un administrador',
      (rol) => {
        crear(rol);
        const tarjeta = Array.from(raiz().querySelectorAll<HTMLElement>('.derecho')).find((t) =>
          t.querySelector('h3')?.textContent?.includes('Supresión'),
        )!;

        expect(tarjeta.querySelector('button')).toBeNull();
        expect(tarjeta.textContent).toContain('la gestiona un administrador');
        expect(raiz().querySelector('app-eliminar-cuenta .boton--peligro')).toBeNull();
      },
    );

    it('oposición, portabilidad y revocación se explican, pero no tienen botón', () => {
      crear('TURISTA');
      const tarjetas = Array.from(raiz().querySelectorAll<HTMLElement>('.derecho'));

      const sinAccion = tarjetas.filter((t) =>
        t.textContent?.includes('Todavía no se puede ejercer'),
      );

      expect(sinAccion.map((t) => t.querySelector('h3')?.textContent)).toEqual([
        'Oposición',
        'Portabilidad',
        'Revocar la autorización',
      ]);
      expect(sinAccion.every((t) => t.querySelector('button') === null)).toBe(true);
    });
  });

  describe('política de datos', () => {
    it('muestra la versión vigente y permite leerla', () => {
      crear('TURISTA');

      expect(texto()).toContain('Versión vigente: v1');
      boton('Leer la política').click();
      fixture.detectChanges();

      expect(raiz().querySelector('app-visor-politica dialog')!.hasAttribute('open')).toBe(true);
      http.expectOne(`${BASE}/politica-datos/v1`).flush('Texto de la política');
    });
  });

  describe('idioma', () => {
    it('en inglés muestra la pantalla, los derechos y la supresión con el glosario', () => {
      establecerIdioma('en', { recordar: false });
      crear('TURISTA');

      expect(raiz().querySelector('h1')?.textContent).toContain('My data and data subject rights');
      expect(texto()).toContain('Verified');
      expect(texto()).toContain('Tourist');
      expect(texto()).toContain('Your rights');
      expect(texto()).toContain('Rectification');
      expect(texto()).toContain('Current version: v1, published on October 28, 2026');
      expect(texto()).toContain('Delete my account');
      expect(texto()).toContain('remove your reviews from the catalog');
      expect(texto()).not.toContain('Tus derechos');
    });

    it('cambia de idioma con la pantalla abierta, también el rol y la fecha', () => {
      crear('PRESTADOR');
      expect(texto()).toContain('Prestador');
      expect(texto()).toContain('publicada el 28 de octubre de 2026');

      establecerIdioma('en', { recordar: false });
      fixture.detectChanges();

      expect(texto()).toContain('Provider');
      expect(texto()).toContain('published on October 28, 2026');
      expect(texto()).toContain('Your offers are withdrawn permanently');
    });

    it('un error al guardar que ya se mostró cambia de idioma', () => {
      crear('TURISTA');
      editar();
      escribir('nombre', 'Laura Gómez');
      guardar();
      http
        .expectOne(`${BASE}/cuenta`)
        .flush(
          { status: 409, codigo: 'CONFLICTO_CONCURRENCIA' },
          { status: 409, statusText: 'Conflict' },
        );
      fixture.detectChanges();
      expect(texto()).toContain('Tus datos cambiaron mientras los editabas.');

      establecerIdioma('en', { recordar: false });
      fixture.detectChanges();

      expect(texto()).toContain('Your data changed while you were editing it.');
    });
  });
});
