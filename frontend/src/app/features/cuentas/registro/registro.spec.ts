import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AvisosService } from '../../../core/avisos/avisos.service';
import { erroresInterceptor } from '../../../core/errores/errores.interceptor';
import { CLAVE_SESION, aSesionGuardada } from '../../../core/sesion/almacen-sesion';
import { SesionService } from '../../../core/sesion/sesion.service';
import { respuestaDeSesion } from '../../../core/sesion/testing';
import { Registro } from './registro';

const BASE = '/api/nucleo';
const REGISTRO = `${BASE}/cuentas/registro`;
const SESION = `${BASE}/cuentas/sesion`;
const POLITICA = {
  version: 'v1',
  fechaPublicacion: '2026-10-28',
  urlTexto: `${BASE}/politica-datos/v1`,
};

describe('Registro', () => {
  let fixture: ComponentFixture<Registro>;
  let http: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;
  let navegarUrl: ReturnType<typeof vi.spyOn>;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const texto = () => raiz().textContent ?? '';
  const campo = (id: string) => raiz().querySelector<HTMLInputElement>(`#${id}`)!;
  const boton = () => raiz().querySelector<HTMLButtonElement>('button[type="submit"]')!;

  function escribir(id: string, valor: string): void {
    const entrada = campo(id);
    entrada.value = valor;
    entrada.dispatchEvent(new Event('input'));
  }

  function elegirRol(rol: 'TURISTA' | 'PRESTADOR'): void {
    const opcion = raiz().querySelector<HTMLInputElement>(`input[type="radio"][value="${rol}"]`)!;
    opcion.click();
    fixture.detectChanges();
  }

  /** Crea la pantalla y responde la lectura de la política vigente que hace al abrirse. */
  function crear(tipo?: string): void {
    fixture = TestBed.createComponent(Registro);
    if (tipo) {
      fixture.componentRef.setInput('tipo', tipo);
    }
    fixture.detectChanges();
    http.expectOne(`${BASE}/politica-datos/vigente`).flush(POLITICA);
    fixture.detectChanges();
  }

  function llenarComoTurista(): void {
    escribir('nombre', 'Laura Gómez');
    escribir('correo', 'laura.gomez@correo.co');
    escribir('contrasena', 'Playa2026');
    escribir('repetir', 'Playa2026');
    escribir('whatsapp', '+57 300 123 4567');
    campo('acepta').click();
    fixture.detectChanges();
  }

  function enviar(): void {
    boton().click();
    fixture.detectChanges();
  }

  const rechazar = (estado: number, cuerpo: object) =>
    http.expectOne(REGISTRO).flush(cuerpo, { status: estado, statusText: 'Error' });

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
    const router = TestBed.inject(Router);
    navegar = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    navegarUrl = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
  });

  afterEach(() => http.verify());

  describe('al abrirse', () => {
    it('parte como turista, pide el nombre completo y el WhatsApp', () => {
      crear();

      expect(raiz().querySelector('h1')?.textContent).toContain('Crear una cuenta');
      expect(raiz().querySelector<HTMLInputElement>('input[value="TURISTA"]')!.checked).toBe(true);
      expect(texto()).toContain('Nombre completo');
      expect(campo('whatsapp')).not.toBeNull();
    });

    it('con ?tipo=prestador deja elegido el prestador', () => {
      crear('prestador');

      expect(raiz().querySelector<HTMLInputElement>('input[value="PRESTADOR"]')!.checked).toBe(
        true,
      );
      expect(texto()).toContain('Nombre comercial del negocio');
    });

    it('como prestador no pide el WhatsApp', () => {
      crear();

      elegirRol('PRESTADOR');

      expect(raiz().querySelector('#whatsapp')).toBeNull();
    });

    it('lee la política vigente y ofrece leerla', () => {
      crear();

      expect(texto()).toContain('Leer la política (versión v1)');
    });

    it('si no se puede leer la política, lo dice y permite reintentar', () => {
      fixture = TestBed.createComponent(Registro);
      fixture.detectChanges();
      http.expectOne(`${BASE}/politica-datos/vigente`).error(new ProgressEvent('error'));
      fixture.detectChanges();

      expect(texto()).toContain('No pudimos conectar con el servidor');
      const reintentar = Array.from(
        raiz().querySelectorAll<HTMLButtonElement>('button.enlace-texto'),
      ).find((b) => b.textContent?.includes('Intentar de nuevo'))!;
      reintentar.click();
      http.expectOne(`${BASE}/politica-datos/vigente`).flush(POLITICA);
      fixture.detectChanges();

      expect(texto()).toContain('Leer la política (versión v1)');
    });

    it('quien ya tiene sesión va a su inicio sin ver el formulario', () => {
      localStorage.setItem(
        CLAVE_SESION,
        JSON.stringify(aSesionGuardada(respuestaDeSesion({ rol: 'PRESTADOR' }))),
      );
      fixture = TestBed.createComponent(Registro);

      fixture.detectChanges();

      expect(navegarUrl).toHaveBeenCalledWith('/prestador/ofertas', { replaceUrl: true });
      http.expectNone(`${BASE}/politica-datos/vigente`);
    });
  });

  describe('validaciones antes de enviar', () => {
    it('no envía si faltan datos y dice cuáles', () => {
      crear();

      enviar();

      http.expectNone(REGISTRO);
      expect(texto()).toContain('Escribe tu nombre completo.');
      expect(texto()).toContain('Escribe tu correo.');
      expect(texto()).toContain('Escribe una contraseña.');
      expect(texto()).toContain('Repite la contraseña.');
      expect(texto()).toContain('Escribe tu número de WhatsApp.');
      expect(texto()).toContain('Debes aceptar la política de tratamiento de datos');
    });

    it('lleva el foco al primer campo con error', () => {
      crear();

      enviar();

      expect(document.activeElement).toBe(campo('nombre'));
    });

    it('rechaza una contraseña corta, o sin número o sin letra', () => {
      crear();

      escribir('contrasena', 'Playa1');
      campo('contrasena').dispatchEvent(new Event('blur'));
      enviar();
      expect(texto()).toContain('entre 8 y 72 caracteres');

      escribir('contrasena', 'SoloLetrasAqui');
      enviar();
      expect(texto()).toContain('al menos una letra y un número');
    });

    it('rechaza una repetición distinta de la contraseña, y la acepta al corregir cualquiera de las dos', () => {
      crear();
      escribir('contrasena', 'Playa2026');
      escribir('repetir', 'Playa2027');
      enviar();
      expect(texto()).toContain('Las contraseñas no coinciden.');

      escribir('contrasena', 'Playa2027');
      fixture.detectChanges();

      expect(texto()).not.toContain('Las contraseñas no coinciden.');
    });

    it('rechaza un WhatsApp sin indicativo', () => {
      crear();

      escribir('whatsapp', '3001234567');
      enviar();

      expect(texto()).toContain('con el indicativo del país');
    });

    it('rechaza un correo mal escrito', () => {
      crear();

      escribir('correo', 'laura@');
      enviar();

      expect(texto()).toContain('Escribe un correo válido');
    });

    it('no deja crear la cuenta sin aceptar la política', () => {
      crear();
      llenarComoTurista();
      campo('acepta').click();

      enviar();

      http.expectNone(REGISTRO);
      expect(texto()).toContain('Debes aceptar la política de tratamiento de datos');
    });
  });

  describe('al registrarse', () => {
    it('un turista envía sus datos con la versión de la política y su WhatsApp normalizado', () => {
      crear();
      llenarComoTurista();

      enviar();

      const peticion = http.expectOne(REGISTRO);
      expect(peticion.request.body).toEqual({
        rol: 'TURISTA',
        nombre: 'Laura Gómez',
        correo: 'laura.gomez@correo.co',
        contrasena: 'Playa2026',
        whatsapp: '+573001234567',
        aceptaPolitica: true,
        versionPolitica: 'v1',
      });
      peticion.flush({ id: 'x' }, { status: 201, statusText: 'Created' });
      http.expectOne(SESION).flush(respuestaDeSesion());
    });

    it('un prestador no envía WhatsApp', () => {
      crear('prestador');
      escribir('nombre', 'Hotel Sierra');
      escribir('correo', 'hotel.sierra@correo.co');
      escribir('contrasena', 'Playa2026');
      escribir('repetir', 'Playa2026');
      campo('acepta').click();
      fixture.detectChanges();

      enviar();

      const peticion = http.expectOne(REGISTRO);
      expect(peticion.request.body).toEqual({
        rol: 'PRESTADOR',
        nombre: 'Hotel Sierra',
        correo: 'hotel.sierra@correo.co',
        contrasena: 'Playa2026',
        aceptaPolitica: true,
        versionPolitica: 'v1',
      });
      peticion.flush({ id: 'x' }, { status: 201, statusText: 'Created' });
      http.expectOne(SESION).flush(respuestaDeSesion({ rol: 'PRESTADOR' }));
    });

    it('mientras espera deshabilita el botón y no envía dos veces', () => {
      crear();
      llenarComoTurista();

      enviar();
      boton().click();
      fixture.detectChanges();

      expect(boton().disabled).toBe(true);
      expect(boton().textContent).toContain('Creando tu cuenta…');
      http.expectOne(REGISTRO).flush({ id: 'x' });
      http.expectOne(SESION).flush(respuestaDeSesion());
    });

    it('un turista entra solo con lo que escribió y va a verificar su WhatsApp', () => {
      crear();
      llenarComoTurista();

      enviar();
      http.expectOne(REGISTRO).flush({ id: 'x' }, { status: 201, statusText: 'Created' });
      const entrada = http.expectOne(SESION);
      expect(entrada.request.body).toEqual({
        correo: 'laura.gomez@correo.co',
        contrasena: 'Playa2026',
      });
      entrada.flush(respuestaDeSesion({ rol: 'TURISTA' }));

      expect(TestBed.inject(SesionService).autenticada()).toBe(true);
      expect(navegar).toHaveBeenCalledWith(['/mis-datos'], {
        queryParams: { verificar: 'whatsapp' },
        replaceUrl: true,
      });
      expect(TestBed.inject(AvisosService).avisos()[0].texto).toContain('código por WhatsApp');
    });

    it('un prestador entra solo y va a su panel', () => {
      crear('prestador');
      escribir('nombre', 'Hotel Sierra');
      escribir('correo', 'hotel.sierra@correo.co');
      escribir('contrasena', 'Playa2026');
      escribir('repetir', 'Playa2026');
      campo('acepta').click();
      fixture.detectChanges();

      enviar();
      http.expectOne(REGISTRO).flush({ id: 'x' }, { status: 201, statusText: 'Created' });
      http.expectOne(SESION).flush(respuestaDeSesion({ rol: 'PRESTADOR' }));

      expect(navegarUrl).toHaveBeenCalledWith('/prestador/ofertas', { replaceUrl: true });
    });

    it('si la cuenta se creó pero la sesión no abrió, lleva a iniciar sesión', () => {
      crear();
      llenarComoTurista();

      enviar();
      http.expectOne(REGISTRO).flush({ id: 'x' }, { status: 201, statusText: 'Created' });
      http.expectOne(SESION).error(new ProgressEvent('error'));

      expect(navegar).toHaveBeenCalledWith(['/iniciar-sesion'], { replaceUrl: true });
      expect(TestBed.inject(AvisosService).avisos()[0].texto).toContain('Inicia sesión');
    });
  });

  describe('cuando el núcleo lo rechaza', () => {
    it('con un correo ya registrado lo dice en el campo y ofrece iniciar sesión o recuperar la contraseña', () => {
      crear();
      llenarComoTurista();

      enviar();
      rechazar(409, { status: 409, codigo: 'CORREO_YA_REGISTRADO' });
      fixture.detectChanges();

      expect(raiz().querySelector('#correo-error')?.textContent).toContain(
        'Ya hay una cuenta con ese correo',
      );
      expect(raiz().querySelector('#correo-error a[href="/iniciar-sesion"]')).not.toBeNull();
      expect(raiz().querySelector('#correo-error a[href="/recuperar-contrasena"]')).not.toBeNull();
      expect(campo('correo').getAttribute('aria-invalid')).toBe('true');
      expect(boton().disabled).toBe(false);
    });

    it('si cambió la política, pide aceptar la nueva y la vuelve a leer', () => {
      crear();
      llenarComoTurista();

      enviar();
      rechazar(409, { status: 409, codigo: 'POLITICA_NO_VIGENTE' });
      fixture.detectChanges();
      http.expectOne(`${BASE}/politica-datos/vigente`).flush({ ...POLITICA, version: 'v2' });
      fixture.detectChanges();

      expect(campo('acepta').checked).toBe(false);
      expect(texto()).toContain('La política de datos cambió');
      expect(texto()).toContain('Leer la política (versión v2)');
    });

    it('con un dato inválido que la API señala, lo muestra en ese campo', () => {
      crear();
      llenarComoTurista();

      enviar();
      rechazar(400, {
        status: 400,
        codigo: 'DATO_INVALIDO',
        detail: 'whatsapp: formato no válido',
      });
      fixture.detectChanges();

      expect(raiz().querySelector('#whatsapp-error')?.textContent).toContain('Formato no válido');
      expect(document.activeElement).toBe(campo('whatsapp'));
    });

    it('al corregir el campo se quita el error que había marcado la API', () => {
      crear();
      llenarComoTurista();
      enviar();
      rechazar(400, { status: 400, codigo: 'DATO_INVALIDO', detail: 'nombre: es obligatorio' });
      fixture.detectChanges();

      escribir('nombre', 'Laura Gómez Ruiz');
      fixture.detectChanges();

      expect(raiz().querySelector('#nombre-error')).toBeNull();
    });

    it('con otro error muestra el mensaje general', () => {
      crear();
      llenarComoTurista();

      enviar();
      http.expectOne(REGISTRO).error(new ProgressEvent('error'));
      fixture.detectChanges();

      expect(raiz().querySelector('[role="alert"]')?.textContent).toContain(
        'No pudimos conectar con el servidor',
      );
      expect(boton().disabled).toBe(false);
    });
  });
});
