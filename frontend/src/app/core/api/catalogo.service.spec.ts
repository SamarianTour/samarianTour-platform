import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CatalogoService } from './catalogo.service';
import { FichaOferta, PaginaOfertas, Zona } from './modelos';

describe('CatalogoService', () => {
  let servicio: CatalogoService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servicio = TestBed.inject(CatalogoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('busca ofertas con los filtros como query params', () => {
    const pagina: PaginaOfertas = { contenido: [], pagina: 1, tamano: 12, totalElementos: 0, totalPaginas: 0 };
    let recibida: PaginaOfertas | undefined;

    servicio
      .buscar({ tipo: 'ACTIVIDAD', precioMax: 150000, accesibleSillaRuedas: true, page: 1, size: 12 })
      .subscribe((respuesta) => (recibida = respuesta));

    const peticion = http.expectOne((candidata) => candidata.url === '/api/catalogo/ofertas');
    expect(peticion.request.method).toBe('GET');
    expect(peticion.request.params.get('tipo')).toBe('ACTIVIDAD');
    expect(peticion.request.params.get('precioMax')).toBe('150000');
    expect(peticion.request.params.get('accesibleSillaRuedas')).toBe('true');
    expect(peticion.request.params.get('page')).toBe('1');
    expect(peticion.request.params.get('size')).toBe('12');
    peticion.flush(pagina);
    expect(recibida).toEqual(pagina);
  });

  it('no envía los filtros sin valor', () => {
    servicio.buscar({ zonaId: '', tipo: undefined, page: 0, size: 12 }).subscribe();

    const peticion = http.expectOne((candidata) => candidata.url === '/api/catalogo/ofertas');
    expect(peticion.request.params.keys().sort()).toEqual(['page', 'size']);
    peticion.flush({ contenido: [], pagina: 0, tamano: 12, totalElementos: 0, totalPaginas: 0 });
  });

  it('pide la ficha de una oferta por su id', () => {
    let recibida: FichaOferta | undefined;

    servicio.ficha('b00000000000000000000001').subscribe((ficha) => (recibida = ficha));

    const peticion = http.expectOne('/api/catalogo/ofertas/b00000000000000000000001');
    expect(peticion.request.method).toBe('GET');
    peticion.flush({ id: 'b00000000000000000000001', nombre: 'Kayak en Taganga' });
    expect(recibida?.nombre).toBe('Kayak en Taganga');
  });

  it('pide las zonas', () => {
    let recibidas: Zona[] | undefined;

    servicio.zonas().subscribe((zonas) => (recibidas = zonas));

    const peticion = http.expectOne('/api/catalogo/zonas');
    expect(peticion.request.method).toBe('GET');
    peticion.flush([{ id: 'a00000000000000000000007', nombre: 'Minca' }]);
    expect(recibidas?.map((zona) => zona.nombre)).toEqual(['Minca']);
  });
});
