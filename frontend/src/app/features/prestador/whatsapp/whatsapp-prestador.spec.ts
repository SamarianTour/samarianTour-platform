import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { PrestadorPropio } from '../../../core/api/modelos-nucleo';
import { AvisosService } from '../../../core/avisos/avisos.service';
import { erroresInterceptor } from '../../../core/errores/errores.interceptor';
import { establecerIdioma } from '../../../core/idioma/idioma';
import { CLAVE_SESION, aSesionGuardada } from '../../../core/sesion/almacen-sesion';
import { SesionService } from '../../../core/sesion/sesion.service';
import { respuestaDeSesion } from '../../../core/sesion/testing';
import { WhatsappPrestador } from './whatsapp-prestador';

const BASE = '/api/nucleo/prestador';

function prestador(extra: Partial<PrestadorPropio> = {}): PrestadorPropio {
  return {
    id: 'p1',
    numeroRnt: '910006',
    tipo: 'AGENCIA_OPERADORA',
    categoria: 'MICRO_PEQUENO',
    estadoRnt: 'ACTIVO',
    habilitado: true,
    ...extra,
  };
}

const SIN_NUMERO = prestador();
const SIN_VERIFICAR = prestador({ whatsapp: '+573001110202', whatsappVerificado: false });
const VERIFICADO = prestador({ whatsapp: '+573001110202', whatsappVerificado: true });

function problema(status: number, codigo: string, detail = codigo, extra: object = {}) {
  return {
    body: { status, codigo, title: codigo, detail, ...extra },
    init: { status, statusText: codigo, headers: { 'Content-Type': 'application/problem+json' } },
  };
}

describe('WhatsappPrestador (P03.4)', () => {
  let harness: RouterTestingHarness;
  let http: HttpTestingController;
  let mostrar: ReturnType<typeof vi.spyOn>;
  let actualizarCuenta: ReturnType<typeof vi.spyOn>;

  const raiz = () => harness.routeNativeElement as HTMLElement;
  const texto = () => (raiz().textContent ?? '').replace(/[  ]/g, ' ').replace(/\s+/g, ' ');
  const numero = () => raiz().querySelector<HTMLInputElement>('#whatsapp-numero')!;
  const verificacion = () => raiz().querySelector('app-verificar-whatsapp');
  const botonGuardar = () => raiz().querySelector<HTMLButtonElement>('.formulario form button[type="submit"]')!;
  const etiqueta = () => raiz().querySelector('.tarjeta__cabecera .etiqueta')?.textContent?.trim();

  /** Abre la pantalla y responde al registro (`null` = 404, todavía sin RNT). */
  async function crear(registro: PrestadorPropio | null): Promise<void> {
    await harness.navigateByUrl('/prestador/whatsapp', WhatsappPrestador);
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

  function escribirNumero(valor: string): void {
    numero().value = valor;
    numero().dispatchEvent(new Event('input'));
    harness.detectChanges();
  }

  function guardar(): void {
    botonGuardar().click();
    harness.detectChanges();
  }

  function escribirCodigo(codigo: string): void {
    const entrada = raiz().querySelector<HTMLInputElement>('#codigo')!;
    entrada.value = codigo;
    entrada.dispatchEvent(new Event('input'));
    harness.detectChanges();
  }

  function verificar(): void {
    raiz().querySelector<HTMLButtonElement>('app-verificar-whatsapp button[type="submit"]')!.click();
    harness.detectChanges();
  }

  beforeEach(async () => {
    localStorage.clear();
    sessionStorage.clear();
    establecerIdioma('es', { recordar: false });
    localStorage.setItem(CLAVE_SESION, JSON.stringify(aSesionGuardada(respuestaDeSesion({ rol: 'PRESTADOR' }))));
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'prestador/whatsapp', component: WhatsappPrestador }]),
        provideHttpClient(withInterceptors([erroresInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    mostrar = vi.spyOn(TestBed.inject(AvisosService), 'mostrar');
    actualizarCuenta = vi.spyOn(TestBed.inject(SesionService), 'actualizarCuenta');
    harness = await RouterTestingHarness.create();
  });

  afterEach(() => {
    establecerIdioma('es', { recordar: false });
    http.verify();
  });

  describe('sin RNT registrado', () => {
    it('explica que primero hay que registrarlo y lleva a la sección RNT', async () => {
      await crear(null);

      expect(texto()).toContain('Primero registra tu RNT');
      expect(raiz().querySelector('a')?.getAttribute('href')).toBe('/prestador/rnt');
      expect(numero()).toBeNull();
    });
  });

  describe('sin número', () => {
    it('invita a configurarlo y no muestra la verificación', async () => {
      await crear(SIN_NUMERO);

      expect(etiqueta()).toBe('Sin configurar');
      expect(texto()).toContain('Todavía no configuraste tu WhatsApp');
      expect(numero().value).toBe('');
      expect(verificacion()).toBeNull();
    });

    it('un número vacío o sin indicativo no se envía', async () => {
      await crear(SIN_NUMERO);

      guardar();
      expect(texto()).toContain('Escribe tu número de WhatsApp.');
      escribirNumero('3001110202');
      guardar();

      http.expectNone((p) => p.method === 'PUT');
      expect(texto()).toContain('Escribe el número con el indicativo del país');
      expect(numero().getAttribute('aria-invalid')).toBe('true');
    });

    it('guarda el número sin separadores y queda por verificar con el código', async () => {
      await crear(SIN_NUMERO);
      escribirNumero(' +57 300 111-0202 ');

      guardar();
      guardar();

      const peticion = http.expectOne(`${BASE}/whatsapp`);
      expect(peticion.request.method).toBe('PUT');
      expect(peticion.request.body).toEqual({ whatsapp: '+573001110202' });
      peticion.flush(SIN_VERIFICAR);
      harness.detectChanges();

      expect(mostrar).toHaveBeenCalledWith('Guardaste tu WhatsApp. Te enviamos un código por WhatsApp para verificarlo.');
      expect(etiqueta()).toBe('Sin verificar');
      expect(numero().value).toBe('+573001110202');
      expect(verificacion()?.querySelector('strong')?.textContent).toBe('+573001110202');
    });

    it('si el núcleo rechaza el número lo marca en el campo', async () => {
      await crear(SIN_NUMERO);
      escribirNumero('+573001110202');
      guardar();

      const { body, init } = problema(400, 'DATO_INVALIDO', 'whatsapp: no es un número válido');
      http.expectOne(`${BASE}/whatsapp`).flush(body, init);
      harness.detectChanges();

      expect(texto()).toContain('no es un número válido');
      expect(numero().getAttribute('aria-invalid')).toBe('true');
      expect(botonGuardar().disabled).toBe(false);
    });
  });

  describe('con el número sin verificar', () => {
    it('muestra el número, la verificación del turista adaptada y el aviso del prestador', async () => {
      await crear(SIN_VERIFICAR);

      expect(numero().value).toBe('+573001110202');
      expect(etiqueta()).toBe('Sin verificar');
      expect(verificacion()).not.toBeNull();
      expect(texto()).toContain('el aviso de una solicitud pagada');
      expect(texto()).not.toContain('no podrás enviar solicitudes');
    });

    it('verifica con las rutas del prestador, no con las del turista, y pasa a verificado', async () => {
      await crear(SIN_VERIFICAR);
      escribirCodigo('123456');

      verificar();

      const peticion = http.expectOne(`${BASE}/whatsapp/verificacion`);
      expect(peticion.request.method).toBe('POST');
      expect(peticion.request.body).toEqual({ codigo: '123456' });
      peticion.flush(VERIFICADO);
      harness.detectChanges();

      expect(etiqueta()).toBe('Verificado');
      expect(texto()).toContain('Tu WhatsApp está verificado');
      expect(verificacion()).toBeNull();
      expect(mostrar).toHaveBeenCalledWith('Verificaste tu WhatsApp');
      // La verificación es del prestador: no cambia la cuenta de la sesión ni pide nada al turista.
      expect(actualizarCuenta).not.toHaveBeenCalled();
    });

    it('un código incorrecto dice cuántos intentos quedan y sigue por verificar', async () => {
      await crear(SIN_VERIFICAR);
      escribirCodigo('000000');
      verificar();

      const { body, init } = problema(400, 'CODIGO_INCORRECTO', 'x', { intentosRestantes: 2 });
      http.expectOne(`${BASE}/whatsapp/verificacion`).flush(body, init);
      harness.detectChanges();

      expect(raiz().querySelector('app-verificar-whatsapp .alerta--error')?.textContent).toContain('Te quedan 2 intentos');
      expect(etiqueta()).toBe('Sin verificar');
    });

    it('pide otro código con la ruta del prestador', async () => {
      await crear(SIN_VERIFICAR);

      raiz().querySelector<HTMLButtonElement>('app-verificar-whatsapp .boton--secundario')!.click();
      harness.detectChanges();

      const peticion = http.expectOne(`${BASE}/whatsapp/codigo`);
      expect(peticion.request.method).toBe('POST');
      peticion.flush(null, { status: 202, statusText: 'Accepted' });
      harness.detectChanges();

      expect(texto()).toContain('Te enviamos un código nuevo');
    });

    it('si el código ya no sirve lo dice con el mensaje del núcleo', async () => {
      await crear(SIN_VERIFICAR);
      escribirCodigo('123456');
      verificar();

      const { body, init } = problema(409, 'CODIGO_NO_VIGENTE');
      http.expectOne(`${BASE}/whatsapp/verificacion`).flush(body, init);
      harness.detectChanges();

      expect(raiz().querySelector('app-verificar-whatsapp .alerta--error')?.textContent).toContain('venció o ya se usó');
    });
  });

  describe('con el número verificado', () => {
    it('lo dice y no pide ningún código', async () => {
      await crear(VERIFICADO);

      expect(etiqueta()).toBe('Verificado');
      expect(texto()).toContain('Tu WhatsApp está verificado');
      expect(verificacion()).toBeNull();
    });

    it('cambiar el número lo deja sin verificar y vuelve a pedir el código', async () => {
      await crear(VERIFICADO);
      escribirNumero('+573009998877');
      guardar();

      http.expectOne(`${BASE}/whatsapp`).flush(prestador({ whatsapp: '+573009998877', whatsappVerificado: false }));
      harness.detectChanges();

      expect(etiqueta()).toBe('Sin verificar');
      expect(verificacion()).not.toBeNull();
    });
  });

  it('si no puede leer el registro lo avisa', async () => {
    await harness.navigateByUrl('/prestador/whatsapp', WhatsappPrestador);
    harness.detectChanges();
    http.expectOne(`${BASE}/registro`).error(new ProgressEvent('error'));
    harness.detectChanges();

    expect(raiz().querySelector('.alerta--error')).not.toBeNull();
    expect(numero()).toBeNull();
  });

  it('en inglés cambian los textos, la verificación y el aviso', async () => {
    establecerIdioma('en', { recordar: false });
    await crear(SIN_VERIFICAR);

    expect(texto()).toContain('Your number');
    expect(etiqueta()).toBe('Not verified');
    expect(texto()).toContain('Verify your WhatsApp');
    expect(texto()).toContain("won't get the notice of a paid request");
  });
});
