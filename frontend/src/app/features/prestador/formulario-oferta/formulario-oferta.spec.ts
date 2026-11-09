import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TipoPrestador } from '../../../core/api/modelos';
import { CLAVE_SESION, aSesionGuardada } from '../../../core/sesion/almacen-sesion';
import { respuestaDeSesion } from '../../../core/sesion/testing';
import { FormularioOferta, MENSAJE_SIN_TIPO } from './formulario-oferta';

describe('FormularioOferta (según la sesión)', () => {
  let fixture: ComponentFixture<FormularioOferta>;
  let http: HttpTestingController;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const texto = () => raiz().textContent ?? '';

  function crear(tipoPrestador?: TipoPrestador): void {
    const sesion = respuestaDeSesion({ rol: 'PRESTADOR', tipoPrestador });
    localStorage.setItem(CLAVE_SESION, JSON.stringify(aSesionGuardada(sesion)));
    fixture = TestBed.createComponent(FormularioOferta);
    fixture.detectChanges();
  }

  /** Responde lo que el formulario pide al abrirse: las zonas y las ofertas del prestador. */
  function responderCarga(): void {
    http.expectOne('/api/catalogo/zonas').flush([]);
    http
      .expectOne((p) => p.url === '/api/catalogo/prestador/ofertas')
      .flush({ contenido: [], pagina: 0, tamano: 100, totalElementos: 0, totalPaginas: 1 });
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

  it('un prestador sin RNT registrado no puede publicar: se le explica por qué, sin pedir nada al servidor', () => {
    crear();

    expect(texto()).toContain(MENSAJE_SIN_TIPO);
    expect(raiz().querySelector('form')).toBeNull();
    http.expectNone('/api/catalogo/zonas');
  });

  it('un prestador con tipo ve el formulario con el único tipo de oferta que su tipo publica', () => {
    crear('ALOJAMIENTO');

    responderCarga();

    expect(raiz().querySelector('form')).not.toBeNull();
    expect(texto()).toContain('Alojamiento');
    expect(texto()).toContain('el único que publica tu tipo de prestador');
  });

  it('un guía independiente debe completar los datos del recorrido', () => {
    crear('GUIA_INDEPENDIENTE');

    responderCarga();

    expect(texto()).toContain('Como guía independiente debes completar los datos del recorrido.');
  });

  it('una agencia puede elegir entre varios tipos de oferta', () => {
    crear('AGENCIA_OPERADORA');

    responderCarga();

    const tipos = Array.from(raiz().querySelectorAll<HTMLInputElement>('input[type="radio"]'));
    expect(tipos.length).toBeGreaterThan(1);
  });
});
