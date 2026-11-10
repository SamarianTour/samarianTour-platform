import { ComponentFixture, TestBed } from '@angular/core/testing';
import { establecerIdioma } from '../../core/idioma/idioma';
import { PasoSolicitud, PasosSolicitud } from './pasos-solicitud';

describe('PasosSolicitud', () => {
  let fixture: ComponentFixture<PasosSolicitud>;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const pasos = () => Array.from(raiz().querySelectorAll<HTMLElement>('li.paso'));

  function crear(paso: PasoSolicitud): void {
    fixture = TestBed.createComponent(PasosSolicitud);
    fixture.componentRef.setInput('paso', paso);
    fixture.detectChanges();
  }

  it('muestra los cuatro pasos en orden', () => {
    crear('solicitar');

    expect(pasos().map((p) => p.querySelector('.paso__texto')?.textContent)).toEqual([
      'Solicitar',
      'Confirmar',
      'Pagar',
      'Listo',
    ]);
  });

  it('marca el paso actual para los lectores de pantalla y deja el resto sin marcar', () => {
    crear('confirmar');

    expect(pasos().map((p) => p.getAttribute('aria-current'))).toEqual([null, 'step', null, null]);
    expect(pasos()[1].textContent).toContain('(paso actual)');
  });

  it('los pasos anteriores quedan como completados, con texto además del color', () => {
    crear('pagar');

    expect(pasos()[0].classList).toContain('paso--hecho');
    expect(pasos()[1].classList).toContain('paso--hecho');
    expect(pasos()[2].classList).toContain('paso--actual');
    expect(pasos()[3].classList).not.toContain('paso--hecho');
    expect(pasos()[0].textContent).toContain('(completado)');
  });

  it('es una lista con nombre', () => {
    crear('solicitar');

    expect(raiz().querySelector('ol')?.getAttribute('aria-label')).toBe('Pasos de la solicitud');
  });

  it('cambia de idioma al instante', () => {
    crear('solicitar');

    establecerIdioma('en', { recordar: false });
    fixture.detectChanges();

    expect(pasos().map((p) => p.querySelector('.paso__texto')?.textContent)).toEqual([
      'Request',
      'Confirm',
      'Pay',
      'Done',
    ]);
    expect(raiz().querySelector('ol')?.getAttribute('aria-label')).toBe('Request steps');
  });
});
