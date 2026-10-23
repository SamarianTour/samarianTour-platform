import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MiOpinion, OpinionesOferta } from './modelos';
import { OpinionesService } from './opiniones.service';

describe('OpinionesService', () => {
  let servicio: OpinionesService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servicio = TestBed.inject(OpinionesService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lee las opiniones aprobadas de una oferta con página y tamaño', () => {
    let recibidas: OpinionesOferta | undefined;

    servicio.deOferta('b00000000000000000000001', 1, 5).subscribe((respuesta) => (recibidas = respuesta));

    const peticion = http.expectOne((candidata) => candidata.url === '/api/catalogo/ofertas/b00000000000000000000001/opiniones');
    expect(peticion.request.method).toBe('GET');
    expect(peticion.request.params.get('page')).toBe('1');
    expect(peticion.request.params.get('size')).toBe('5');
    peticion.flush({
      resumen: { totalOpiniones: 3, promedioCalificacion: 4.7 },
      contenido: [],
      pagina: 1,
      tamano: 5,
      totalElementos: 3,
      totalPaginas: 1,
    });
    expect(recibidas?.resumen.promedioCalificacion).toBe(4.7);
  });

  it('publica la opinión del turista con POST en /turista/opiniones', () => {
    let creada: MiOpinion | undefined;
    const opinion = { ofertaId: 'b00000000000000000000002', calificacion: 5, comentario: 'Muy buen recorrido.' };

    servicio.publicar(opinion).subscribe((respuesta) => (creada = respuesta));

    const peticion = http.expectOne('/api/catalogo/turista/opiniones');
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual(opinion);
    peticion.flush({ id: 'd1', estado: 'EN_MODERACION' });
    expect(creada?.estado).toBe('EN_MODERACION');
  });

  it('lista las opiniones propias del turista', () => {
    servicio.mias(0, 10).subscribe();

    const peticion = http.expectOne((candidata) => candidata.url === '/api/catalogo/turista/opiniones');
    expect(peticion.request.method).toBe('GET');
    expect(peticion.request.params.get('size')).toBe('10');
    peticion.flush({ contenido: [], pagina: 0, tamano: 10, totalElementos: 0, totalPaginas: 0 });
  });
});
