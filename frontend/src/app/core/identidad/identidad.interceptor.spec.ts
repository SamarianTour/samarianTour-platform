import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { HEADER_ROL, HEADER_TIPO, HEADER_USUARIO, identidadInterceptor } from './identidad.interceptor';
import { IdentidadService } from './identidad.service';
import { PRESTADORES_DEMO } from './prestadores-demo';
import { TURISTAS_DEMO } from './turistas-demo';

describe('identidadInterceptor', () => {
  let cliente: HttpClient;
  let http: HttpTestingController;
  let identidad: IdentidadService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([identidadInterceptor])), provideHttpClientTesting()],
    });
    cliente = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
    identidad = TestBed.inject(IdentidadService);
  });

  afterEach(() => http.verify());

  it('agrega la identidad del prestador elegido a las rutas /prestador/**', () => {
    const guia = PRESTADORES_DEMO[1];
    identidad.elegir(guia.id);

    cliente.get('/api/catalogo/prestador/ofertas').subscribe();

    const peticion = http.expectOne('/api/catalogo/prestador/ofertas');
    expect(peticion.request.headers.get(HEADER_USUARIO)).toBe(guia.id);
    expect(peticion.request.headers.get(HEADER_ROL)).toBe('PRESTADOR');
    expect(peticion.request.headers.get(HEADER_TIPO)).toBe('GUIA_INDEPENDIENTE');
    peticion.flush({});
  });

  it('también la agrega al editar y al retirar una oferta', () => {
    cliente.patch('/api/catalogo/prestador/ofertas/b00000000000000000000001/retiro', null).subscribe();

    const peticion = http.expectOne('/api/catalogo/prestador/ofertas/b00000000000000000000001/retiro');
    expect(peticion.request.headers.get(HEADER_USUARIO)).toBe(PRESTADORES_DEMO[0].id);
    expect(peticion.request.headers.get(HEADER_ROL)).toBe('PRESTADOR');
    expect(peticion.request.headers.get(HEADER_TIPO)).toBe('AGENCIA_OPERADORA');
    peticion.flush(null);
  });

  it('no la agrega al catálogo público', () => {
    cliente.get('/api/catalogo/ofertas').subscribe();
    cliente.get('/api/catalogo/zonas').subscribe();

    for (const url of ['/api/catalogo/ofertas', '/api/catalogo/zonas']) {
      const peticion = http.expectOne(url);
      expect(peticion.request.headers.has(HEADER_USUARIO)).toBe(false);
      expect(peticion.request.headers.has(HEADER_ROL)).toBe(false);
      expect(peticion.request.headers.has(HEADER_TIPO)).toBe(false);
      peticion.flush({});
    }
  });

  it('no la agrega a una ruta que solo se parece a la del prestador', () => {
    cliente.get('/api/catalogo/prestadores').subscribe();

    const peticion = http.expectOne('/api/catalogo/prestadores');
    expect(peticion.request.headers.has(HEADER_USUARIO)).toBe(false);
    peticion.flush({});
  });

  it('recuerda el prestador elegido en localStorage', () => {
    const hotel = PRESTADORES_DEMO[2];

    identidad.elegir(hotel.id);

    expect(localStorage.getItem('samarian-tour.usuario-demo')).toBe(hotel.id);
    expect(identidad.prestador()).toEqual(hotel);
  });

  it('agrega la identidad del turista elegido a las rutas /turista/**, sin tipo de prestador', () => {
    const laura = TURISTAS_DEMO[0];
    identidad.elegir(laura.id);

    cliente.post('/api/catalogo/turista/opiniones', {}).subscribe();

    const peticion = http.expectOne('/api/catalogo/turista/opiniones');
    expect(peticion.request.headers.get(HEADER_USUARIO)).toBe(laura.id);
    expect(peticion.request.headers.get(HEADER_ROL)).toBe('TURISTA');
    expect(peticion.request.headers.has(HEADER_TIPO)).toBe(false);
    peticion.flush({});
  });

  it('no envía la identidad a una ruta de otro rol: el backend responde 401 y la pantalla lo explica', () => {
    identidad.elegir(TURISTAS_DEMO[1].id);
    cliente.get('/api/catalogo/prestador/ofertas').subscribe();
    const delPrestador = http.expectOne('/api/catalogo/prestador/ofertas');
    expect(delPrestador.request.headers.has(HEADER_USUARIO)).toBe(false);
    delPrestador.flush({});

    identidad.elegir(PRESTADORES_DEMO[0].id);
    cliente.get('/api/catalogo/turista/opiniones').subscribe();
    const delTurista = http.expectOne('/api/catalogo/turista/opiniones');
    expect(delTurista.request.headers.has(HEADER_USUARIO)).toBe(false);
    delTurista.flush({});
  });

  it('recuerda al turista elegido y expone a la vez quién es prestador y quién turista', () => {
    identidad.elegir(TURISTAS_DEMO[1].id);

    expect(localStorage.getItem('samarian-tour.usuario-demo')).toBe(TURISTAS_DEMO[1].id);
    expect(identidad.turista()).toEqual(TURISTAS_DEMO[1]);
    expect(identidad.prestador()).toBeNull();
    expect(TestBed.runInInjectionContext(() => new IdentidadService()).turista()).toEqual(TURISTAS_DEMO[1]);
  });
});
