import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ErrorApi } from '../errores/error-api';
import { erroresInterceptor } from '../errores/errores.interceptor';
import { PrestadorPropio } from './modelos-nucleo';
import { PrestadorService } from './prestador.service';

const BASE = '/api/nucleo/prestador';

const PRESTADOR: PrestadorPropio = {
  id: '20000000-0000-0000-0000-000000000002',
  numeroRnt: '910006',
  tipo: 'GUIA_INDEPENDIENTE',
  categoria: 'GUIA_INDEPENDIENTE',
  estadoRnt: 'ACTIVO',
  habilitado: true,
};

function problema(status: number, codigo: string) {
  return {
    body: { status, codigo, title: codigo, detail: codigo },
    init: { status, statusText: codigo, headers: { 'Content-Type': 'application/problem+json' } },
  };
}

describe('PrestadorService', () => {
  let servicio: PrestadorService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([erroresInterceptor])), provideHttpClientTesting()],
    });
    servicio = TestBed.inject(PrestadorService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('pide su registro con GET', () => {
    let recibido: PrestadorPropio | null | undefined;

    servicio.registro().subscribe((prestador) => (recibido = prestador));

    const peticion = http.expectOne(`${BASE}/registro`);
    expect(peticion.request.method).toBe('GET');
    peticion.flush(PRESTADOR);
    expect(recibido).toEqual(PRESTADOR);
  });

  it('si todavía no registró el RNT devuelve null en vez de fallar', () => {
    let recibido: PrestadorPropio | null | undefined;
    let fallo: unknown;

    servicio.registro().subscribe({ next: (prestador) => (recibido = prestador), error: (e) => (fallo = e) });

    const { body, init } = problema(404, 'PRESTADOR_NO_REGISTRADO');
    http.expectOne(`${BASE}/registro`).flush(body, init);
    expect(recibido).toBeNull();
    expect(fallo).toBeUndefined();
  });

  it('registra el RNT y el tipo con POST', () => {
    let recibido: PrestadorPropio | undefined;
    const cuerpo = { numeroRnt: '910006', tipo: 'AGENCIA_OPERADORA', numeroEmpleados: 14 } as const;

    servicio.registrar(cuerpo).subscribe((prestador) => (recibido = prestador));

    const peticion = http.expectOne(`${BASE}/registro`);
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual(cuerpo);
    peticion.flush(PRESTADOR);
    expect(recibido).toEqual(PRESTADOR);
  });

  it('cambia el número de RNT con PUT y solo manda el número', () => {
    servicio.cambiarRnt('123456').subscribe();

    const peticion = http.expectOne(`${BASE}/rnt`);
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual({ numeroRnt: '123456' });
    peticion.flush(PRESTADOR);
  });

  it('cambia el tipo con PUT', () => {
    const cuerpo = { tipo: 'GUIA_INDEPENDIENTE', modalidades: ['KAYAK'], idiomas: ['es'] } as const;

    servicio.cambiarTipo({ ...cuerpo, modalidades: ['KAYAK'], idiomas: ['es'] }).subscribe();

    const peticion = http.expectOne(`${BASE}/tipo`);
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual(cuerpo);
    peticion.flush(PRESTADOR);
  });

  it('configura el WhatsApp con PUT y solo manda el número', () => {
    servicio.configurarWhatsapp('+573001110202').subscribe();

    const peticion = http.expectOne(`${BASE}/whatsapp`);
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual({ whatsapp: '+573001110202' });
    peticion.flush(PRESTADOR);
  });

  it('pide otro código de WhatsApp con POST y sin cuerpo', () => {
    servicio.enviarCodigoWhatsapp().subscribe();

    const peticion = http.expectOne(`${BASE}/whatsapp/codigo`);
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toBeNull();
    peticion.flush(null, { status: 202, statusText: 'Accepted' });
  });

  it('verifica el WhatsApp con el código y devuelve al prestador', () => {
    let recibido: PrestadorPropio | undefined;

    servicio.verificarWhatsapp('123456').subscribe((prestador) => (recibido = prestador));

    const peticion = http.expectOne(`${BASE}/whatsapp/verificacion`);
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual({ codigo: '123456' });
    peticion.flush({ ...PRESTADOR, whatsappVerificado: true });
    expect(recibido?.whatsappVerificado).toBe(true);
  });

  it('cualquier otro fallo sigue siendo un fallo', () => {
    let fallo: unknown;

    servicio.registro().subscribe({ error: (e) => (fallo = e) });

    const { body, init } = problema(403, 'ACCESO_DENEGADO');
    http.expectOne(`${BASE}/registro`).flush(body, init);
    expect(fallo).toBeInstanceOf(ErrorApi);
    expect((fallo as ErrorApi).codigo).toBe('ACCESO_DENEGADO');
  });
});
