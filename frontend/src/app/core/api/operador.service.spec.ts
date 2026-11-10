import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AtencionRequest } from './modelos-nucleo';
import { OperadorService } from './operador.service';

const BASE = '/api/nucleo/operador';
const ID = '6f1c2a9e-4b7d-4c1e-9a52-3d8e0f7b1a20';
const VACIA = { contenido: [], pagina: 0, tamano: 20, totalElementos: 0, totalPaginas: 0 };

describe('OperadorService', () => {
  let servicio: OperadorService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servicio = TestBed.inject(OperadorService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('pide la cola sin estado: sus pendientes, por páginas que empiezan en 0', () => {
    servicio.cola(undefined, 1, 10).subscribe();

    const peticion = http.expectOne((p) => p.url === `${BASE}/solicitudes`);
    expect(peticion.request.method).toBe('GET');
    expect(peticion.request.params.has('estado')).toBe(false);
    expect(peticion.request.params.get('page')).toBe('1');
    expect(peticion.request.params.get('size')).toBe('10');
    peticion.flush(VACIA);
  });

  it('pide el historial con el parámetro estado', () => {
    servicio.cola('ATENDIDA').subscribe();

    const peticion = http.expectOne((p) => p.url === `${BASE}/solicitudes`);
    expect(peticion.request.params.get('estado')).toBe('ATENDIDA');
    expect(peticion.request.params.get('page')).toBe('0');
    expect(peticion.request.params.get('size')).toBe('20');
    peticion.flush(VACIA);
  });

  it('consulta una solicitud por su identificador', () => {
    servicio.consultar(ID).subscribe();

    const peticion = http.expectOne(`${BASE}/solicitudes/${ID}`);
    expect(peticion.request.method).toBe('GET');
    peticion.flush({ id: ID });
  });

  it('atiende con las condiciones y devuelve la solicitud con su nuevo estado', () => {
    const atencion: AtencionRequest = {
      resultado: 'ATENDIDA',
      precioBase: 300000,
      politicaCancelacion: 'Sin reembolso a menos de 24 horas del servicio',
      aparteAcordado: true,
    };
    let respuesta: unknown;

    servicio.atender(ID, atencion).subscribe((s) => (respuesta = s));

    const peticion = http.expectOne(`${BASE}/solicitudes/${ID}/atencion`);
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual(atencion);
    peticion.flush({ id: ID, estado: 'ATENDIDA' });
    expect(respuesta).toEqual({ id: ID, estado: 'ATENDIDA' });
  });

  it('marca la solicitud como no disponible con su motivo', () => {
    const atencion: AtencionRequest = { resultado: 'NO_DISPONIBLE', nota: 'Sin cupo esa semana' };

    servicio.atender(ID, atencion).subscribe();

    const peticion = http.expectOne(`${BASE}/solicitudes/${ID}/atencion`);
    expect(peticion.request.body).toEqual(atencion);
    peticion.flush({ id: ID, estado: 'NO_DISPONIBLE' });
  });

  it('pide los avisos por páginas', () => {
    servicio.avisos(2, 5).subscribe();

    const peticion = http.expectOne((p) => p.url === `${BASE}/avisos`);
    expect(peticion.request.method).toBe('GET');
    expect(peticion.request.params.get('page')).toBe('2');
    expect(peticion.request.params.get('size')).toBe('5');
    peticion.flush(VACIA);
  });
});
