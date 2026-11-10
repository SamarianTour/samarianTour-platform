import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { establecerIdioma } from '../../core/idioma/idioma';
import { CLAVE_SESION, aSesionGuardada } from '../../core/sesion/almacen-sesion';
import { respuestaDeSesion } from '../../core/sesion/testing';
import { EnlacePanel } from './enlace-activo';
import { PanelLateral } from './panel-lateral';

@Component({ selector: 'app-pantalla-falsa', template: '<p class="falsa">Pantalla</p>' })
class PantallaFalsa {}

@Component({
  selector: 'app-anfitrion',
  imports: [PanelLateral],
  template: `<app-panel-lateral rol="etiquetas.rol.ADMINISTRADOR" titulo="admin.panel.control" [enlaces]="enlaces" />`,
})
class Anfitrion {
  readonly enlaces: readonly EnlacePanel[] = [
    { clave: 'app.titulo.prestadoresRnt', ruta: '/panel', icono: 'escudo', exacto: true },
    { clave: 'app.titulo.zonasAdmin', ruta: '/panel/zonas', icono: 'ubicacion' },
  ];
}

describe('PanelLateral', () => {
  let harness: RouterTestingHarness;

  const raiz = () => harness.routeNativeElement as HTMLElement;
  const enlaces = () => Array.from(raiz().querySelectorAll<HTMLAnchorElement>('.menu a'));

  beforeEach(async () => {
    localStorage.clear();
    sessionStorage.clear();
    establecerIdioma('es', { recordar: false });
    const sesion = respuestaDeSesion({ rol: 'ADMINISTRADOR' });
    localStorage.setItem(
      CLAVE_SESION,
      JSON.stringify(aSesionGuardada({ ...sesion, cuenta: { ...sesion.cuenta, nombre: 'Marta Ríos' } })),
    );
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          {
            path: 'panel',
            component: Anfitrion,
            children: [
              { path: '', component: PantallaFalsa },
              { path: 'zonas', component: PantallaFalsa },
            ],
          },
        ]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    harness = await RouterTestingHarness.create();
  });

  it('muestra el nombre, la inicial y el rol de quien tiene la sesión', async () => {
    await harness.navigateByUrl('/panel', Anfitrion);

    expect(raiz().querySelector('.identidad__nombre')?.textContent).toBe('Marta Ríos');
    expect(raiz().querySelector('.identidad__avatar')?.textContent).toBe('M');
    expect(raiz().querySelector('.identidad .rotulo')?.textContent).toBe('Administrador');
  });

  it('nombra el menú y lista los enlaces recibidos, con su texto y su dirección', async () => {
    await harness.navigateByUrl('/panel', Anfitrion);

    expect(raiz().querySelector('nav .rotulo')?.textContent).toBe('Panel del administrador');
    expect(enlaces().map((a) => [a.textContent?.trim(), a.getAttribute('href')])).toEqual([
      ['Prestadores y RNT', '/panel'],
      ['Zonas', '/panel/zonas'],
    ]);
  });

  it('muestra la pantalla de la ruta junto al menú', async () => {
    await harness.navigateByUrl('/panel', Anfitrion);

    expect(raiz().querySelector('.contenido .falsa')).not.toBeNull();
  });

  it('marca solo el inicio en su dirección exacta, con aria-current', async () => {
    await harness.navigateByUrl('/panel', Anfitrion);
    harness.detectChanges();

    const activos = enlaces().filter((a) => a.classList.contains('activo'));
    expect(activos.map((a) => a.textContent?.trim())).toEqual(['Prestadores y RNT']);
    expect(activos[0].getAttribute('aria-current')).toBe('page');
  });

  it('al entrar en otra sección marca esa y desmarca el inicio', async () => {
    await harness.navigateByUrl('/panel/zonas', Anfitrion);
    harness.detectChanges();

    const activos = enlaces().filter((a) => a.classList.contains('activo'));
    expect(activos.map((a) => a.textContent?.trim())).toEqual(['Zonas']);
    expect(enlaces()[0].getAttribute('aria-current')).toBeNull();
  });

  it('el menú también está en inglés', async () => {
    establecerIdioma('en', { recordar: false });
    await harness.navigateByUrl('/panel', Anfitrion);

    expect(raiz().querySelector('nav .rotulo')?.textContent).toBe('Administrator panel');
    expect(enlaces().map((a) => a.textContent?.trim())).toEqual(['Providers and RNT', 'Areas']);
  });
});
