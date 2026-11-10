import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AdminCatalogoService } from './admin-catalogo.service';

const BASE = '/api/catalogo/admin';
const VACIA = { contenido: [], pagina: 0, tamano: 10, totalElementos: 0, totalPaginas: 0 };

describe('AdminCatalogoService', () => {
  let servicio: AdminCatalogoService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servicio = TestBed.inject(AdminCatalogoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('pide las ofertas en moderación por páginas que empiezan en 0', () => {
    servicio.ofertasEnModeracion(2, 5).subscribe();

    const peticion = http.expectOne((p) => p.url === `${BASE}/moderacion/ofertas`);
    expect(peticion.request.method).toBe('GET');
    expect(peticion.request.params.get('page')).toBe('2');
    expect(peticion.request.params.get('size')).toBe('5');
    peticion.flush(VACIA);
  });

  it('pide las opiniones en moderación por páginas', () => {
    servicio.opinionesEnModeracion().subscribe();

    const peticion = http.expectOne((p) => p.url === `${BASE}/moderacion/opiniones`);
    expect(peticion.request.params.get('page')).toBe('0');
    expect(peticion.request.params.get('size')).toBe('10');
    peticion.flush(VACIA);
  });

  it('pide todas las zonas, también las inactivas', () => {
    servicio.zonas().subscribe();

    const peticion = http.expectOne(`${BASE}/zonas`);
    expect(peticion.request.method).toBe('GET');
    peticion.flush([]);
  });
});
