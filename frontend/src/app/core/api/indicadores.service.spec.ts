import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ErrorApi } from '../errores/error-api';
import { erroresInterceptor } from '../errores/errores.interceptor';
import { IndicadoresService } from './indicadores.service';

const BASE = '/api/nucleo/gestor/indicadores';
const PERIODO = { desde: '2026-10-01', hasta: '2026-10-31' };

describe('IndicadoresService', () => {
  let servicio: IndicadoresService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([erroresInterceptor])), provideHttpClientTesting()],
    });
    servicio = TestBed.inject(IndicadoresService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('pide la demanda del periodo con GET y sus dos fechas', () => {
    let recibido: unknown;
    const tablero = { ...PERIODO, total: 42, filas: [{ zonaId: 'tayrona', tipoOferta: 'ACTIVIDAD', solicitudes: 18 }] };

    servicio.tablero(PERIODO).subscribe((t) => (recibido = t));

    const peticion = http.expectOne((p) => p.url === `${BASE}/tablero`);
    expect(peticion.request.method).toBe('GET');
    expect(peticion.request.params.get('desde')).toBe('2026-10-01');
    expect(peticion.request.params.get('hasta')).toBe('2026-10-31');
    peticion.flush(tablero);
    expect(recibido).toEqual(tablero);
  });

  it('pide el embudo del periodo', () => {
    servicio.embudo(PERIODO).subscribe();

    const peticion = http.expectOne((p) => p.url === `${BASE}/embudo`);
    expect(peticion.request.method).toBe('GET');
    expect(peticion.request.params.get('desde')).toBe('2026-10-01');
    expect(peticion.request.params.get('hasta')).toBe('2026-10-31');
    peticion.flush({ ...PERIODO, etapas: [], porTipo: [] });
  });

  it('pide la distribución por categoría del periodo', () => {
    servicio.categorias(PERIODO).subscribe();

    const peticion = http.expectOne((p) => p.url === `${BASE}/categorias`);
    expect(peticion.request.method).toBe('GET');
    expect(peticion.request.params.get('desde')).toBe('2026-10-01');
    expect(peticion.request.params.get('hasta')).toBe('2026-10-31');
    peticion.flush({ ...PERIODO, categorias: [] });
  });

  it('un periodo que el núcleo rechaza llega como un error de la API', () => {
    let fallo: unknown;

    servicio.tablero(PERIODO).subscribe({ error: (e) => (fallo = e) });

    http.expectOne((p) => p.url === `${BASE}/tablero`).flush(
      { status: 400, codigo: 'DATO_INVALIDO', title: 'x', detail: 'hasta: no puede ser anterior a desde' },
      { status: 400, statusText: 'Bad Request', headers: { 'Content-Type': 'application/problem+json' } },
    );
    expect(fallo).toBeInstanceOf(ErrorApi);
    expect((fallo as ErrorApi).codigo).toBe('DATO_INVALIDO');
  });
});
