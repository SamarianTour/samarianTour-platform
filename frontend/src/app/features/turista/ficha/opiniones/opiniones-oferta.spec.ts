import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Rol } from '../../../../core/api/modelos-nucleo';
import { CLAVE_SESION, aSesionGuardada } from '../../../../core/sesion/almacen-sesion';
import { respuestaDeSesion } from '../../../../core/sesion/testing';
import { OpinionesDeLaOferta } from './opiniones-oferta';

const OFERTA = 'b00000000000000000000001';
const URL_LECTURA = `/api/catalogo/ofertas/${OFERTA}/opiniones`;

describe('OpinionesDeLaOferta (según la sesión)', () => {
  let fixture: ComponentFixture<OpinionesDeLaOferta>;
  let http: HttpTestingController;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const texto = () => raiz().textContent ?? '';

  /** Abre la ficha con la sesión del rol dado, o sin sesión, y responde la lectura de las opiniones. */
  function crear(rol?: Rol): void {
    if (rol) {
      localStorage.setItem(
        CLAVE_SESION,
        JSON.stringify(aSesionGuardada(respuestaDeSesion({ rol }))),
      );
    }
    fixture = TestBed.createComponent(OpinionesDeLaOferta);
    fixture.componentRef.setInput('ofertaId', OFERTA);
    fixture.detectChanges();
    http
      .expectOne((p) => p.url === URL_LECTURA)
      .flush({
        resumen: { totalOpiniones: 0 },
        contenido: [],
        pagina: 0,
        tamano: 5,
        totalElementos: 0,
        totalPaginas: 0,
      });
    fixture.detectChanges();
  }

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('un turista con sesión puede dejar una opinión, firmada con su nombre', () => {
    crear('TURISTA');

    expect(texto()).toContain('Dejar una opinión como Laura');
    expect(raiz().querySelector('form.dejar-opinion')).not.toBeNull();
  });

  it('quien no ha iniciado sesión recibe una invitación a hacerlo, que vuelve a esta misma ficha', () => {
    vi.spyOn(TestBed.inject(Router), 'url', 'get').mockReturnValue(`/ofertas/${OFERTA}`);

    crear();

    const enlace = raiz().querySelector<HTMLAnchorElement>('a[href^="/iniciar-sesion"]')!;
    expect(enlace.textContent).toContain('inicia sesión');
    expect(decodeURIComponent(enlace.getAttribute('href')!)).toBe(
      `/iniciar-sesion?volver=/ofertas/${OFERTA}`,
    );
    expect(raiz().querySelector('form.dejar-opinion')).toBeNull();
    expect(texto()).not.toContain('Usuario demo');
  });

  it.each(['PRESTADOR', 'ADMINISTRADOR', 'OPERADOR_DISPONIBILIDAD', 'GESTOR_DESTINO'] as const)(
    'un %s no puede opinar y se le explica',
    (rol) => {
      crear(rol);

      expect(texto()).toContain('Solo los turistas pueden dejar una opinión');
      expect(raiz().querySelector('form.dejar-opinion')).toBeNull();
    },
  );
});
