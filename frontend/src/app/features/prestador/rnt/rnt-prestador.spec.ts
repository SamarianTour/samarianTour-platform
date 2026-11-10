import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { PrestadorPropio } from '../../../core/api/modelos-nucleo';
import { AvisosService } from '../../../core/avisos/avisos.service';
import { erroresInterceptor } from '../../../core/errores/errores.interceptor';
import { establecerIdioma } from '../../../core/idioma/idioma';
import { CLAVE_SESION, aSesionGuardada } from '../../../core/sesion/almacen-sesion';
import { SesionService } from '../../../core/sesion/sesion.service';
import { respuestaDeSesion } from '../../../core/sesion/testing';
import { RntPrestador } from './rnt-prestador';

const BASE = '/api/nucleo/prestador';

function prestador(extra: Partial<PrestadorPropio> = {}): PrestadorPropio {
  return {
    id: 'p1',
    numeroRnt: '910006',
    tipo: 'AGENCIA_OPERADORA',
    numeroEmpleados: 14,
    categoria: 'MICRO_PEQUENO',
    razonSocial: 'MAR Y SELVA S.A.S.',
    municipio: 'SANTA MARTA',
    estadoRnt: 'ACTIVO',
    ultimaSincronizacion: '2026-11-09T15:00:00Z',
    habilitado: true,
    ...extra,
  };
}

const GUIA = prestador({
  tipo: 'GUIA_INDEPENDIENTE',
  numeroEmpleados: undefined,
  categoria: 'GUIA_INDEPENDIENTE',
  modalidades: ['CAMINATA'],
  idiomas: ['es', 'en'],
});

function problema(status: number, codigo: string, detail = codigo) {
  return {
    body: { status, codigo, title: codigo, detail },
    init: { status, statusText: codigo, headers: { 'Content-Type': 'application/problem+json' } },
  };
}

describe('RntPrestador (P03.1)', () => {
  let harness: RouterTestingHarness;
  let http: HttpTestingController;
  let mostrar: ReturnType<typeof vi.spyOn>;
  let renovar: ReturnType<typeof vi.spyOn>;

  const raiz = () => harness.routeNativeElement as HTMLElement;
  const texto = () => (raiz().textContent ?? '').replace(/[  ]/g, ' ').replace(/\s+/g, ' ');
  const campo = (id: string) => raiz().querySelector<HTMLInputElement>(`#rnt-${id}`)!;
  const dato = (rotulo: string) => {
    const dt = Array.from(raiz().querySelectorAll('dt')).find((e) => e.textContent?.trim() === rotulo);
    return dt?.nextElementSibling?.textContent?.replace(/\s+/g, ' ').trim();
  };
  const botonDeForm = () => raiz().querySelector<HTMLButtonElement>('#rnt-form-titulo ~ form button[type="submit"]')!;

  /** Abre la pantalla y responde al registro (`null` = 404, todavía sin RNT). */
  async function crear(registro: PrestadorPropio | null): Promise<void> {
    await harness.navigateByUrl('/prestador/rnt', RntPrestador);
    harness.detectChanges();
    const peticion = http.expectOne(`${BASE}/registro`);
    if (registro) {
      peticion.flush(registro);
    } else {
      const { body, init } = problema(404, 'PRESTADOR_NO_REGISTRADO');
      peticion.flush(body, init);
    }
    harness.detectChanges();
  }

  function escribir(id: string, valor: string): void {
    const entrada = campo(id);
    entrada.value = valor;
    entrada.dispatchEvent(new Event(entrada.tagName === 'SELECT' ? 'change' : 'input'));
    harness.detectChanges();
  }

  function enviar(): void {
    botonDeForm().click();
    harness.detectChanges();
  }

  beforeEach(async () => {
    localStorage.clear();
    sessionStorage.clear();
    establecerIdioma('es', { recordar: false });
    localStorage.setItem(CLAVE_SESION, JSON.stringify(aSesionGuardada(respuestaDeSesion({ rol: 'PRESTADOR' }))));
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'prestador/rnt', component: RntPrestador }]),
        provideHttpClient(withInterceptors([erroresInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    mostrar = vi.spyOn(TestBed.inject(AvisosService), 'mostrar');
    renovar = vi.spyOn(TestBed.inject(SesionService), 'renovar').mockReturnValue(of('token-nuevo'));
    harness = await RouterTestingHarness.create();
  });

  afterEach(() => {
    establecerIdioma('es', { recordar: false });
    http.verify();
  });

  describe('sin RNT registrado', () => {
    it('ofrece registrarlo y no muestra ninguna validación', async () => {
      await crear(null);

      expect(texto()).toContain('Registra tu RNT');
      expect(raiz().querySelector('#rnt-estado-titulo')).toBeNull();
      expect(campo('numeroRnt')).not.toBeNull();
      expect(botonDeForm().textContent?.trim()).toBe('Validar y registrar');
    });

    it('los campos del tipo cambian con el tipo elegido', async () => {
      await crear(null);
      expect(campo('numeroEmpleados')).toBeNull();
      expect(campo('idiomas')).toBeNull();

      escribir('tipo', 'AGENCIA_OPERADORA');
      expect(campo('numeroEmpleados')).not.toBeNull();
      expect(campo('idiomas')).toBeNull();

      escribir('tipo', 'GUIA_INDEPENDIENTE');
      expect(campo('numeroEmpleados')).toBeNull();
      expect(campo('idiomas')).not.toBeNull();
      expect(raiz().querySelectorAll('#rnt-modalidades input[type="checkbox"]')).toHaveLength(4);
    });

    it('no envía nada si faltan datos y marca los campos', async () => {
      await crear(null);

      enviar();

      http.expectNone((p) => p.method === 'POST');
      expect(texto()).toContain('Escribe tu número de RNT.');
      expect(texto()).toContain('Elige tu tipo de prestador.');
      expect(campo('numeroRnt').getAttribute('aria-invalid')).toBe('true');
    });

    it('una agencia envía su número y sus empleados; mientras valida no deja enviar otra vez', async () => {
      await crear(null);
      escribir('numeroRnt', ' 910006 ');
      escribir('tipo', 'AGENCIA_OPERADORA');
      escribir('numeroEmpleados', '14');

      enviar();
      enviar();

      const peticion = http.expectOne(`${BASE}/registro`);
      expect(peticion.request.method).toBe('POST');
      expect(peticion.request.body).toEqual({ numeroRnt: '910006', tipo: 'AGENCIA_OPERADORA', numeroEmpleados: 14 });
      expect(botonDeForm().disabled).toBe(true);
      expect(botonDeForm().textContent?.trim()).toBe('Validando…');
      expect(texto()).toContain('puede tardar unos segundos');
      peticion.flush(prestador());
    });

    it('un guía envía modalidades e idiomas y no empleados', async () => {
      await crear(null);
      escribir('numeroRnt', '910006');
      escribir('tipo', 'GUIA_INDEPENDIENTE');
      raiz().querySelectorAll<HTMLInputElement>('#rnt-modalidades input')[0].click();
      raiz().querySelectorAll<HTMLInputElement>('#rnt-modalidades input')[2].click();
      escribir('idiomas', 'ES, en');

      enviar();

      const peticion = http.expectOne(`${BASE}/registro`);
      expect(peticion.request.body).toEqual({
        numeroRnt: '910006',
        tipo: 'GUIA_INDEPENDIENTE',
        modalidades: ['CAMINATA', 'KAYAK'],
        idiomas: ['es', 'en'],
      });
      peticion.flush(GUIA);
    });

    it('al registrar con éxito muestra la validación, avisa y renueva la sesión una vez', async () => {
      await crear(null);
      escribir('numeroRnt', '910006');
      escribir('tipo', 'AGENCIA_OPERADORA');
      escribir('numeroEmpleados', '14');
      enviar();

      http.expectOne(`${BASE}/registro`).flush(prestador());
      harness.detectChanges();

      expect(mostrar).toHaveBeenCalledWith('Validamos tu RNT: está activo. Ya puedes publicar ofertas.');
      expect(renovar).toHaveBeenCalledTimes(1);
      expect(raiz().querySelector('#rnt-estado-titulo')).not.toBeNull();
      expect(dato('Número de RNT')).toBe('910006');
      // El formulario ya no pide el número: ahora es el del tipo.
      expect(campo('numeroRnt')).toBeNull();
      expect(botonDeForm().textContent?.trim()).toBe('Guardar el tipo');
    });

    it.each([
      ['VERIFICACION_PENDIENTE', 'el registro oficial no respondió a tiempo'],
      ['NO_ENCONTRADO', 'no lo encontramos en el registro oficial'],
      ['INACTIVO', 'lo muestra inactivo'],
    ] as const)('si el RNT queda %s lo dice en el aviso', async (estado, fragmento) => {
      await crear(null);
      escribir('numeroRnt', '910006');
      escribir('tipo', 'AGENCIA_OPERADORA');
      escribir('numeroEmpleados', '14');
      enviar();

      http.expectOne(`${BASE}/registro`).flush(prestador({ estadoRnt: estado, habilitado: false }));

      expect(mostrar).toHaveBeenCalledWith(expect.stringContaining(fragmento));
    });

    it('si otro prestador ya tiene el número lo marca en el campo del número', async () => {
      await crear(null);
      escribir('numeroRnt', '910006');
      escribir('tipo', 'AGENCIA_OPERADORA');
      escribir('numeroEmpleados', '14');
      enviar();

      const { body, init } = problema(409, 'RNT_YA_REGISTRADO');
      http.expectOne(`${BASE}/registro`).flush(body, init);
      harness.detectChanges();

      expect(texto()).toContain('Otro prestador ya registró ese número de RNT.');
      expect(campo('numeroRnt').getAttribute('aria-invalid')).toBe('true');
      expect(renovar).not.toHaveBeenCalled();
      expect(botonDeForm().disabled).toBe(false);
    });

    it('si el tipo no coincide con el RNT lo marca en el campo del tipo', async () => {
      await crear(null);
      escribir('numeroRnt', '910006');
      escribir('tipo', 'ALOJAMIENTO');
      escribir('numeroEmpleados', '14');
      enviar();

      const { body, init } = problema(409, 'TIPO_NO_COINCIDE_CON_RNT');
      http.expectOne(`${BASE}/registro`).flush(body, init);
      harness.detectChanges();

      expect(campo('tipo').getAttribute('aria-invalid')).toBe('true');
      expect(renovar).not.toHaveBeenCalled();
    });

    it('si la API señala un campo, el mensaje va en ese campo', async () => {
      await crear(null);
      escribir('numeroRnt', '910006');
      escribir('tipo', 'AGENCIA_OPERADORA');
      escribir('numeroEmpleados', '14');
      enviar();

      const { body, init } = problema(400, 'DATO_INVALIDO', 'numeroEmpleados: no puede ser tan alto');
      http.expectOne(`${BASE}/registro`).flush(body, init);
      harness.detectChanges();

      expect(texto()).toContain('no puede ser tan alto');
      expect(campo('numeroEmpleados').getAttribute('aria-invalid')).toBe('true');
    });

    it('si ya estaba registrado (otra pestaña) vuelve a leer su registro', async () => {
      await crear(null);
      escribir('numeroRnt', '910006');
      escribir('tipo', 'AGENCIA_OPERADORA');
      escribir('numeroEmpleados', '14');
      enviar();

      const { body, init } = problema(409, 'PRESTADOR_YA_REGISTRADO');
      http.expectOne(`${BASE}/registro`).flush(body, init);
      harness.detectChanges();

      http.expectOne(`${BASE}/registro`).flush(prestador());
      harness.detectChanges();
      expect(raiz().querySelector('#rnt-estado-titulo')).not.toBeNull();
    });

    it('si no se puede renovar la sesión lo explica, pero lo registrado queda', async () => {
      renovar.mockReturnValue(throwError(() => new Error('sin red')));
      await crear(null);
      escribir('numeroRnt', '910006');
      escribir('tipo', 'AGENCIA_OPERADORA');
      escribir('numeroEmpleados', '14');
      enviar();

      http.expectOne(`${BASE}/registro`).flush(prestador());
      harness.detectChanges();

      expect(texto()).toContain('no pudimos actualizar tu sesión');
      expect(raiz().querySelector('#rnt-estado-titulo')).not.toBeNull();
    });
  });

  describe('con el RNT registrado', () => {
    it('muestra cómo quedó la validación', async () => {
      await crear(prestador());

      expect(dato('Número de RNT')).toBe('910006');
      expect(dato('Razón social')).toBe('MAR Y SELVA S.A.S.');
      expect(dato('Municipio')).toBe('SANTA MARTA');
      expect(dato('Categoría')).toBe('Micro o pequeña empresa');
      expect(dato('Tipo de prestador')).toBe('Agencia operadora');
      expect(dato('Publicar ofertas')).toBe('Sí, puedes publicar');
      expect(raiz().querySelector('.tarjeta .etiqueta')?.textContent?.trim()).toBe('Activo');
      expect(texto()).toContain('Tu RNT está activo y vigente');
    });

    it.each([
      ['INACTIVO', 'Tu RNT está inactivo'],
      ['NO_ENCONTRADO', 'No encontramos tu RNT'],
      ['VERIFICACION_PENDIENTE', 'Estamos verificando tu RNT'],
    ] as const)('con el RNT %s explica qué pasa', async (estado, titulo) => {
      await crear(prestador({ estadoRnt: estado, habilitado: false }));

      expect(texto()).toContain(titulo);
      expect(dato('Publicar ofertas')).toBe('Todavía no');
      expect(texto()).not.toContain('Tu RNT está activo y vigente');
    });

    it('el formulario del tipo nace con los datos que ya tiene y no pide el número', async () => {
      await crear(prestador());

      expect(campo('numeroRnt')).toBeNull();
      expect(campo('tipo').value).toBe('AGENCIA_OPERADORA');
      expect(campo('numeroEmpleados').value).toBe('14');
    });

    it('un guía ve sus modalidades e idiomas', async () => {
      await crear(GUIA);

      expect(campo('idiomas').value).toBe('es, en');
      const marcadas = Array.from(raiz().querySelectorAll<HTMLInputElement>('#rnt-modalidades input')).map((c) => c.checked);
      expect(marcadas).toEqual([true, false, false, false]);
    });

    it('cambiar el tipo manda el tipo nuevo con PUT y renueva la sesión', async () => {
      await crear(prestador());
      escribir('tipo', 'ALOJAMIENTO');
      escribir('numeroEmpleados', '30');

      enviar();

      const peticion = http.expectOne(`${BASE}/tipo`);
      expect(peticion.request.method).toBe('PUT');
      expect(peticion.request.body).toEqual({ tipo: 'ALOJAMIENTO', numeroEmpleados: 30 });
      peticion.flush(prestador({ tipo: 'ALOJAMIENTO', numeroEmpleados: 30 }));
      harness.detectChanges();

      expect(renovar).toHaveBeenCalledTimes(1);
      expect(mostrar).toHaveBeenCalledWith('Guardaste tu tipo de prestador.');
      expect(dato('Tipo de prestador')).toBe('Alojamiento');
    });

    it('un tipo que no coincide con el RNT se marca en el campo y no renueva la sesión', async () => {
      await crear(prestador());
      escribir('tipo', 'RESTAURANTE');
      escribir('numeroEmpleados', '30');
      enviar();

      const { body, init } = problema(409, 'TIPO_NO_COINCIDE_CON_RNT');
      http.expectOne(`${BASE}/tipo`).flush(body, init);
      harness.detectChanges();

      expect(campo('tipo').getAttribute('aria-invalid')).toBe('true');
      expect(renovar).not.toHaveBeenCalled();
    });

    describe('cambiar el número', () => {
      const enviarNumero = () => {
        raiz().querySelector<HTMLButtonElement>('#rnt-numero-titulo ~ form button[type="submit"]')!.click();
        harness.detectChanges();
      };

      it('un número mal escrito no se envía', async () => {
        await crear(prestador());
        escribir('numeroNuevo', '91-0006');

        enviarNumero();

        http.expectNone((p) => p.method === 'PUT');
        expect(texto()).toContain('El número de RNT solo lleva dígitos');
      });

      it('lo manda con PUT, actualiza la validación y avisa, sin renovar la sesión', async () => {
        await crear(prestador());
        escribir('numeroNuevo', ' 123456 ');

        enviarNumero();

        const peticion = http.expectOne(`${BASE}/rnt`);
        expect(peticion.request.method).toBe('PUT');
        expect(peticion.request.body).toEqual({ numeroRnt: '123456' });
        peticion.flush(prestador({ numeroRnt: '123456', estadoRnt: 'NO_ENCONTRADO', habilitado: false }));
        harness.detectChanges();

        expect(dato('Número de RNT')).toBe('123456');
        expect(texto()).toContain('No encontramos tu RNT');
        expect(mostrar).toHaveBeenCalledWith(expect.stringContaining('no lo encontramos en el registro oficial'));
        expect(renovar).not.toHaveBeenCalled();
        expect(campo('numeroNuevo').value).toBe('');
      });

      it('si el número es de otro prestador lo dice junto al campo', async () => {
        await crear(prestador());
        escribir('numeroNuevo', '123456');
        enviarNumero();

        const { body, init } = problema(409, 'RNT_YA_REGISTRADO');
        http.expectOne(`${BASE}/rnt`).flush(body, init);
        harness.detectChanges();

        expect(raiz().querySelector('#rnt-numeroNuevo-error')?.textContent).toContain('Otro prestador ya registró ese número');
        expect(campo('numeroNuevo').getAttribute('aria-invalid')).toBe('true');
      });
    });
  });

  it('si no puede leer el registro lo avisa', async () => {
    await harness.navigateByUrl('/prestador/rnt', RntPrestador);
    harness.detectChanges();
    http.expectOne(`${BASE}/registro`).error(new ProgressEvent('error'));
    harness.detectChanges();

    expect(raiz().querySelector('.alerta--error')).not.toBeNull();
    expect(campo('tipo')).toBeNull();
  });

  it('en inglés cambian los textos y los nombres', async () => {
    establecerIdioma('en', { recordar: false });
    await crear(prestador({ estadoRnt: 'INACTIVO', habilitado: false }));

    expect(texto()).toContain('RNT validation');
    expect(texto()).toContain('Your RNT is inactive');
    expect(dato('Category')).toBe('Micro or small business');
    expect(dato('Provider type')).toBe('Tour operator agency');
    expect(botonDeForm().textContent?.trim()).toBe('Save the type');
  });
});
