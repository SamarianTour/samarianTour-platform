import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { OfertaPrestador, OfertaRequest, PaginaOfertasPrestador } from './modelos';
import { OfertasPrestadorService } from './ofertas-prestador.service';

const URL = '/api/catalogo/prestador/ofertas';

function oferta(id: string): OfertaPrestador {
  return {
    id,
    tipo: 'ACTIVIDAD',
    retirada: false,
    pendiente: {
      numero: 1,
      estadoModeracion: 'EN_MODERACION',
      enviadaEn: '2026-10-01T10:00:00Z',
      nombre: `Oferta ${id}`,
      precioReferencia: 90000,
      unidadPrecio: 'por persona',
    },
  };
}

function pagina(contenido: OfertaPrestador[], numero: number, totalPaginas: number): PaginaOfertasPrestador {
  return { contenido, pagina: numero, tamano: 100, totalElementos: 3, totalPaginas };
}

const PETICION: OfertaRequest = {
  tipo: 'RESTAURANTE',
  nombre: 'Cocina de mar',
  descripcion: 'Pesca del día en Taganga.',
  precioReferencia: 45000,
  zonaId: 'a00000000000000000000002',
  capacidadMaxima: 8,
  fotos: ['3f2504e0-4f89-11d3-9a0c-0305e82c3301'],
  accesibleSillaRuedas: true,
};

describe('OfertasPrestadorService', () => {
  let servicio: OfertasPrestadorService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servicio = TestBed.inject(OfertasPrestadorService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lista las ofertas propias con página y tamaño', () => {
    servicio.listar(2, 10).subscribe();

    const peticion = http.expectOne((candidata) => candidata.url === URL);
    expect(peticion.request.method).toBe('GET');
    expect(peticion.request.params.get('page')).toBe('2');
    expect(peticion.request.params.get('size')).toBe('10');
    peticion.flush(pagina([], 2, 3));
  });

  it('reúne todas las ofertas recorriendo las páginas', () => {
    let recibidas: OfertaPrestador[] | undefined;

    servicio.todas().subscribe((ofertas) => (recibidas = ofertas));

    http
      .expectOne((candidata) => candidata.url === URL && candidata.params.get('page') === '0')
      .flush(pagina([oferta('1'), oferta('2')], 0, 2));
    http
      .expectOne((candidata) => candidata.url === URL && candidata.params.get('page') === '1')
      .flush(pagina([oferta('3')], 1, 2));
    expect(recibidas?.map((propia) => propia.id)).toEqual(['1', '2', '3']);
  });

  it('busca una oferta propia por id en la lista', () => {
    let encontrada: OfertaPrestador | undefined;
    let ajena: OfertaPrestador | undefined = oferta('otra');

    servicio.buscarPropia('2').subscribe((propia) => (encontrada = propia));
    http.expectOne((candidata) => candidata.url === URL).flush(pagina([oferta('1'), oferta('2')], 0, 1));
    servicio.buscarPropia('9').subscribe((propia) => (ajena = propia));
    http.expectOne((candidata) => candidata.url === URL).flush(pagina([oferta('1'), oferta('2')], 0, 1));

    expect(encontrada?.id).toBe('2');
    expect(ajena).toBeUndefined();
  });

  it('publica una oferta con POST', () => {
    let creada: OfertaPrestador | undefined;

    servicio.publicar(PETICION).subscribe((propia) => (creada = propia));

    const peticion = http.expectOne(URL);
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual(PETICION);
    peticion.flush(oferta('nueva'));
    expect(creada?.id).toBe('nueva');
  });

  it('edita una oferta con PUT sobre su id', () => {
    servicio.editar('b00000000000000000000001', PETICION).subscribe();

    const peticion = http.expectOne(`${URL}/b00000000000000000000001`);
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual(PETICION);
    peticion.flush(oferta('b00000000000000000000001'));
  });

  it('retira una oferta con PATCH sobre /retiro', () => {
    let termino = false;

    servicio.retirar('b00000000000000000000001').subscribe(() => (termino = true));

    const peticion = http.expectOne(`${URL}/b00000000000000000000001/retiro`);
    expect(peticion.request.method).toBe('PATCH');
    peticion.flush(null, { status: 204, statusText: 'No Content' });
    expect(termino).toBe(true);
  });
});
