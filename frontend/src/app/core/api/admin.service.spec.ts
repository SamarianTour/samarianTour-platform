import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AdminService } from './admin.service';

const BASE = '/api/nucleo/admin';

describe('AdminService', () => {
  let servicio: AdminService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servicio = TestBed.inject(AdminService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('pide todos los prestadores sin el parámetro sinOperador', () => {
    servicio.prestadores().subscribe();

    const peticion = http.expectOne((p) => p.url === `${BASE}/prestadores`);
    expect(peticion.request.method).toBe('GET');
    expect(peticion.request.params.has('sinOperador')).toBe(false);
    peticion.flush([]);
  });

  it('pide solo los que faltan por asignar con sinOperador=true', () => {
    servicio.prestadores(true).subscribe();

    const peticion = http.expectOne((p) => p.url === `${BASE}/prestadores`);
    expect(peticion.request.params.get('sinOperador')).toBe('true');
    peticion.flush([]);
  });

  it('pide las alertas de sesgo abiertas', () => {
    let alertas: unknown;

    servicio.alertasSesgo().subscribe((a) => (alertas = a));

    const peticion = http.expectOne(`${BASE}/alertas-sesgo`);
    expect(peticion.request.method).toBe('GET');
    peticion.flush([{ prestadorId: 'p1', indice: 3 }]);
    expect(alertas).toEqual([{ prestadorId: 'p1', indice: 3 }]);
  });
});
