import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { CuentaCobro } from '../../../core/api/modelos-nucleo';
import { AvisosService } from '../../../core/avisos/avisos.service';
import { erroresInterceptor } from '../../../core/errores/errores.interceptor';
import { establecerIdioma } from '../../../core/idioma/idioma';
import { CLAVE_SESION, aSesionGuardada } from '../../../core/sesion/almacen-sesion';
import { respuestaDeSesion } from '../../../core/sesion/testing';
import { CuentaCobroPrestador } from './cuenta-cobro-prestador';

const URL = '/api/nucleo/prestador/cuenta-cobro';
/** El número completo que se escribe en las pruebas: no debe aparecer en ninguna parte. */
const NUMERO = '40112233440101';

const VIGENTE: CuentaCobro = {
  tipo: 'AHORROS',
  entidad: 'Bancolombia',
  numeroEnmascarado: '******0101',
  titular: 'Mar y Selva Expediciones S.A.S.',
  vigenteDesde: '2026-10-29T15:00:00Z',
};

function problema(status: number, codigo: string, detail = codigo) {
  return {
    body: { status, codigo, title: codigo, detail },
    init: { status, statusText: codigo, headers: { 'Content-Type': 'application/problem+json' } },
  };
}

describe('CuentaCobroPrestador (P03.5)', () => {
  let harness: RouterTestingHarness;
  let http: HttpTestingController;
  let mostrar: ReturnType<typeof vi.spyOn>;

  const raiz = () => harness.routeNativeElement as HTMLElement;
  const texto = () => (raiz().textContent ?? '').replace(/[  ]/g, ' ').replace(/\s+/g, ' ');
  const campo = (id: string) => raiz().querySelector<HTMLInputElement>(`#cobro-${id}`)!;
  const dato = (rotulo: string) => {
    const dt = Array.from(raiz().querySelectorAll('dt')).find((e) => e.textContent?.trim() === rotulo);
    return dt?.nextElementSibling?.textContent?.replace(/\s+/g, ' ').trim();
  };
  const botonGuardar = () => raiz().querySelector<HTMLButtonElement>('.formulario form button[type="submit"]')!;

  /** Abre la pantalla y responde a la cuenta de cobro (`null` = todavía no registró ninguna). */
  async function crear(cuenta: CuentaCobro | null): Promise<void> {
    await harness.navigateByUrl('/prestador/cuenta-cobro', CuentaCobroPrestador);
    harness.detectChanges();
    const peticion = http.expectOne(URL);
    if (cuenta) {
      peticion.flush(cuenta);
    } else {
      const { body, init } = problema(404, 'CUENTA_COBRO_NO_REGISTRADA');
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

  function llenarBanco(): void {
    escribir('tipo', 'AHORROS');
    escribir('entidad', 'Bancolombia');
    escribir('numero', ` ${NUMERO.slice(0, 4)} ${NUMERO.slice(4)} `);
    escribir('titular', ' Mar y Selva Expediciones S.A.S. ');
  }

  function guardar(): void {
    botonGuardar().click();
    harness.detectChanges();
  }

  /** Todo lo que el navegador podría haber guardado: ninguna clave ni valor debe llevar el número. */
  function almacenamiento(): string {
    const volcar = (almacen: Storage) =>
      Object.keys(almacen)
        .map((clave) => `${clave}=${almacen.getItem(clave)}`)
        .join('\n');
    return `${volcar(localStorage)}\n${volcar(sessionStorage)}`;
  }

  beforeEach(async () => {
    localStorage.clear();
    sessionStorage.clear();
    establecerIdioma('es', { recordar: false });
    localStorage.setItem(CLAVE_SESION, JSON.stringify(aSesionGuardada(respuestaDeSesion({ rol: 'PRESTADOR' }))));
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'prestador/cuenta-cobro', component: CuentaCobroPrestador }]),
        provideHttpClient(withInterceptors([erroresInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    mostrar = vi.spyOn(TestBed.inject(AvisosService), 'mostrar');
    harness = await RouterTestingHarness.create();
  });

  afterEach(() => {
    establecerIdioma('es', { recordar: false });
    http.verify();
  });

  it('explica que la comisión se descuenta de lo que recibe y no se suma al turista', async () => {
    await crear(null);

    expect(texto()).toContain('se descuenta de lo que recibes');
    expect(texto()).toContain('nunca se le suma al turista');
  });

  describe('sin cuenta de cobro', () => {
    it('lo dice y ofrece registrarla', async () => {
      await crear(null);

      expect(texto()).toContain('Todavía no registraste tu cuenta de cobro');
      expect(texto()).toContain('Registra tu cuenta de cobro');
      expect(raiz().querySelector('#cobro-vigente-titulo + dl')).toBeNull();
    });

    it('los campos cambian con el tipo: banco y número de cuenta, o solo celular', async () => {
      await crear(null);
      expect(campo('numero')).toBeNull();

      escribir('tipo', 'AHORROS');
      expect(campo('entidad')).not.toBeNull();
      expect(raiz().querySelector('label[for="cobro-numero"]')?.textContent).toContain('Número de cuenta');

      escribir('tipo', 'DAVIPLATA');
      expect(campo('entidad')).toBeNull();
      expect(raiz().querySelector('label[for="cobro-numero"]')?.textContent).toContain('Número de celular');
    });

    it('no envía nada si faltan datos y marca los campos', async () => {
      await crear(null);

      guardar();

      http.expectNone((p) => p.method === 'PUT');
      expect(texto()).toContain('Elige el tipo de cuenta.');
      expect(texto()).toContain('Escribe el nombre del titular.');
      expect(campo('tipo').getAttribute('aria-invalid')).toBe('true');
    });

    it('un número que no cumple el formato se rechaza sin repetirlo', async () => {
      await crear(null);
      escribir('tipo', 'AHORROS');
      escribir('entidad', 'Bancolombia');
      escribir('numero', '12345678901234567890123');
      escribir('titular', 'Laura');

      guardar();

      http.expectNone((p) => p.method === 'PUT');
      expect(texto()).toContain('El número de cuenta tiene de 6 a 20 dígitos.');
      expect(texto()).not.toContain('12345678901234567890123');
    });

    it('guarda una cuenta bancaria con el número limpio y una sola petición', async () => {
      await crear(null);
      llenarBanco();

      guardar();
      guardar();

      const peticion = http.expectOne(URL);
      expect(peticion.request.method).toBe('PUT');
      expect(peticion.request.body).toEqual({
        tipo: 'AHORROS',
        entidad: 'Bancolombia',
        numero: NUMERO,
        titular: 'Mar y Selva Expediciones S.A.S.',
      });
      expect(botonGuardar().disabled).toBe(true);
      peticion.flush(VIGENTE);
    });

    it('una billetera no manda banco', async () => {
      await crear(null);
      escribir('tipo', 'NEQUI');
      escribir('numero', '300 111 0202');
      escribir('titular', 'Laura Pérez');

      guardar();

      const peticion = http.expectOne(URL);
      expect(peticion.request.body).toEqual({ tipo: 'NEQUI', numero: '3001110202', titular: 'Laura Pérez' });
      peticion.flush({ ...VIGENTE, tipo: 'NEQUI', entidad: 'Nequi', numeroEnmascarado: '******0202' });
    });
  });

  describe('el número completo es solo para escribir (tu punto 1)', () => {
    it('el campo no se autocompleta ni se corrige', async () => {
      await crear(null);
      escribir('tipo', 'AHORROS');

      const numero = campo('numero');
      expect(numero.getAttribute('autocomplete')).toBe('off');
      expect(numero.getAttribute('autocorrect')).toBe('off');
      expect(numero.getAttribute('spellcheck')).toBe('false');
      expect(numero.hasAttribute('name')).toBe(false);
      expect(raiz().querySelector('.formulario form')?.getAttribute('autocomplete')).toBe('off');
    });

    it('al guardar, el campo queda vacío y solo se ve enmascarado, como lo devuelve la API', async () => {
      await crear(null);
      llenarBanco();
      guardar();

      http.expectOne(URL).flush(VIGENTE);
      harness.detectChanges();

      expect(campo('numero').value).toBe('');
      expect(dato('Número')).toContain('******0101');
      expect(raiz().querySelector('.solo-lectores')?.textContent).toContain('Termina en 0101');
      expect(texto()).not.toContain(NUMERO);
      expect(texto()).not.toContain(NUMERO.slice(0, 4));
    });

    it('el aviso de confirmación no lleva el número', async () => {
      await crear(null);
      llenarBanco();
      guardar();

      http.expectOne(URL).flush(VIGENTE);

      expect(mostrar).toHaveBeenCalledWith('Guardaste tu cuenta de cobro.');
      expect(JSON.stringify(mostrar.mock.calls)).not.toContain(NUMERO);
    });

    it('no queda guardado en el navegador ni antes ni después de guardar', async () => {
      await crear(null);
      llenarBanco();
      expect(almacenamiento()).not.toContain(NUMERO);

      guardar();
      http.expectOne(URL).flush(VIGENTE);
      harness.detectChanges();

      expect(almacenamiento()).not.toContain(NUMERO);
    });

    it('un error de la API que repite el valor no se muestra: sale un texto fijo del campo', async () => {
      await crear(null);
      llenarBanco();
      guardar();

      const { body, init } = problema(400, 'DATO_INVALIDO', `numero: el valor ${NUMERO} no es válido`);
      http.expectOne(URL).flush(body, init);
      harness.detectChanges();

      expect(texto()).not.toContain(NUMERO);
      expect(texto()).toContain('El número de cuenta tiene de 6 a 20 dígitos.');
      expect(campo('numero').getAttribute('aria-invalid')).toBe('true');
    });

    it('un fallo general tampoco repite el número', async () => {
      await crear(null);
      llenarBanco();
      guardar();

      const { body, init } = problema(409, 'CONFLICTO_CONCURRENCIA', `conflicto con ${NUMERO}`);
      http.expectOne(URL).flush(body, init);
      harness.detectChanges();

      expect(raiz().querySelector('.alerta--error')).not.toBeNull();
      expect(texto()).not.toContain(NUMERO);
    });
  });

  describe('con una cuenta de cobro vigente', () => {
    it('muestra la cuenta con el número enmascarado y desde cuándo vale', async () => {
      await crear(VIGENTE);

      expect(dato('Tipo de cuenta')).toBe('Cuenta de ahorros');
      expect(dato('Banco')).toBe('Bancolombia');
      expect(dato('Número')).toContain('******0101');
      expect(dato('Titular')).toBe('Mar y Selva Expediciones S.A.S.');
      expect(dato('Vigente desde')).toMatch(/2026/);
      expect(raiz().querySelector('[aria-hidden="true"].enmascarado')).not.toBeNull();
    });

    it('el formulario nace con los datos menos el número, que hay que escribir otra vez', async () => {
      await crear(VIGENTE);

      expect(texto()).toContain('Cambiar la cuenta de cobro');
      expect(campo('tipo').value).toBe('AHORROS');
      expect(campo('entidad').value).toBe('Bancolombia');
      expect(campo('titular').value).toBe('Mar y Selva Expediciones S.A.S.');
      expect(campo('numero').value).toBe('');
    });

    it('sin escribir el número otra vez no se guarda ningún cambio', async () => {
      await crear(VIGENTE);
      escribir('titular', 'Otro titular');

      guardar();

      http.expectNone((p) => p.method === 'PUT');
      expect(texto()).toContain('Escribe el número.');
    });

    it('cambiar la cuenta manda todo con PUT y actualiza lo que se ve', async () => {
      await crear(VIGENTE);
      escribir('tipo', 'DAVIPLATA');
      escribir('numero', '3009998877');

      guardar();

      const peticion = http.expectOne(URL);
      expect(peticion.request.body).toEqual({
        tipo: 'DAVIPLATA',
        numero: '3009998877',
        titular: 'Mar y Selva Expediciones S.A.S.',
      });
      peticion.flush({ ...VIGENTE, tipo: 'DAVIPLATA', entidad: 'Daviplata', numeroEnmascarado: '******8877' });
      harness.detectChanges();

      expect(dato('Tipo de cuenta')).toBe('Daviplata');
      expect(dato('Banco')).toBeUndefined();
      expect(dato('Número')).toContain('******8877');
      expect(campo('numero').value).toBe('');
    });
  });

  describe('sin RNT registrado', () => {
    it('al leer: explica que primero hay que registrarlo y lleva a la sección RNT', async () => {
      await harness.navigateByUrl('/prestador/cuenta-cobro', CuentaCobroPrestador);
      harness.detectChanges();
      const { body, init } = problema(404, 'PRESTADOR_NO_REGISTRADO');
      http.expectOne(URL).flush(body, init);
      harness.detectChanges();

      expect(texto()).toContain('Primero registra tu RNT');
      expect(raiz().querySelector('.explicacion a')?.getAttribute('href')).toBe('/prestador/rnt');
      expect(campo('tipo')).toBeNull();
    });

    it('al guardar: lo explica igual', async () => {
      await crear(null);
      llenarBanco();
      guardar();

      const { body, init } = problema(404, 'PRESTADOR_NO_REGISTRADO');
      http.expectOne(URL).flush(body, init);
      harness.detectChanges();

      expect(texto()).toContain('Primero registra tu RNT');
    });
  });

  it('si no puede leer la cuenta lo avisa', async () => {
    await harness.navigateByUrl('/prestador/cuenta-cobro', CuentaCobroPrestador);
    harness.detectChanges();
    http.expectOne(URL).error(new ProgressEvent('error'));
    harness.detectChanges();

    expect(raiz().querySelector('.alerta--error')).not.toBeNull();
    expect(campo('tipo')).toBeNull();
  });

  it('en inglés cambian los textos, los tipos y la lectura del número', async () => {
    establecerIdioma('en', { recordar: false });
    await crear(VIGENTE);

    expect(texto()).toContain('Your current payout account');
    expect(dato('Account type')).toBe('Savings account');
    expect(dato('Account holder')).toBe('Mar y Selva Expediciones S.A.S.');
    expect(raiz().querySelector('.solo-lectores')?.textContent).toContain('Ends in 0101');
    expect(texto()).toContain('never added to the tourist');
  });
});
