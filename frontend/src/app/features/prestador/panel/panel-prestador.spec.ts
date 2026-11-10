import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { TipoPrestador } from '../../../core/api/modelos';
import { CLAVE_SESION, aSesionGuardada } from '../../../core/sesion/almacen-sesion';
import { respuestaDeSesion } from '../../../core/sesion/testing';
import { PanelPrestador } from './panel-prestador';

describe('PanelPrestador', () => {
  let fixture: ComponentFixture<PanelPrestador>;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const texto = () => raiz().textContent ?? '';

  function crear(tipoPrestador?: TipoPrestador): void {
    const sesion = respuestaDeSesion({ rol: 'PRESTADOR', tipoPrestador });
    localStorage.setItem(
      CLAVE_SESION,
      JSON.stringify(
        aSesionGuardada({ ...sesion, cuenta: { ...sesion.cuenta, nombre: 'Hotel Sierra' } }),
      ),
    );
    fixture = TestBed.createComponent(PanelPrestador);
    fixture.detectChanges();
  }

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
  });

  it('muestra el nombre del prestador de la sesión y su tipo', () => {
    crear('ALOJAMIENTO');

    expect(raiz().querySelector('.identidad__nombre')?.textContent).toBe('Hotel Sierra');
    expect(raiz().querySelector('.identidad__avatar')?.textContent).toBe('H');
    expect(raiz().querySelector('.identidad__tipo')?.textContent?.trim()).toBe('Alojamiento');
    expect(raiz().querySelector('.alerta')).toBeNull();
  });

  it('ya no manda a cambiar de prestador en el selector demo', () => {
    crear('RESTAURANTE');

    expect(texto()).not.toContain('Usuario demo');
  });

  it('un prestador que aún no registra su RNT lo ve y recibe un aviso', () => {
    crear();

    expect(raiz().querySelector('.identidad__tipo')?.textContent?.trim()).toBe('RNT por registrar');
    expect(raiz().querySelector('.alerta')?.textContent).toContain('Todavía no registras tu RNT');
  });

  it('ofrece el inicio, las ofertas y cada sección del registro', () => {
    crear('AGENCIA_OPERADORA');

    const enlaces = Array.from(raiz().querySelectorAll<HTMLAnchorElement>('.menu a'));
    expect(enlaces.map((a) => [a.textContent?.trim(), a.getAttribute('href')])).toEqual([
      ['Inicio', '/prestador'],
      ['Mis ofertas', '/prestador/ofertas'],
      ['Publicar oferta', '/prestador/ofertas/nueva'],
      ['Mi RNT', '/prestador/rnt'],
      ['Perfil público', '/prestador/perfil'],
      ['WhatsApp de contacto', '/prestador/whatsapp'],
      ['Cuenta de cobro', '/prestador/cuenta-cobro'],
    ]);
  });

  describe('la sección marcada', () => {
    const marcados = () =>
      Array.from(raiz().querySelectorAll<HTMLAnchorElement>('.menu a[aria-current="page"]')).map((a) =>
        a.getAttribute('href'),
      );

    async function irA(url: string): Promise<void> {
      await TestBed.inject(Router).navigateByUrl(url);
      fixture.detectChanges();
    }

    beforeEach(() => {
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [
          provideRouter([{ path: '**', children: [] }]),
          provideHttpClient(),
          provideHttpClientTesting(),
        ],
      });
      crear('AGENCIA_OPERADORA');
    });

    it.each([
      ['/prestador', '/prestador'],
      ['/prestador/ofertas', '/prestador/ofertas'],
      ['/prestador/ofertas/abc', '/prestador/ofertas'],
      ['/prestador/ofertas/abc/editar', '/prestador/ofertas'],
      ['/prestador/ofertas/nueva', '/prestador/ofertas/nueva'],
      ['/prestador/rnt', '/prestador/rnt'],
      ['/prestador/perfil', '/prestador/perfil'],
      ['/prestador/whatsapp', '/prestador/whatsapp'],
      ['/prestador/cuenta-cobro', '/prestador/cuenta-cobro'],
    ])('en %s se marca solo %s', async (url, esperado) => {
      await irA(url);

      expect(marcados()).toEqual([esperado]);
    });
  });
});
