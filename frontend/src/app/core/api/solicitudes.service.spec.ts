import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { EnviarSolicitudRequest } from './modelos-nucleo';
import { SolicitudesService } from './solicitudes.service';

const BASE = '/api/nucleo/turista/solicitudes';
const CLAVE = '3d6f9a0e-2b1c-4e5f-8a7b-9c0d1e2f3a4b';

describe('SolicitudesService', () => {
  let servicio: SolicitudesService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servicio = TestBed.inject(SolicitudesService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('envía la solicitud con su clave de idempotencia', () => {
    const solicitud: EnviarSolicitudRequest = {
      ofertaId: 'b1',
      fechaInicio: '2026-10-28',
      horaSalida: '07:00',
      personas: 2,
    };
    let creada: unknown;

    servicio.enviar(solicitud, CLAVE).subscribe((s) => (creada = s));

    const peticion = http.expectOne(BASE);
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual(solicitud);
    expect(peticion.request.headers.get('Idempotency-Key')).toBe(CLAVE);
    peticion.flush({ id: 's1', estado: 'PENDIENTE' });
    expect(creada).toEqual({ id: 's1', estado: 'PENDIENTE' });
  });

  it('lista por páginas que empiezan en 0', () => {
    servicio.listar(2, 10).subscribe();

    const peticion = http.expectOne((p) => p.url === BASE);
    expect(peticion.request.method).toBe('GET');
    expect(peticion.request.params.get('page')).toBe('2');
    expect(peticion.request.params.get('size')).toBe('10');
    peticion.flush({ contenido: [], pagina: 2, tamano: 10, totalElementos: 0, totalPaginas: 0 });
  });

  it('sin indicarlo, pide la primera página de 20', () => {
    servicio.listar().subscribe();

    const peticion = http.expectOne((p) => p.url === BASE);
    expect(peticion.request.params.get('page')).toBe('0');
    expect(peticion.request.params.get('size')).toBe('20');
    peticion.flush({});
  });

  it('consulta una solicitud por su id', () => {
    servicio.consultar('6f1c2a9e-4b7d-4c1e-9a52-3d8e0f7b1a20').subscribe();

    const peticion = http.expectOne(`${BASE}/6f1c2a9e-4b7d-4c1e-9a52-3d8e0f7b1a20`);
    expect(peticion.request.method).toBe('GET');
    peticion.flush({});
  });

  it('codifica el id para que no pueda cambiar la ruta', () => {
    servicio.consultar('a/../../admin').subscribe();

    http.expectOne(`${BASE}/a%2F..%2F..%2Fadmin`).flush({});
  });

  it.each(['CONFIRMAR', 'DECLINAR'] as const)('responde las condiciones: %s', (decision) => {
    servicio.responder('s1', decision, CLAVE).subscribe();

    const peticion = http.expectOne(`${BASE}/s1/respuesta`);
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual({ decision });
    expect(peticion.request.headers.get('Idempotency-Key')).toBe(CLAVE);
    peticion.flush({});
  });
});
