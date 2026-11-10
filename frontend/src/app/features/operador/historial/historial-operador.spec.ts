import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { EstadoSolicitud, SolicitudCola } from '../../../core/api/modelos-nucleo';
import { erroresInterceptor } from '../../../core/errores/errores.interceptor';
import { establecerIdioma } from '../../../core/idioma/idioma';
import { CLAVE_SESION, aSesionGuardada } from '../../../core/sesion/almacen-sesion';
import { respuestaDeSesion } from '../../../core/sesion/testing';
import { HistorialOperador } from './historial-operador';

const URL = '/api/nucleo/operador/solicitudes';
const AHORA = Date.parse('2026-10-26T15:00:00Z');
const HORA = 3_600_000;
const en = (ms: number) => new Date(AHORA + ms).toISOString();

function solicitud(i: number, estado: EstadoSolicitud): SolicitudCola {
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
    estado,
    proximaAVencer: false,
    creadaEn: en(-5 * HORA),
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

describe('HistorialOperador', () => {
  let harness: RouterTestingHarness;
  let http: HttpTestingController;

  const raiz = () => harness.routeNativeElement as HTMLElement;
  const texto = () => (raiz().textContent ?? '').replace(/[  ]/g, ' ');
  const filas = () => Array.from(raiz().querySelectorAll<HTMLElement>('tbody tr'));
  const filtros = () => Array.from(raiz().querySelectorAll<HTMLAnchorElement>('.filtros a'));
  const consulta = () => http.expectOne((p) => p.url === URL);

  /** Navega a la dirección y devuelve la consulta que la pantalla hizo al núcleo. */
  async function abrir(url: string) {
    await harness.navigateByUrl(url, HistorialOperador);
    harness.detectChanges();
    return consulta();
  }

  beforeEach(async () => {
    localStorage.clear();
    sessionStorage.clear();
    establecerIdioma('es', { recordar: false });
    localStorage.setItem(
      CLAVE_SESION,
      JSON.stringify(aSesionGuardada(respuestaDeSesion({ rol: 'OPERADOR_DISPONIBILIDAD' }))),
    );
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'operador/historial', component: HistorialOperador }]),
        provideHttpClient(withInterceptors([erroresInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
  });

  afterEach(() => http.verify());

  it('sin estado en la dirección abre las atendidas', async () => {
    const peticion = await abrir('/operador/historial');

    expect(peticion.request.params.get('estado')).toBe('ATENDIDA');
    expect(peticion.request.params.get('page')).toBe('0');
    expect(peticion.request.params.get('size')).toBe('10');
    peticion.flush(pagina([]));
  });

  it('pide el estado y la página de la dirección', async () => {
    const peticion = await abrir('/operador/historial?estado=PAGADA&page=2');

    expect(peticion.request.params.get('estado')).toBe('PAGADA');
    expect(peticion.request.params.get('page')).toBe('2');
    peticion.flush(pagina([], { pagina: 2, totalPaginas: 3 }));
  });

  it('un estado que no es del historial (como Pendiente) no llega a la API', async () => {
    const peticion = await abrir('/operador/historial?estado=PENDIENTE');

    expect(peticion.request.params.get('estado')).toBe('ATENDIDA');
    peticion.flush(pagina([]));
  });

  it('ofrece un enlace por cada estado y marca el actual', async () => {
    (await abrir('/operador/historial?estado=NO_DISPONIBLE')).flush(pagina([]));
    harness.detectChanges();

    expect(filtros()).toHaveLength(8);
    expect(filtros().map((a) => a.getAttribute('href'))).toContain(
      '/operador/historial?estado=PAGADA',
    );
    const actual = filtros().filter((a) => a.getAttribute('aria-current') === 'page');
    expect(actual.map((a) => a.textContent?.trim())).toEqual(['No disponible']);
  });

  it('muestra cada solicitud con su estado y un enlace para verla, sin urgencia ni plazo', async () => {
    (await abrir('/operador/historial?estado=PAGADA')).flush(pagina([solicitud(1, 'PAGADA')]));
    harness.detectChanges();

    expect(filas()).toHaveLength(1);
    expect(filas()[0].querySelector('.etiqueta')?.textContent?.trim()).toBe('Pagada');
    expect(filas()[0].textContent).toContain('Oferta 1');
    const cabeceras = Array.from(raiz().querySelectorAll('th')).map((c) => c.textContent?.trim());
    expect(cabeceras).toContain('Estado');
    expect(cabeceras).not.toContain('Plazo');
    expect(cabeceras).not.toContain('Urgencia');
    const enlace = filas()[0].querySelector<HTMLAnchorElement>('a.boton')!;
    expect(enlace.getAttribute('href')).toBe(
      '/operador/solicitudes/6f1c2a9e-4b7d-4c1e-9a52-3d8e0f7b1a21',
    );
    expect(enlace.getAttribute('aria-label')).toBe('Ver la solicitud de Oferta 1');
  });

  it('respeta el orden de la API: de la más reciente a la más antigua', async () => {
    (await abrir('/operador/historial')).flush(
      pagina([solicitud(2, 'ATENDIDA'), solicitud(1, 'ATENDIDA')]),
    );
    harness.detectChanges();

    expect(filas().map((f) => f.querySelector('.oferta')?.textContent)).toEqual([
      'Oferta 2',
      'Oferta 1',
    ]);
  });

  it('dice cuántas solicitudes hay en ese estado, en singular y plural', async () => {
    (await abrir('/operador/historial')).flush(
      pagina([solicitud(1, 'ATENDIDA')], { totalElementos: 1 }),
    );
    harness.detectChanges();
    expect(texto()).toContain('1 solicitud');
    expect(texto()).not.toContain('1 solicitudes');

    await harness.navigateByUrl('/operador/historial?estado=VENCIDA');
    harness.detectChanges();
    consulta().flush(pagina([solicitud(2, 'VENCIDA')], { totalElementos: 14, totalPaginas: 2 }));
    harness.detectChanges();
    expect(texto()).toContain('14 solicitudes');
  });

  it('sin solicitudes en el estado lo dice', async () => {
    (await abrir('/operador/historial?estado=DECLINADA')).flush(pagina([]));
    harness.detectChanges();

    expect(raiz().querySelector('table')).toBeNull();
    expect(texto()).toContain('Sin solicitudes en este estado');
  });

  it('al elegir otro estado vuelve a consultar con ese estado', async () => {
    (await abrir('/operador/historial')).flush(pagina([solicitud(1, 'ATENDIDA')]));
    harness.detectChanges();

    filtros()
      .find((a) => a.textContent?.trim() === 'Pagada')!
      .click();
    await harness.fixture.whenStable();
    harness.detectChanges();

    const peticion = consulta();
    expect(peticion.request.params.get('estado')).toBe('PAGADA');
    peticion.flush(pagina([solicitud(2, 'PAGADA')]));
    harness.detectChanges();
    expect(filas()[0].textContent).toContain('Oferta 2');
  });

  it('al paginar conserva el estado en la dirección', async () => {
    (await abrir('/operador/historial?estado=PAGADA')).flush(
      pagina([solicitud(1, 'PAGADA')], { totalPaginas: 3 }),
    );
    harness.detectChanges();

    Array.from(raiz().querySelectorAll<HTMLButtonElement>('.paginacion button'))
      .find((b) => b.textContent?.includes('Siguiente'))!
      .click();
    await harness.fixture.whenStable();

    expect(TestBed.inject(Router).url).toBe('/operador/historial?estado=PAGADA&page=1');
    const peticion = consulta();
    expect(peticion.request.params.get('page')).toBe('1');
    peticion.flush(pagina([], { pagina: 1, totalPaginas: 3 }));
  });

  it('si la consulta falla avisa del error', async () => {
    (await abrir('/operador/historial')).error(new ProgressEvent('error'));
    harness.detectChanges();

    expect(raiz().querySelector('.alerta--error')).not.toBeNull();
  });

  it('también se ve en inglés', async () => {
    establecerIdioma('en', { recordar: false });
    (await abrir('/operador/historial?estado=PAGADA')).flush(
      pagina([solicitud(1, 'PAGADA')], { totalElementos: 5 }),
    );
    harness.detectChanges();

    expect(texto()).toContain('Request history');
    expect(texto()).toContain('5 requests');
    expect(filtros().map((a) => a.textContent?.trim())).toContain('Unavailable');
    expect(filas()[0].querySelector('.etiqueta')?.textContent?.trim()).toBe('Paid');
    expect(filas()[0].querySelector('a.boton')?.textContent?.trim()).toBe('View');
  });
});
