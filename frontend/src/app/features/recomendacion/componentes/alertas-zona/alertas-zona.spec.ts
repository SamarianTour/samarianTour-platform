import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, TestRequest, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AlertasZona as AlertasDeLaZona } from '../../../../core/api/modelos-recomendacion';
import { erroresInterceptor } from '../../../../core/errores/errores.interceptor';
import { establecerIdioma } from '../../../../core/idioma/idioma';
import { AlertasZona } from './alertas-zona';

const URL = (zonaId: string) => `/api/recomendacion/zonas/${zonaId}/alertas`;

const CON_ALERTAS: AlertasDeLaZona = {
  zonaId: 'a1',
  actualizadoEn: '2026-10-24T23:42:10.512Z',
  alertas: [
    {
      id: 'h1',
      tipo: 'HIDROLOGICA',
      nivel: 'AMARILLA',
      titulo: 'Alerta hidrológica en la subzona Río Piedras',
      descripcion: 'Consulta las indicaciones de las autoridades.',
      fuente: 'IDEAM, alertas hidrológicas vigentes',
    },
    { id: 's1', tipo: 'CIERRE', nivel: 'ROJA', titulo: 'Cierre temporal', fuente: 'Parques Nacionales (simulado)' },
  ],
};

const SIN_ALERTAS: AlertasDeLaZona = { zonaId: 'a1', actualizadoEn: '2026-10-24T23:42:10.512Z', alertas: [] };

function problema(status: number, codigo: string) {
  return {
    body: { status, codigo, title: codigo, detail: codigo },
    init: { status, statusText: codigo, headers: { 'Content-Type': 'application/problem+json' } },
  };
}

const normal = (texto: string | null | undefined) => (texto ?? '').replace(/[  ]/g, ' ').replace(/\s+/g, ' ').trim();

describe('AlertasZona (sección de la ficha, P08.2)', () => {
  let fixture: ComponentFixture<AlertasZona>;
  let http: HttpTestingController;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const seccion = () => raiz().querySelector('section');
  const tarjetas = () => Array.from(raiz().querySelectorAll('app-tarjeta-alerta'));

  function crear(zonaId = 'a1'): TestRequest {
    fixture = TestBed.createComponent(AlertasZona);
    fixture.componentRef.setInput('zonaId', zonaId);
    fixture.detectChanges();
    return http.expectOne((p) => p.url === URL(zonaId));
  }

  function responder(peticion: TestRequest, datos: AlertasDeLaZona = CON_ALERTAS): void {
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

  it('pide las alertas de la zona de la oferta, con el idioma', () => {
    const peticion = crear('a1');

    expect(peticion.request.method).toBe('GET');
    expect(peticion.request.params.get('idioma')).toBe('es');
    responder(peticion);
  });

  it('mientras consulta no ocupa lugar: la ficha no se mueve al llegar', () => {
    const peticion = crear();

    expect(seccion()).toBeNull();
    expect(normal(raiz().textContent)).toBe('');
    responder(peticion);
  });

  describe('con alertas', () => {
    it('muestra cada alerta con su nivel, su fuente y la hora de actualización', () => {
      responder(crear());

      expect(raiz().querySelector('h2')?.textContent).toBe('Alertas del destino');
      expect(seccion()?.getAttribute('aria-labelledby')).toBe('alertas-zona-titulo');
      expect(tarjetas()).toHaveLength(2);
      expect(tarjetas()[0].textContent).toContain('IDEAM, alertas hidrológicas vigentes');
      expect(tarjetas()[1].textContent).toContain('Parques Nacionales (simulado)');
      expect(normal(raiz().querySelector('.alertas-zona__actualizado')?.textContent)).toMatch(/^Actualizado: 24 oct 2026, \d{1,2}:42 [ap]\. ?m\./);
      expect(raiz().querySelector('time')?.getAttribute('datetime')).toBe('2026-10-24T23:42:10.512Z');
    });

    it('lleva a las alertas de todo el destino', () => {
      responder(crear());

      const enlace = raiz().querySelector<HTMLAnchorElement>('a[href="/alertas"]');
      expect(enlace?.textContent?.trim()).toBe('Ver las alertas de todas las zonas');
    });
  });

  describe('sin alertas', () => {
    it('dice "Sin alertas activas" con la hora de actualización y sigue ofreciendo el enlace', () => {
      responder(crear(), SIN_ALERTAS);

      expect(normal(raiz().querySelector('.alertas-zona__sin')?.textContent)).toBe('Sin alertas activas No hay avisos vigentes para esta zona.');
      expect(raiz().querySelector('.alertas-zona__actualizado time')).not.toBeNull();
      expect(tarjetas()).toHaveLength(0);
      expect(raiz().querySelector('a[href="/alertas"]')).not.toBeNull();
    });
  });

  describe('si las alertas no están disponibles (degradación): la sección se oculta, sin avisos', () => {
    it.each([502, 503, 504])('un %s oculta la sección', (estado) => {
      const peticion = crear();

      const { body, init } = problema(estado, 'ALERTAS_NO_DISPONIBLES');
      peticion.flush(body, init);
      fixture.detectChanges();

      expect(seccion()).toBeNull();
      expect(raiz().querySelector('[role="alert"]')).toBeNull();
      expect(normal(raiz().textContent)).toBe('');
    });

    it('sin conexión oculta la sección', () => {
      const peticion = crear();

      peticion.error(new ProgressEvent('error'));
      fixture.detectChanges();

      expect(seccion()).toBeNull();
    });

    it('un error propio de la consulta también la oculta: la ficha no es el lugar para explicarlo', () => {
      const peticion = crear();

      const { body, init } = problema(400, 'DATO_INVALIDO');
      peticion.flush(body, init);
      fixture.detectChanges();

      expect(seccion()).toBeNull();
    });

    it('a los 3 s sin respuesta la oculta y cancela la consulta', () => {
      vi.useFakeTimers();
      const peticion = crear();

      vi.advanceTimersByTime(2_999);
      expect(peticion.cancelled).toBe(false);

      vi.advanceTimersByTime(1);
      fixture.detectChanges();

      expect(peticion.cancelled).toBe(true);
      expect(seccion()).toBeNull();
    });

    it('una respuesta que llega justo a tiempo (2,9 s) sí se muestra', () => {
      vi.useFakeTimers();
      const peticion = crear();

      vi.advanceTimersByTime(2_900);
      responder(peticion);

      expect(tarjetas()).toHaveLength(2);
    });
  });

  it('al pasar a otra zona se pide la nueva y no se queda lo de la anterior', () => {
    responder(crear('a1'));
    expect(tarjetas()).toHaveLength(2);

    fixture.componentRef.setInput('zonaId', 'a5');
    fixture.detectChanges();

    expect(seccion()).toBeNull();
    responder(http.expectOne((p) => p.url === URL('a5')), SIN_ALERTAS);
    expect(raiz().querySelector('.alertas-zona__sin')).not.toBeNull();
    expect(tarjetas()).toHaveLength(0);
  });

  it('en inglés pide los textos en inglés y traduce el resto de la sección', () => {
    establecerIdioma('en', { recordar: false });
    const peticion = crear();
    expect(peticion.request.params.get('idioma')).toBe('en');

    responder(peticion, SIN_ALERTAS);

    expect(raiz().querySelector('h2')?.textContent).toBe('Destination alerts');
    expect(normal(raiz().querySelector('.alertas-zona__sin')?.textContent)).toBe('No active alerts There are no active notices for this area.');
    expect(normal(raiz().querySelector('.alertas-zona__actualizado')?.textContent)).toMatch(/^Last updated: Oct 24, 2026/);
    expect(raiz().querySelector('a[href="/alertas"]')?.textContent?.trim()).toBe('See alerts for all areas');
  });
});
