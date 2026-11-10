import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Rol } from '../../../core/api/modelos-nucleo';
import { establecerIdioma } from '../../../core/idioma/idioma';
import { CLAVE_SESION, aSesionGuardada } from '../../../core/sesion/almacen-sesion';
import { respuestaDeSesion } from '../../../core/sesion/testing';
import { Ficha } from './ficha';

const FICHA = {
  id: 'b00000000000000000000001',
  tipo: 'ACTIVIDAD',
  nombre: 'Caminata a Marinka',
  descripcion: 'Un recorrido.',
  precioReferencia: 70000,
  unidadPrecio: 'por persona',
  capacidadMaxima: 10,
  accesibleSillaRuedas: false,
  fotos: [],
  actividad: { tipoActividad: 'SENDERISMO', nivelEsfuerzo: 'BAJO', horariosSalida: ['07:00'] },
};

/** El botón "Solicitar disponibilidad" de la ficha de una oferta (P02.1). */
describe('Ficha: botón "Solicitar disponibilidad"', () => {
  let fixture: ComponentFixture<Ficha>;
  let http: HttpTestingController;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const boton = () =>
    raiz().querySelector<HTMLAnchorElement>('.resumen a[href^="/turista/solicitar"]');
  const lateral = () => raiz().querySelector('.resumen')?.textContent ?? '';

  function crear(rol?: Rol): void {
    if (rol) {
      localStorage.setItem(
        CLAVE_SESION,
        JSON.stringify(aSesionGuardada(respuestaDeSesion({ rol }))),
      );
    }
    fixture = TestBed.createComponent(Ficha);
    fixture.componentRef.setInput('id', FICHA.id);
    fixture.detectChanges();
    http.expectOne(`/api/catalogo/ofertas/${FICHA.id}`).flush(FICHA);
    fixture.detectChanges();
    // La ficha también pide las opiniones de la oferta.
    http
      .match((p) => p.url.includes('/opiniones'))
      .forEach((p) =>
        p.flush({
          resumen: { totalOpiniones: 0 },
          contenido: [],
          pagina: 0,
          tamano: 5,
          totalElementos: 0,
          totalPaginas: 0,
        }),
      );
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

  it('un turista lo ve y lleva al formulario de esa oferta', () => {
    crear('TURISTA');

    expect(boton()?.textContent).toContain('Solicitar disponibilidad');
    expect(boton()?.getAttribute('href')).toBe(`/turista/solicitar/${FICHA.id}`);
    expect(lateral()).not.toContain('Te pediremos iniciar sesión.');
  });

  it('sin sesión también lo ve, con el aviso de que se le pedirá iniciar sesión', () => {
    crear();

    expect(boton()?.getAttribute('href')).toBe(`/turista/solicitar/${FICHA.id}`);
    expect(lateral()).toContain('Te pediremos iniciar sesión.');
  });

  it.each(['PRESTADOR', 'ADMINISTRADOR', 'OPERADOR_DISPONIBILIDAD', 'GESTOR_DESTINO'] as const)(
    'un %s no lo ve y se le explica que solo piden los turistas',
    (rol) => {
      crear(rol);

      expect(boton()).toBeNull();
      expect(lateral()).toContain('Solo los turistas pueden solicitar disponibilidad.');
    },
  );

  it('cambia de idioma al instante', () => {
    crear();

    establecerIdioma('en', { recordar: false });
    fixture.detectChanges();

    expect(boton()?.textContent).toContain('Request availability');
    expect(lateral()).toContain("We'll ask you to log in.");
  });
});
