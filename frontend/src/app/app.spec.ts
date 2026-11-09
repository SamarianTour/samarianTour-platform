import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { App } from './app';
import { Rol } from './core/api/modelos-nucleo';
import { CLAVE_SESION, aSesionGuardada } from './core/sesion/almacen-sesion';
import { SesionService } from './core/sesion/sesion.service';
import { respuestaDeSesion } from './core/sesion/testing';

@Component({ template: '' })
class Vacio {}

describe('App (cabecera)', () => {
  let fixture: ComponentFixture<App>;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const enlaces = () =>
    Array.from(raiz().querySelectorAll<HTMLAnchorElement>('nav a')).map((a) =>
      a.textContent?.trim(),
    );
  const botonesDeCuenta = () =>
    Array.from(raiz().querySelectorAll<HTMLAnchorElement>('.cabecera__cuenta a'));

  function crear(rol?: Rol): void {
    if (rol) {
      localStorage.setItem(
        CLAVE_SESION,
        JSON.stringify(aSesionGuardada(respuestaDeSesion({ rol }))),
      );
    }
    fixture = TestBed.createComponent(App);
    fixture.detectChanges();
  }

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: '**', component: Vacio }]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
  });

  describe('sin sesión', () => {
    it('ofrece iniciar sesión y registrarse, y ningún menú de cuenta', () => {
      crear();

      expect(botonesDeCuenta().map((a) => [a.textContent?.trim(), a.getAttribute('href')])).toEqual(
        [
          ['Iniciar sesión', '/iniciar-sesion'],
          ['Registrarse', '/registro'],
        ],
      );
      expect(raiz().querySelector('app-menu-usuario')).toBeNull();
    });

    it('la navegación es el catálogo y el camino para los prestadores', () => {
      crear();

      expect(enlaces()).toEqual(['Catálogo', 'Soy prestador']);
      expect(
        raiz().querySelector<HTMLAnchorElement>('nav a[href^="/registro"]')?.getAttribute('href'),
      ).toBe('/registro?tipo=prestador');
    });

    it('ya no tiene el selector de usuario demo', () => {
      crear();

      expect(raiz().textContent).not.toContain('Usuario demo');
      expect(raiz().querySelector('select')).toBeNull();
    });
  });

  describe('con sesión', () => {
    it('muestra el menú de la cuenta en lugar de los botones de acceso', () => {
      crear('TURISTA');

      expect(raiz().querySelector('app-menu-usuario .menu__nombre')?.textContent).toBe('Laura');
      expect(raiz().textContent).not.toContain('Registrarse');
      expect(raiz().textContent).not.toContain('Iniciar sesión');
    });

    it.each([
      ['TURISTA', ['Catálogo', 'Mis solicitudes', 'Mis opiniones']],
      ['PRESTADOR', ['Catálogo', 'Mis ofertas']],
      ['OPERADOR_DISPONIBILIDAD', ['Cola de solicitudes']],
      ['GESTOR_DESTINO', ['Indicadores']],
      ['ADMINISTRADOR', ['Administración']],
    ] as const)('un %s ve los enlaces de su rol', (rol, esperados) => {
      crear(rol);

      expect(enlaces()).toEqual(esperados);
    });

    it('al cerrar la sesión la cabecera vuelve a ofrecer iniciar sesión', () => {
      crear('PRESTADOR');

      TestBed.inject(SesionService).descartarSesion();
      fixture.detectChanges();

      expect(enlaces()).toEqual(['Catálogo', 'Soy prestador']);
      expect(botonesDeCuenta().map((a) => a.textContent?.trim())).toEqual([
        'Iniciar sesión',
        'Registrarse',
      ]);
    });
  });

  describe('idioma', () => {
    const botonDeIdioma = (codigo: string) =>
      Array.from(raiz().querySelectorAll<HTMLButtonElement>('app-selector-idioma button')).find(
        (b) => b.textContent?.trim() === codigo,
      )!;

    it('tiene el selector ES | EN en la cabecera', () => {
      crear();

      expect(raiz().querySelector('header app-selector-idioma')).not.toBeNull();
    });

    it('al elegir EN, la cabecera, el menú y el pie pasan a inglés sin recargar', () => {
      crear();

      botonDeIdioma('EN').click();
      fixture.detectChanges();

      expect(enlaces()).toEqual(['Catalog', "I'm a provider"]);
      expect(botonesDeCuenta().map((a) => a.textContent?.trim())).toEqual(['Log in', 'Sign up']);
      expect(raiz().querySelector('.saltar')?.textContent).toBe('Skip to content');
      expect(raiz().querySelector('nav')?.getAttribute('aria-label')).toBe('Main');
      expect(raiz().querySelector('footer')?.textContent).toContain(
        'Sustainable tourism in Santa Marta',
      );
    });

    it('el menú de un rol también cambia, y vuelve a español', () => {
      crear('TURISTA');

      botonDeIdioma('EN').click();
      fixture.detectChanges();
      expect(enlaces()).toEqual(['Catalog', 'My requests', 'My reviews']);

      botonDeIdioma('ES').click();
      fixture.detectChanges();
      expect(enlaces()).toEqual(['Catálogo', 'Mis solicitudes', 'Mis opiniones']);
    });

    it('la elección se recuerda: una página nueva arranca en el idioma elegido', () => {
      crear();
      botonDeIdioma('EN').click();
      fixture.detectChanges();

      expect(localStorage.getItem('samarian-tour.idioma')).toBe('en');
    });
  });

  describe('sección actual', () => {
    const marcado = () =>
      Array.from(raiz().querySelectorAll<HTMLAnchorElement>('nav a[aria-current="page"]')).map(
        (a) => a.textContent?.trim(),
      );

    async function ir(url: string): Promise<void> {
      await TestBed.inject(Router).navigateByUrl(url);
      fixture.detectChanges();
    }

    it('marca el catálogo en la ficha de una oferta', async () => {
      crear('TURISTA');

      await ir('/ofertas/abc');

      expect(marcado()).toEqual(['Catálogo']);
    });

    it('marca la sección de las opiniones', async () => {
      crear('TURISTA');

      await ir('/mis-opiniones');

      expect(marcado()).toEqual(['Mis opiniones']);
      expect(raiz().querySelector('nav a.activo')?.textContent?.trim()).toBe('Mis opiniones');
    });

    it('marca "Mis ofertas" en cualquier pantalla del panel del prestador', async () => {
      crear('PRESTADOR');

      await ir('/prestador/ofertas/nueva');

      expect(marcado()).toEqual(['Mis ofertas']);
    });

    it('no marca ninguna en una pantalla que no está en el menú', async () => {
      crear('TURISTA');

      await ir('/mis-datos');

      expect(marcado()).toEqual([]);
    });
  });
});
