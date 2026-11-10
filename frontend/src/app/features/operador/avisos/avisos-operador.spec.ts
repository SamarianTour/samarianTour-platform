import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AvisoOperador } from '../../../core/api/modelos-nucleo';
import { erroresInterceptor } from '../../../core/errores/errores.interceptor';
import { establecerIdioma } from '../../../core/idioma/idioma';
import { CLAVE_SESION, aSesionGuardada } from '../../../core/sesion/almacen-sesion';
import { respuestaDeSesion } from '../../../core/sesion/testing';
import { AvisosOperador } from './avisos-operador';

const URL = '/api/nucleo/operador/avisos';

function aviso(i: number, datos: Record<string, string> | null = null): AvisoOperador {
  return {
    id: `0b7e3c55-8f0a-4d63-b2a1-7c9d4e5f6a1${i}`,
    tipo: 'NUEVA_SOLICITUD',
    solicitudId: `6f1c2a9e-4b7d-4c1e-9a52-3d8e0f7b1a2${i}`,
    datos: datos ?? {
      solicitudId: `6f1c2a9e-4b7d-4c1e-9a52-3d8e0f7b1a2${i}`,
      nombreOferta: `Oferta ${i}`,
      fechaInicio: '2026-10-28',
      horaSalida: '07:00:00',
      personas: '2',
      estado: 'PENDIENTE',
      venceEn: '2026-10-27T15:00:00Z',
    },
    creadoEn: '2026-10-26T15:00:00Z',
  };
}

const pagina = (contenido: AvisoOperador[], extra: object = {}) => ({
  contenido,
  pagina: 0,
  tamano: 10,
  totalElementos: contenido.length,
  totalPaginas: 1,
  ...extra,
});

describe('AvisosOperador', () => {
  let fixture: ComponentFixture<AvisosOperador>;
  let http: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const texto = () => (raiz().textContent ?? '').replace(/[  ]/g, ' ');
  const tarjetas = () => Array.from(raiz().querySelectorAll<HTMLElement>('li.aviso'));
  const lista = () => http.expectOne((p) => p.url === URL);

  function abrir(): void {
    localStorage.setItem(
      CLAVE_SESION,
      JSON.stringify(aSesionGuardada(respuestaDeSesion({ rol: 'OPERADOR_DISPONIBILIDAD' }))),
    );
    fixture = TestBed.createComponent(AvisosOperador);
    fixture.detectChanges();
  }

  function crear(contenido: AvisoOperador[], extra: object = {}): void {
    abrir();
    lista().flush(pagina(contenido, extra));
    fixture.detectChanges();
  }

  beforeEach(() => {
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

  afterEach(() => http.verify());

  it('pide los avisos del operador desde la primera página', () => {
    abrir();

    const peticion = lista();
    expect(peticion.request.params.get('page')).toBe('0');
    expect(peticion.request.params.get('size')).toBe('10');
    peticion.flush(pagina([]));
  });

  it('muestra cada aviso de solicitud nueva con su oferta, fecha, hora y personas', () => {
    crear([aviso(1), aviso(2)]);

    expect(tarjetas()).toHaveLength(2);
    expect(tarjetas()[0].textContent).toContain('Nueva solicitud');
    expect(tarjetas()[0].textContent).toContain('Oferta 1');
    expect(tarjetas()[0].textContent).toContain('07:00');
    expect(tarjetas()[0].textContent).toContain('2 personas');
    expect(tarjetas()[0].textContent).toContain('Recibido');
  });

  it('respeta el orden de la API: del más reciente al más antiguo', () => {
    crear([aviso(2), aviso(1)]);

    expect(tarjetas().map((t) => t.querySelector('.aviso__oferta')?.textContent)).toEqual([
      'Oferta 2',
      'Oferta 1',
    ]);
  });

  it('cada aviso lleva a atender esa solicitud', () => {
    crear([aviso(1)]);

    const enlace = tarjetas()[0].querySelector<HTMLAnchorElement>('a.boton')!;
    expect(enlace.getAttribute('href')).toBe(
      '/operador/solicitudes/6f1c2a9e-4b7d-4c1e-9a52-3d8e0f7b1a21',
    );
    expect(enlace.getAttribute('aria-label')).toBe('Atender la solicitud de Oferta 1');
  });

  it('si el aviso no trae los datos usa el código de la solicitud, sin inventar nada', () => {
    crear([aviso(1, {})]);

    expect(tarjetas()[0].querySelector('.aviso__oferta')?.textContent).toBe('Ref. 6F1C2A9E');
    expect(tarjetas()[0].textContent).not.toContain('personas');
  });

  it('sin avisos lo dice', () => {
    crear([]);

    expect(tarjetas()).toHaveLength(0);
    expect(texto()).toContain('No tienes avisos');
  });

  it('pagina: pasa a la página elegida con ?page', () => {
    crear([aviso(1)], { totalPaginas: 3 });

    Array.from(raiz().querySelectorAll<HTMLButtonElement>('.paginacion button'))
      .find((b) => b.textContent?.includes('Siguiente'))!
      .click();

    expect(navegar).toHaveBeenCalledWith([], expect.objectContaining({ queryParams: { page: 1 } }));
  });

  it('si la consulta falla avisa del error', () => {
    abrir();
    lista().error(new ProgressEvent('error'));
    fixture.detectChanges();

    expect(raiz().querySelector('.alerta--error')).not.toBeNull();
  });

  it('vuelve a consultar al regresar a la pestaña, para ver los avisos nuevos', () => {
    crear([aviso(1)]);

    document.dispatchEvent(new Event('visibilitychange'));

    lista().flush(pagina([aviso(2), aviso(1)]));
    fixture.detectChanges();
    expect(tarjetas()).toHaveLength(2);
  });

  it('también se ve en inglés', () => {
    establecerIdioma('en', { recordar: false });
    crear([aviso(1)]);

    expect(texto()).toContain('Notices');
    expect(tarjetas()[0].textContent).toContain('New request');
    expect(tarjetas()[0].textContent).toContain('2 people');
    expect(tarjetas()[0].querySelector('a.boton')?.textContent?.trim()).toBe('Handle');
  });
});
