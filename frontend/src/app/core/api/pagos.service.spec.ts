import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { SesionCobro } from './modelos-nucleo';
import { PagosService } from './pagos.service';

const ID = '6f1c2a9e-4b7d-4c1e-9a52-3d8e0f7b1a20';
const BASE = `/api/nucleo/turista/solicitudes/${ID}`;
const CLAVE = '3d6f9a0e-2b1c-4e5f-8a7b-9c0d1e2f3a4b';

const SESION: SesionCobro = {
  intentoId: '2b7e3f10-5c4d-4a8e-9f61-0d2c3b4a5e6f',
  solicitudId: ID,
  numero: 1,
  estado: 'EN_CURSO',
  referencia: 'ST-2b7e3f10-5c4d-4a8e-9f61-0d2c3b4a5e6f',
  montoCentavos: 30000000,
  moneda: 'COP',
  creadoEn: '2026-10-26T17:05:00Z',
  checkout: {
    pasarela: 'SIMULADA',
    url: 'http://localhost:8080/api/nucleo/pasarela-simulada/ST-2b7e',
    urlRedireccion: `http://localhost:8080/turista/solicitudes/${ID}`,
  },
};

describe('PagosService', () => {
  let servicio: PagosService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servicio = TestBed.inject(PagosService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('abre el pago con la clave de idempotencia y sin cuerpo: no viajan datos de tarjeta (RF-47)', () => {
    let sesion: SesionCobro | undefined;

    servicio.abrir(ID, CLAVE).subscribe((s) => (sesion = s));

    const peticion = http.expectOne(`${BASE}/pago`);
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toBeNull();
    expect(peticion.request.headers.get('Idempotency-Key')).toBe(CLAVE);
    peticion.flush(SESION);
    expect(sesion).toEqual(SESION);
  });

  it('consulta los intentos de pago de la solicitud', () => {
    let intentos: unknown;

    servicio.intentos(ID).subscribe((i) => (intentos = i));

    const peticion = http.expectOne(`${BASE}/pagos`);
    expect(peticion.request.method).toBe('GET');
    peticion.flush([{ id: 'i1', numero: 1, estado: 'RECHAZADO' }]);
    expect(intentos).toEqual([{ id: 'i1', numero: 1, estado: 'RECHAZADO' }]);
  });

  it('codifica el identificador en la dirección', () => {
    servicio.intentos('a/b').subscribe();

    http.expectOne('/api/nucleo/turista/solicitudes/a%2Fb/pagos').flush([]);
  });
});
