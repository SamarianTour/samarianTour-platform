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

  it('registra una zona con POST', () => {
    let zona: unknown;
    const cuerpo = {
      nombre: 'Isla de Salamanca',
      descripcion: 'Vía Parque entre Barranquilla y Ciénaga',
      ubicacion: { latitud: 10.98, longitud: -74.47 },
      areaProtegida: true,
      altaAfluencia: false,
      buenasPracticas: ['No alimentar a los animales'],
    };

    servicio.registrarZona(cuerpo).subscribe((z) => (zona = z));

    const peticion = http.expectOne(`${BASE}/zonas`);
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual(cuerpo);
    peticion.flush({ id: 'z8', nombre: 'Isla de Salamanca', activa: true });
    expect(zona).toMatchObject({ id: 'z8', activa: true });
  });

  it('edita una zona con PUT, reemplazando todos sus datos e indicando si está activa', () => {
    const cuerpo = {
      nombre: 'Taganga',
      descripcion: 'Bahía de pescadores',
      ubicacion: { latitud: 11.27, longitud: -74.19 },
      areaProtegida: false,
      altaAfluencia: true,
      buenasPracticas: [],
      activa: false,
    };

    servicio.editarZona('a05', cuerpo).subscribe();

    const peticion = http.expectOne(`${BASE}/zonas/a05`);
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual(cuerpo);
    peticion.flush({ id: 'a05', activa: false });
  });

  it('codifica el identificador de la zona en la dirección', () => {
    servicio
      .editarZona('a/b', {
        nombre: 'x',
        descripcion: 'y',
        ubicacion: { latitud: 0, longitud: 0 },
        areaProtegida: false,
        altaAfluencia: false,
        activa: true,
      })
      .subscribe();

    http.expectOne(`${BASE}/zonas/a%2Fb`).flush({});
  });
});
