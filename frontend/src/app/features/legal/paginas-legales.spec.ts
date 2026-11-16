import { Type } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { establecerIdioma } from '../../core/idioma/idioma';
import { RUTAS_LEGAL } from './legal.routes';
import { Pqrsf } from './pqrsf/pqrsf';
import { ProteccionConsumidor } from './proteccion-consumidor/proteccion-consumidor';
import { Terminos } from './terminos/terminos';

describe('Páginas legales de texto', () => {
  let harness: RouterTestingHarness;

  const raiz = () => harness.routeNativeElement as HTMLElement;
  const texto = () => (raiz().textContent ?? '').replace(/\s+/g, ' ');

  beforeEach(async () => {
    establecerIdioma('es', { recordar: false });
    TestBed.configureTestingModule({ providers: [provideRouter(RUTAS_LEGAL)] });
    harness = await RouterTestingHarness.create();
  });

  afterEach(() => establecerIdioma('es', { recordar: false }));

  it.each([
    ['/terminos', Terminos],
    ['/proteccion-consumidor', ProteccionConsumidor],
    ['/pqrsf', Pqrsf],
  ] as const)('%s crea su pantalla, con el aviso de proyecto académico y el correo', async (url, pantalla) => {
    expect(await harness.navigateByUrl(url, pantalla as Type<unknown>)).toBeInstanceOf(pantalla);

    expect(texto()).toContain('Texto de demostración del prototipo académico. No ha sido revisado por un abogado.');
    expect(raiz().querySelector('.alerta--aviso')).not.toBeNull();
    expect(raiz().querySelector<HTMLAnchorElement>('.legal__contacto a')?.getAttribute('href')).toBe(
      'mailto:contacto@samariantour.com',
    );
    expect(raiz().querySelectorAll('h1')).toHaveLength(1);
  });

  it('los términos dicen que la solicitud no es una reserva, que el pago es externo y que la comisión es del prestador', async () => {
    await harness.navigateByUrl('/terminos', Terminos);

    expect(texto()).toContain('Una solicitud de disponibilidad no es una reserva');
    expect(texto()).toContain('pasarela de pago externa');
    expect(texto()).toContain('se descuenta al prestador');
    expect(texto()).toContain('no paga cargos adicionales');
  });

  it('protección al consumidor nombra la Ley 1480 de 2011 y a la Superintendencia de Industria y Comercio', async () => {
    await harness.navigateByUrl('/proteccion-consumidor', ProteccionConsumidor);

    expect(texto()).toContain('Estatuto del Consumidor (Ley 1480 de 2011)');
    expect(texto()).toContain('Superintendencia de Industria y Comercio');
    expect(texto()).toContain('Información clara del precio');
  });

  it('PQRSF explica cada letra y qué datos incluir, sin prometer plazos', async () => {
    await harness.navigateByUrl('/pqrsf', Pqrsf);

    expect(raiz().querySelectorAll('.legal__seccion')[0].querySelectorAll('li')).toHaveLength(5);
    expect(texto()).toContain('Qué datos incluir');
    expect(texto()).not.toMatch(/\d+\s*(días|horas)/i);
  });

  it('cambia a inglés con la pantalla abierta', async () => {
    await harness.navigateByUrl('/terminos', Terminos);

    establecerIdioma('en', { recordar: false });
    harness.detectChanges();

    expect(raiz().querySelector('h1')?.textContent).toBe('Terms and conditions');
    expect(texto()).toContain('Demo text for the academic prototype. It has not been reviewed by a lawyer.');
    expect(texto()).toContain('An availability request is not a booking');
  });

  it('las rutas legales tienen un título traducible', () => {
    expect(RUTAS_LEGAL.map((ruta) => [ruta.path, ruta.title])).toEqual([
      ['terminos', 'app.titulo.terminos'],
      ['proteccion-consumidor', 'app.titulo.proteccionConsumidor'],
      ['pqrsf', 'app.titulo.pqrsf'],
      ['politica-datos', 'app.titulo.politicaDatos'],
    ]);
  });
});
