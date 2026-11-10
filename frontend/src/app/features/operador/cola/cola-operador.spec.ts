import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { SolicitudCola } from '../../../core/api/modelos-nucleo';
import { erroresInterceptor } from '../../../core/errores/errores.interceptor';
import { establecerIdioma } from '../../../core/idioma/idioma';
import { CLAVE_SESION, aSesionGuardada } from '../../../core/sesion/almacen-sesion';
import { respuestaDeSesion } from '../../../core/sesion/testing';
import { ColaOperador } from './cola-operador';

const URL = '/api/nucleo/operador/solicitudes';
const AHORA = Date.parse('2026-10-26T15:00:00Z');
const HORA = 3_600_000;
const en = (ms: number) => new Date(AHORA + ms).toISOString();

function solicitud(i: number, extra: Partial<SolicitudCola> = {}): SolicitudCola {
  return {
    id: `6f1c2a9e-4b7d-4c1e-9a52-3d8e0f7b1a2${i}`,
    oferta: {
      id: `b0000000000000000000000${i}`,
      tipo: 'ACTIVIDAD',
      nombre: `Oferta ${i}`,
      zonaId: 'z1',
    },
    fechaInicio: '2026-10-28',
    horaSalida: '07:00:00',
    personas: 2,
    estado: 'PENDIENTE',
    proximaAVencer: false,
    venceEn: en(20 * HORA),
    creadaEn: en(-4 * HORA),
    ...extra,
  };
}

const pagina = (contenido: SolicitudCola[], extra: object = {}) => ({
  contenido,
  pagina: 0,
  tamano: 10,
  totalElementos: contenido.length,
  totalPaginas: 1,
  ...extra,
});

describe('ColaOperador (P02.4)', () => {
  let fixture: ComponentFixture<ColaOperador>;
  let http: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const texto = () => (raiz().textContent ?? '').replace(/[  ]/g, ' ');
  const filas = () => Array.from(raiz().querySelectorAll<HTMLElement>('tbody tr'));
  const lista = () => http.expectOne((p) => p.url === URL);

  function crear(contenido: SolicitudCola[], extra: object = {}): void {
    localStorage.setItem(
      CLAVE_SESION,
      JSON.stringify(aSesionGuardada(respuestaDeSesion({ rol: 'OPERADOR_DISPONIBILIDAD' }))),
    );
    fixture = TestBed.createComponent(ColaOperador);
    fixture.detectChanges();
    lista().flush(pagina(contenido, extra));
    fixture.detectChanges();
  }

  beforeEach(() => {
    vi.useFakeTimers({ now: AHORA, toFake: ['Date', 'setInterval', 'clearInterval'] });
    localStorage.clear();
    sessionStorage.clear();
    establecerIdioma('es', { recordar: false });
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([erroresInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });

  afterEach(() => {
    http.verify();
    vi.useRealTimers();
  });

  it('pide la cola del operador sin estado, desde la primera página', () => {
    localStorage.setItem(
      CLAVE_SESION,
      JSON.stringify(aSesionGuardada(respuestaDeSesion({ rol: 'OPERADOR_DISPONIBILIDAD' }))),
    );
    fixture = TestBed.createComponent(ColaOperador);
    fixture.detectChanges();

    const peticion = lista();
    expect(peticion.request.params.has('estado')).toBe(false);
    expect(peticion.request.params.get('page')).toBe('0');
    expect(peticion.request.params.get('size')).toBe('10');
    peticion.flush(pagina([]));
  });

  it('muestra las pendientes con su oferta, su fecha y la cantidad de personas', () => {
    crear([solicitud(1)]);

    expect(filas()).toHaveLength(1);
    expect(filas()[0].textContent).toContain('Oferta 1');
    expect(filas()[0].textContent).toContain('07:00');
    expect(filas()[0].textContent).toContain('2 personas');
    expect(filas()[0].textContent).toContain('Ref. 6F1C2A9E');
  });

  it('pone primero y resalta las de 6 horas o menos, con el texto "Urgente" (RF-36)', () => {
    crear([
      solicitud(1, { venceEn: en(20 * HORA) }),
      solicitud(2, { venceEn: en(3 * HORA), proximaAVencer: true }),
    ]);

    expect(filas().map((f) => f.querySelector('.oferta')?.textContent)).toEqual([
      'Oferta 2',
      'Oferta 1',
    ]);
    expect(filas()[0].classList).toContain('fila--urgente');
    expect(filas()[0].textContent).toContain('Urgente');
    expect(filas()[0].textContent).toContain('Vence en 3 h');
    expect(filas()[1].classList).not.toContain('fila--urgente');
    expect(filas()[1].textContent).toContain('En plazo');
  });

  it('marca como vencida la que ya pasó su plazo y la deja al final', () => {
    crear([solicitud(1, { venceEn: en(-HORA) }), solicitud(2)]);

    expect(filas().map((f) => f.querySelector('.oferta')?.textContent)).toEqual([
      'Oferta 2',
      'Oferta 1',
    ]);
    expect(filas()[1].classList).toContain('fila--vencida');
    expect(filas()[1].textContent).toContain('Plazo vencido');
  });

  it('cada fila lleva a atender esa solicitud', () => {
    crear([solicitud(1)]);

    const enlace = filas()[0].querySelector<HTMLAnchorElement>('a.boton')!;
    expect(enlace.getAttribute('href')).toBe(
      '/operador/solicitudes/6f1c2a9e-4b7d-4c1e-9a52-3d8e0f7b1a21',
    );
    expect(enlace.getAttribute('aria-label')).toBe('Atender la solicitud de Oferta 1');
  });

  it('no muestra datos personales del turista', () => {
    crear([solicitud(1)]);

    expect(raiz().querySelector('th')?.textContent).not.toMatch(/turista/i);
    expect(Array.from(raiz().querySelectorAll('th')).map((c) => c.textContent)).not.toContain(
      'Turista',
    );
  });

  it('cuenta los pendientes con el total de la API, no solo con los de la página', () => {
    crear([solicitud(1)], { totalElementos: 23, totalPaginas: 3 });

    expect(raiz().querySelector('.kpi__valor')?.textContent).toBe('23');
  });

  it('sin pendientes dice que todo está al día', () => {
    crear([]);

    expect(raiz().querySelector('table')).toBeNull();
    expect(texto()).toContain('Todo al día');
  });

  it('pagina: pasa a la página elegida con ?page', () => {
    crear([solicitud(1)], { totalPaginas: 3 });

    const siguiente = Array.from(raiz().querySelectorAll<HTMLButtonElement>('.paginacion button')).find(
      (b) => b.textContent?.includes('Siguiente'),
    )!;
    siguiente.click();

    expect(navegar).toHaveBeenCalledWith(
      [],
      expect.objectContaining({ queryParams: { page: 1 } }),
    );
  });

  it('si la consulta falla avisa del error', () => {
    localStorage.setItem(
      CLAVE_SESION,
      JSON.stringify(aSesionGuardada(respuestaDeSesion({ rol: 'OPERADOR_DISPONIBILIDAD' }))),
    );
    fixture = TestBed.createComponent(ColaOperador);
    fixture.detectChanges();
    lista().error(new ProgressEvent('error'));
    fixture.detectChanges();

    expect(raiz().querySelector('.alerta--error')).not.toBeNull();
  });

  it('vuelve a consultar al regresar a la pestaña, para ver las solicitudes nuevas', () => {
    crear([solicitud(1)]);

    document.dispatchEvent(new Event('visibilitychange'));

    lista().flush(pagina([solicitud(1), solicitud(2)]));
    fixture.detectChanges();
    expect(filas()).toHaveLength(2);
  });

  it('también se ve en inglés', () => {
    establecerIdioma('en', { recordar: false });
    crear([solicitud(1, { venceEn: en(2 * HORA), proximaAVencer: true })]);

    expect(texto()).toContain('Request queue');
    expect(texto()).toContain('Urgent');
    expect(texto()).toContain('Due in 2 h');
    expect(filas()[0].querySelector('a.boton')?.textContent?.trim()).toBe('Handle');
  });
});
