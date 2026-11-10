import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, TestRequest, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Alternativas, Recomendacion } from '../../../../core/api/modelos-recomendacion';
import { erroresInterceptor } from '../../../../core/errores/errores.interceptor';
import { establecerIdioma } from '../../../../core/idioma/idioma';
import { AlternativasOferta, alternativasVisibles } from './alternativas-oferta';

const URL = (id: string) => `/api/recomendacion/ofertas/${id}/alternativas`;

function alternativa(posicion: number, nombre: string): Recomendacion {
  return {
    posicion,
    oferta: {
      id: `b${posicion}`,
      nombre,
      tipo: 'ACTIVIDAD',
      zonaNombre: 'Minca y Sierra Nevada',
      precioReferencia: 95000,
      unidadPrecio: 'por persona',
    },
    criterios: ['TIPO_OFERTA', 'NIVEL_ESFUERZO', 'PRECIO'],
    explicacion: `Mismo tipo de oferta que ${nombre}, en una zona con menos visitantes.`,
  };
}

const TRES: Alternativas = {
  aplica: true,
  zonaOrigen: { id: 'z1', nombre: 'Parque Tayrona' },
  alternativas: [alternativa(1, 'Pozo Azul'), alternativa(2, 'Aves en Minca'), alternativa(3, 'Tubing')],
};

function problema(status: number, codigo: string) {
  return {
    body: { status, codigo, title: codigo, detail: codigo },
    init: { status, statusText: codigo, headers: { 'Content-Type': 'application/problem+json' } },
  };
}

const normal = (texto: string | null | undefined) => (texto ?? '').replace(/[  ]/g, ' ').replace(/\s+/g, ' ').trim();

describe('alternativasVisibles', () => {
  it('muestra las alternativas cuando aplica y hay alguna', () => {
    expect(alternativasVisibles(TRES)).toBe(TRES);
  });

  it('no muestra nada si la zona no es de alta afluencia (aplica: false)', () => {
    expect(alternativasVisibles({ aplica: false, alternativas: [] })).toBeNull();
  });

  it('no muestra una sección vacía aunque aplique', () => {
    expect(alternativasVisibles({ aplica: true, zonaOrigen: { id: 'z1' }, alternativas: [] })).toBeNull();
  });

  it('con pocas coincidencias muestra las que haya', () => {
    const una: Alternativas = { aplica: true, aviso: 'POCAS_COINCIDENCIAS', alternativas: [alternativa(1, 'Minca: aves y café')] };

    expect(alternativasVisibles(una)).toBe(una);
  });
});

describe('AlternativasOferta (P08.3)', () => {
  let fixture: ComponentFixture<AlternativasOferta>;
  let http: HttpTestingController;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const seccion = () => raiz().querySelector('section');
  const tarjetas = () => Array.from(raiz().querySelectorAll('app-tarjeta-recomendada'));

  function crear(ofertaId = 'o1'): TestRequest {
    fixture = TestBed.createComponent(AlternativasOferta);
    fixture.componentRef.setInput('ofertaId', ofertaId);
    fixture.detectChanges();
    return http.expectOne((p) => p.url === URL(ofertaId));
  }

  function responder(peticion: TestRequest, datos: Alternativas = TRES): void {
    peticion.flush(datos);
    fixture.detectChanges();
  }

  beforeEach(() => {
    establecerIdioma('es', { recordar: false });
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([erroresInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    vi.useRealTimers();
    establecerIdioma('es', { recordar: false });
    http.verify();
  });

  it('pide las alternativas de la oferta, con el idioma, y mientras consulta no ocupa lugar', () => {
    const peticion = crear('o1');

    expect(peticion.request.method).toBe('GET');
    expect(peticion.request.params.get('idioma')).toBe('es');
    expect(seccion()).toBeNull();
    expect(normal(raiz().textContent)).toBe('');
    responder(peticion);
  });

  describe('con alternativas', () => {
    it('explica de qué zona se aleja la oferta y muestra cada alternativa con su explicación', () => {
      responder(crear());

      expect(raiz().querySelector('h2')?.textContent).toBe('Alternativas con menos visitantes');
      expect(seccion()?.getAttribute('aria-labelledby')).toBe('alternativas-titulo');
      expect(normal(raiz().querySelector('.alternativas__intro')?.textContent)).toBe(
        'Esta oferta está en Parque Tayrona, una zona con alta afluencia. Estas ofertas parecidas están en zonas con menos visitantes.',
      );
      expect(tarjetas()).toHaveLength(3);
      expect(tarjetas()[0].textContent).toContain('Mismo tipo de oferta que Pozo Azul');
      expect(tarjetas()[1].querySelector('h3 a')?.getAttribute('href')).toBe('/ofertas/b2');
    });

    it('si el catálogo no dio el nombre de la zona, no lo inventa', () => {
      responder(crear(), { ...TRES, zonaOrigen: { id: 'z1' } });

      expect(normal(raiz().querySelector('.alternativas__intro')?.textContent)).toBe(
        'Esta oferta está en una zona con alta afluencia. Estas ofertas parecidas están en zonas con menos visitantes.',
      );
    });

    it('dice que son sugerencias por criterios explícitos y no publicidad', () => {
      responder(crear());

      expect(raiz().querySelector('.alternativas__nota')?.textContent).toContain('no publicidad');
    });

    it('sin el aviso de pocas coincidencias cuando hay suficientes', () => {
      responder(crear());

      expect(raiz().querySelector('.alerta--aviso')).toBeNull();
    });
  });

  describe('pocas coincidencias', () => {
    it('muestra las que haya con el aviso', () => {
      responder(crear(), { aplica: true, aviso: 'POCAS_COINCIDENCIAS', zonaOrigen: { id: 'z1', nombre: 'Taganga' }, alternativas: [alternativa(1, 'Minca: aves y café')] });

      expect(tarjetas()).toHaveLength(1);
      expect(raiz().querySelector('.alerta--aviso')?.textContent).toContain('Encontramos pocas ofertas parecidas');
    });
  });

  describe('cuándo se oculta la sección (degradación), sin avisos ni errores', () => {
    it('si la zona no es de alta afluencia (aplica: false)', () => {
      responder(crear(), { aplica: false, alternativas: [] });

      expect(seccion()).toBeNull();
    });

    it('si no hay ninguna alternativa', () => {
      responder(crear(), { aplica: true, zonaOrigen: { id: 'z1' }, alternativas: [] });

      expect(seccion()).toBeNull();
    });

    it.each([502, 503, 504])('con un %s', (estado) => {
      const peticion = crear();

      const { body, init } = problema(estado, 'RECOMENDACION_NO_DISPONIBLE');
      peticion.flush(body, init);
      fixture.detectChanges();

      expect(seccion()).toBeNull();
      expect(raiz().querySelector('[role="alert"]')).toBeNull();
      expect(normal(raiz().textContent)).toBe('');
    });

    it('si la oferta no existe en el servicio (404)', () => {
      const peticion = crear();

      const { body, init } = problema(404, 'OFERTA_NO_ENCONTRADA');
      peticion.flush(body, init);
      fixture.detectChanges();

      expect(seccion()).toBeNull();
    });

    it('sin conexión', () => {
      crear().error(new ProgressEvent('error'));
      fixture.detectChanges();

      expect(seccion()).toBeNull();
    });

    it('a los 3 s sin respuesta, y cancela la consulta', () => {
      vi.useFakeTimers();
      const peticion = crear();

      vi.advanceTimersByTime(2_999);
      expect(peticion.cancelled).toBe(false);
      vi.advanceTimersByTime(1);
      fixture.detectChanges();

      expect(peticion.cancelled).toBe(true);
      expect(seccion()).toBeNull();
    });

    it('una respuesta que llega a los 2,9 s todavía se muestra', () => {
      vi.useFakeTimers();
      const peticion = crear();

      vi.advanceTimersByTime(2_900);
      responder(peticion);

      expect(tarjetas()).toHaveLength(3);
    });
  });

  it('al pasar a otra oferta se pide la nueva y no se queda lo de la anterior', () => {
    responder(crear('o1'));
    expect(tarjetas()).toHaveLength(3);

    fixture.componentRef.setInput('ofertaId', 'o2');
    fixture.detectChanges();

    expect(seccion()).toBeNull();
    responder(http.expectOne((p) => p.url === URL('o2')), { aplica: false, alternativas: [] });
    expect(seccion()).toBeNull();
  });

  it('en inglés pide las explicaciones en inglés y traduce el resto', () => {
    establecerIdioma('en', { recordar: false });
    const peticion = crear();
    expect(peticion.request.params.get('idioma')).toBe('en');

    responder(peticion);

    expect(raiz().querySelector('h2')?.textContent).toBe('Alternatives with fewer visitors');
    expect(normal(raiz().querySelector('.alternativas__intro')?.textContent)).toBe(
      'This offer is in Parque Tayrona, an area with high visitor traffic. These similar offers are in areas with fewer visitors.',
    );
    expect(raiz().querySelector('.alternativas__nota')?.textContent).toContain('not advertising');
  });
});
