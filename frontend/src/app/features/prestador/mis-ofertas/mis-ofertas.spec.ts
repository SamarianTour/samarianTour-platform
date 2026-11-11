import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { OfertaPrestador } from '../../../core/api/modelos';
import { CLAVE_SESION, aSesionGuardada } from '../../../core/sesion/almacen-sesion';
import { SesionService } from '../../../core/sesion/sesion.service';
import { respuestaDeSesion } from '../../../core/sesion/testing';
import { MisOfertas } from './mis-ofertas';

const URL = '/api/catalogo/prestador/ofertas';

const OFERTA: OfertaPrestador = {
  id: 'b00000000000000000000001',
  tipo: 'ACTIVIDAD',
  retirada: false,
  pendiente: {
    numero: 1,
    estadoModeracion: 'EN_MODERACION',
    enviadaEn: '2026-11-01T10:00:00Z',
    nombre: 'Caminata a Marinka',
    precioReferencia: 70000,
    unidadPrecio: 'por persona',
  },
};

describe('MisOfertas (según la sesión)', () => {
  let fixture: ComponentFixture<MisOfertas>;
  let http: HttpTestingController;

  const texto = () => (fixture.nativeElement as HTMLElement).textContent ?? '';

  function crear(): void {
    localStorage.setItem(
      CLAVE_SESION,
      JSON.stringify(
        aSesionGuardada(respuestaDeSesion({ rol: 'PRESTADOR', tipoPrestador: 'ALOJAMIENTO' })),
      ),
    );
    fixture = TestBed.createComponent(MisOfertas);
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

  it('pide las ofertas de la cuenta que tiene la sesión y las muestra', () => {
    crear();

    const peticion = http.expectOne((p) => p.url === URL);
    expect(peticion.request.params.get('page')).toBe('0');
    expect(peticion.request.params.get('size')).toBe('10');
    peticion.flush({
      contenido: [OFERTA],
      pagina: 0,
      tamano: 10,
      totalElementos: 1,
      totalPaginas: 1,
    });
    fixture.detectChanges();

    expect(texto()).toContain('Caminata a Marinka');
  });

  it('el precio lleva la unidad que entrega la API, no la que correspondería al tipo de oferta', () => {
    crear();
    http.expectOne((p) => p.url === URL).flush({
      contenido: [{ ...OFERTA, pendiente: { ...OFERTA.pendiente, unidadPrecio: 'por noche y unidad' } }],
      pagina: 0,
      tamano: 10,
      totalElementos: 1,
      totalPaginas: 1,
    });
    fixture.detectChanges();

    expect(texto()).toContain('por noche y unidad');
  });

  it('editar los datos personales no vuelve a pedir la lista', () => {
    crear();
    http
      .expectOne((p) => p.url === URL)
      .flush({ contenido: [], pagina: 0, tamano: 10, totalElementos: 0, totalPaginas: 1 });
    const sesion = TestBed.inject(SesionService);

    sesion.actualizarCuenta({ ...sesion.cuenta()!, nombre: 'Otro nombre' });
    fixture.detectChanges();

    http.expectNone((p) => p.url === URL);
  });
});
