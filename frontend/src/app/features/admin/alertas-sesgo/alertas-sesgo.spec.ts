import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AlertaSesgo, PrestadorAdmin } from '../../../core/api/modelos-nucleo';
import { erroresInterceptor } from '../../../core/errores/errores.interceptor';
import { establecerIdioma } from '../../../core/idioma/idioma';
import { CLAVE_SESION, aSesionGuardada } from '../../../core/sesion/almacen-sesion';
import { respuestaDeSesion } from '../../../core/sesion/testing';
import { AlertasSesgo } from './alertas-sesgo';

const ALERTAS = '/api/nucleo/admin/alertas-sesgo';
const PRESTADORES = '/api/nucleo/admin/prestadores';

function alerta(id: string, indice: number, extra: Partial<AlertaSesgo> = {}): AlertaSesgo {
  return {
    prestadorId: id,
    indice,
    porcentajeApariciones: 30,
    porcentajeOfertas: 10,
    apariciones: 450,
    recomendaciones: 300,
    abiertaEn: '2026-11-01T09:00:00',
    actualizadaEn: '2026-11-02T09:00:00',
    ...extra,
  };
}

const prestador = (id: string, razonSocial: string): PrestadorAdmin => ({
  id,
  numeroRnt: '910005',
  razonSocial,
  tipo: 'AGENCIA_OPERADORA',
  estadoRnt: 'ACTIVO',
  registradoEn: '2026-10-30T14:00:00Z',
});

describe('AlertasSesgo', () => {
  let fixture: ComponentFixture<AlertasSesgo>;
  let http: HttpTestingController;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const texto = () => (raiz().textContent ?? '').replace(/[  ]/g, ' ');
  const tarjetas = () => Array.from(raiz().querySelectorAll<HTMLElement>('li.alerta-sesgo'));

  function abrir(): void {
    localStorage.setItem(
      CLAVE_SESION,
      JSON.stringify(aSesionGuardada(respuestaDeSesion({ rol: 'ADMINISTRADOR' }))),
    );
    fixture = TestBed.createComponent(AlertasSesgo);
    fixture.detectChanges();
  }

  /** Abre la pantalla y responde las alertas y la lista de prestadores. */
  function crear(alertas: AlertaSesgo[], prestadores: PrestadorAdmin[] = []): void {
    abrir();
    http.expectOne(ALERTAS).flush(alertas);
    http.expectOne(PRESTADORES).flush(prestadores);
    fixture.detectChanges();
  }

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    establecerIdioma('es', { recordar: false });
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([erroresInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('pide las alertas y la lista de prestadores para ponerles nombre', () => {
    abrir();

    expect(http.expectOne(ALERTAS).request.method).toBe('GET');
    expect(http.expectOne(PRESTADORES).request.params.has('sinOperador')).toBe(false);
    http.match(() => true).forEach((p) => p.flush([]));
  });

  it('muestra el nombre del prestador, su tipo, el índice y de cuántas veces se pasa', () => {
    crear([alerta('p1', 3)], [prestador('p1', 'TURISMO SIERRA NEVADA LTDA.')]);

    expect(tarjetas()).toHaveLength(1);
    expect(tarjetas()[0].querySelector('h2')?.textContent?.trim()).toBe('TURISMO SIERRA NEVADA LTDA.');
    expect(tarjetas()[0].textContent).toContain('Agencia operadora');
    expect(tarjetas()[0].querySelector('.etiqueta')?.textContent).toContain('Índice 3,00');
    expect(tarjetas()[0].textContent).toContain('Aparece 3,0 veces lo que le corresponde.');
  });

  it('muestra los porcentajes, las cuentas y las fechas', () => {
    crear([alerta('p1', 3)], [prestador('p1', 'Sierra')]);

    expect(texto()).toContain('Parte de las apariciones30,0 %');
    expect(texto()).toContain('Parte de las ofertas visibles10,0 %');
    expect(texto()).toContain('450 apariciones en 300 recomendaciones');
    expect(texto()).toContain('Abierta');
    expect(texto()).toContain('Actualizada');
  });

  it('con una sola aparición usa el singular', () => {
    crear([alerta('p1', 2, { apariciones: 1 })], [prestador('p1', 'Sierra')]);

    expect(texto()).toContain('1 aparición en 300 recomendaciones');
  });

  it('ordena de mayor a menor índice aunque lleguen desordenadas', () => {
    crear(
      [alerta('a', 1.6), alerta('b', 3), alerta('c', 2.2)],
      [prestador('a', 'Alfa'), prestador('b', 'Beta'), prestador('c', 'Gama')],
    );

    expect(tarjetas().map((t) => t.querySelector('h2')?.textContent?.trim())).toEqual(['Beta', 'Gama', 'Alfa']);
  });

  it('si no se conoce el nombre del prestador, muestra su código', () => {
    crear([alerta('10000000-0000-0000-0000-000000000001', 2)], []);

    expect(tarjetas()[0].querySelector('h2')?.textContent?.trim()).toBe('Prestador 10000000');
  });

  it('si la lista de prestadores falla, las alertas se ven igual, con el código', () => {
    abrir();
    http.expectOne(ALERTAS).flush([alerta('abcdef12-0000', 2)]);
    http.expectOne(PRESTADORES).error(new ProgressEvent('error'));
    fixture.detectChanges();

    expect(tarjetas()).toHaveLength(1);
    expect(tarjetas()[0].querySelector('h2')?.textContent?.trim()).toBe('Prestador ABCDEF12');
    expect(raiz().querySelector('.alerta--error')).toBeNull();
  });

  it('sin alertas abiertas lo dice', () => {
    crear([]);

    expect(tarjetas()).toHaveLength(0);
    expect(texto()).toContain('Sin alertas abiertas');
  });

  it('explica cómo funciona el índice y el umbral', () => {
    crear([]);

    expect(texto()).toContain('si el índice pasa de 1,5, se abre una alerta');
  });

  it('si la consulta falla avisa del error', () => {
    abrir();
    http.expectOne(ALERTAS).error(new ProgressEvent('error'));
    http.expectOne(PRESTADORES).flush([]);
    fixture.detectChanges();

    expect(raiz().querySelector('.alerta--error')).not.toBeNull();
  });

  it('vuelve a consultar al regresar a la pestaña', () => {
    crear([alerta('p1', 2)], [prestador('p1', 'Sierra')]);

    document.dispatchEvent(new Event('visibilitychange'));

    http.expectOne(ALERTAS).flush([alerta('p1', 2), alerta('p2', 4)]);
    http.expectOne(PRESTADORES).flush([prestador('p1', 'Sierra'), prestador('p2', 'Mar')]);
    fixture.detectChanges();
    expect(tarjetas()).toHaveLength(2);
    expect(tarjetas()[0].querySelector('h2')?.textContent?.trim()).toBe('Mar');
  });

  it('también se ve en inglés', () => {
    establecerIdioma('en', { recordar: false });
    crear([alerta('p1', 3)], [prestador('p1', 'Sierra')]);

    expect(texto()).toContain('Bias alerts');
    expect(texto()).toContain('Index 3.00');
    expect(texto()).toContain('Appears 3.0 times its fair share.');
    expect(texto()).toContain('Share of appearances30.0 %');
    expect(texto()).toContain('450 appearances in 300 recommendations');
    expect(tarjetas()[0].textContent).toContain('Tour operator agency');
  });
});
