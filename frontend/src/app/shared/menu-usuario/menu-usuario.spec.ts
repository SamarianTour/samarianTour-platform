import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AvisosService } from '../../core/avisos/avisos.service';
import { Rol } from '../../core/api/modelos-nucleo';
import { CLAVE_SESION, aSesionGuardada } from '../../core/sesion/almacen-sesion';
import { SesionService } from '../../core/sesion/sesion.service';
import { respuestaDeSesion } from '../../core/sesion/testing';
import { MenuUsuario } from './menu-usuario';

@Component({ template: '' })
class Vacio {}

describe('MenuUsuario', () => {
  let fixture: ComponentFixture<MenuUsuario>;
  let http: HttpTestingController;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const boton = () => raiz().querySelector<HTMLButtonElement>('.menu__boton')!;
  const panel = () => raiz().querySelector('.menu__panel');

  function crear(rol: Rol = 'TURISTA', nombre = 'Laura'): void {
    const sesion = respuestaDeSesion({ rol });
    localStorage.setItem(
      CLAVE_SESION,
      JSON.stringify(aSesionGuardada({ ...sesion, cuenta: { ...sesion.cuenta, nombre } })),
    );
    fixture = TestBed.createComponent(MenuUsuario);
    fixture.detectChanges();
  }

  function abrir(): void {
    boton().click();
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
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('muestra el nombre, su inicial y el menú cerrado', () => {
    crear();

    expect(raiz().querySelector('.menu__nombre')?.textContent).toBe('Laura');
    expect(raiz().querySelector('.menu__avatar')?.textContent).toBe('L');
    expect(boton().getAttribute('aria-expanded')).toBe('false');
    expect(panel()).toBeNull();
  });

  it('anuncia a los lectores de pantalla que es el menú de la cuenta', () => {
    crear();

    expect(boton().textContent).toContain('menú de la cuenta');
  });

  it('la inicial va en mayúscula aunque el nombre tenga espacios y minúscula al comienzo', () => {
    crear('TURISTA', '  ana');

    expect(raiz().querySelector('.menu__avatar')?.textContent).toBe('A');
  });

  it('no muestra nada sin sesión', () => {
    fixture = TestBed.createComponent(MenuUsuario);
    fixture.detectChanges();

    expect(raiz().querySelector('.menu')).toBeNull();
  });

  describe('abierto', () => {
    it('muestra los datos de la cuenta, "Mis datos" y "Cerrar sesión"', () => {
      crear('PRESTADOR', 'Hotel Sierra');

      abrir();

      expect(boton().getAttribute('aria-expanded')).toBe('true');
      expect(panel()?.textContent).toContain('Hotel Sierra');
      expect(panel()?.textContent).toContain('Prestador');
      expect(panel()?.textContent).toContain('laura@demo.samariantour.co');
      expect(panel()?.querySelector('a')?.getAttribute('href')).toBe('/mis-datos');
      expect(panel()?.querySelector('button')?.textContent).toContain('Cerrar sesión');
    });

    it('el botón lo vuelve a cerrar', () => {
      crear();
      abrir();

      abrir();

      expect(panel()).toBeNull();
    });

    it('Escape lo cierra y devuelve el foco al botón', () => {
      crear();
      abrir();

      raiz().dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
      fixture.detectChanges();

      expect(panel()).toBeNull();
      expect(document.activeElement).toBe(boton());
    });

    it('un clic fuera lo cierra', () => {
      crear();
      abrir();

      document.body.click();
      fixture.detectChanges();

      expect(panel()).toBeNull();
    });

    it('un clic dentro del panel no lo cierra', () => {
      crear();
      abrir();

      raiz().querySelector<HTMLElement>('.menu__datos')!.click();
      fixture.detectChanges();

      expect(panel()).not.toBeNull();
    });

    it('al salir del menú con el teclado se cierra', () => {
      crear();
      abrir();
      const afuera = document.createElement('button');
      document.body.appendChild(afuera);

      raiz()
        .querySelector('.menu')!
        .dispatchEvent(new FocusEvent('focusout', { relatedTarget: afuera, bubbles: true }));
      fixture.detectChanges();

      expect(panel()).toBeNull();
      afuera.remove();
    });

    it('al pasar el foco a otro elemento del menú sigue abierto', () => {
      crear();
      abrir();

      raiz()
        .querySelector('.menu')!
        .dispatchEvent(
          new FocusEvent('focusout', { relatedTarget: panel()!.querySelector('a'), bubbles: true }),
        );
      fixture.detectChanges();

      expect(panel()).not.toBeNull();
    });

    it('elegir "Mis datos" lo cierra', () => {
      crear();
      abrir();

      panel()!.querySelector<HTMLAnchorElement>('a')!.click();
      fixture.detectChanges();

      expect(panel()).toBeNull();
    });
  });

  describe('cerrar sesión', () => {
    it('avisa al servidor, cierra la sesión local, confirma y lleva al catálogo', () => {
      crear();
      const ir = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
      abrir();

      panel()!.querySelector<HTMLButtonElement>('button')!.click();
      http
        .expectOne('/api/nucleo/cuentas/sesion/cierre')
        .flush(null, { status: 204, statusText: 'No Content' });
      fixture.detectChanges();

      expect(TestBed.inject(SesionService).autenticada()).toBe(false);
      expect(
        TestBed.inject(AvisosService)
          .avisos()
          .map((aviso) => aviso.texto),
      ).toEqual(['Cerraste sesión']);
      expect(ir).toHaveBeenCalledWith('/');
      expect(panel()).toBeNull();
    });
  });
});
