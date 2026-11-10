import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Zona } from '../../../../core/api/modelos';
import { establecerIdioma } from '../../../../core/idioma/idioma';
import { FiltrosRecomendacion, SIN_FILTROS } from '../../consulta';
import { FiltrosConsulta } from './filtros-consulta';

const ZONAS = [
  { id: 'minca', nombre: 'Minca', areaProtegida: false, altaAfluencia: false, activa: true, buenasPracticas: [] },
  { id: 'taganga', nombre: 'Taganga', areaProtegida: false, altaAfluencia: true, activa: true, buenasPracticas: [] },
] as Zona[];

describe('FiltrosConsulta', () => {
  let fixture: ComponentFixture<FiltrosConsulta>;
  let emitidos: FiltrosRecomendacion[];

  const raiz = () => fixture.nativeElement as HTMLElement;
  const ultimo = () => emitidos[emitidos.length - 1];

  function crear(filtros: FiltrosRecomendacion = SIN_FILTROS, zonas: readonly Zona[] = ZONAS): void {
    fixture = TestBed.createComponent(FiltrosConsulta);
    fixture.componentRef.setInput('filtros', filtros);
    fixture.componentRef.setInput('zonas', zonas);
    emitidos = [];
    fixture.componentInstance.filtrosChange.subscribe((f) => emitidos.push(f));
    fixture.detectChanges();
  }

  function marcar(texto: string, marcado = true): void {
    const caja = Array.from(raiz().querySelectorAll<HTMLLabelElement>('label.opcion'))
      .find((l) => l.textContent?.trim() === texto)!
      .querySelector<HTMLInputElement>('input')!;
    caja.checked = marcado;
    caja.dispatchEvent(new Event('change'));
  }

  function elegir(id: string, valor: string): void {
    const select = raiz().querySelector<HTMLSelectElement>(`#${id}`)!;
    select.value = valor;
    select.dispatchEvent(new Event('change'));
  }

  beforeEach(() => establecerIdioma('es', { recordar: false }));
  afterEach(() => establecerIdioma('es', { recordar: false }));

  it('está plegado: escribir lo que se busca sigue siendo lo primero', () => {
    crear();

    expect(raiz().querySelector('details')!.open).toBe(false);
    expect(raiz().querySelector('summary')?.textContent).toContain('Afinar la búsqueda');
  });

  it('explica cuáles filtros son estrictos', () => {
    crear();

    expect(raiz().textContent).toContain('nunca se relajan');
  });

  it('ofrece los cuatro tipos de oferta y los doce tipos de actividad', () => {
    crear();

    const grupos = Array.from(raiz().querySelectorAll('fieldset'));
    expect(grupos[0].querySelectorAll('input[type="checkbox"]')).toHaveLength(4);
    expect(grupos[1].querySelectorAll('input[type="checkbox"]')).toHaveLength(12);
  });

  it('marcar un tipo de oferta emite los filtros con ese tipo y no cambia lo demás', () => {
    crear({ ...SIN_FILTROS, zonaId: 'minca' });

    marcar('Actividad');

    expect(ultimo()).toEqual({ ...SIN_FILTROS, zonaId: 'minca', tiposOferta: ['ACTIVIDAD'] });
  });

  it('desmarcarlo lo quita', () => {
    crear({ ...SIN_FILTROS, tiposOferta: ['ACTIVIDAD', 'PAQUETE'] });

    marcar('Actividad', false);

    expect(ultimo().tiposOferta).toEqual(['PAQUETE']);
  });

  it('refleja los filtros que recibe: las casillas marcadas', () => {
    crear({ ...SIN_FILTROS, tiposActividad: ['SENDERISMO'], accesibleSillaRuedas: true });

    const marcadas = Array.from(raiz().querySelectorAll<HTMLInputElement>('input[type="checkbox"]:checked')).map(
      (c) => c.closest('label')?.textContent?.trim(),
    );
    expect(marcadas).toEqual(['Senderismo', 'Accesible en silla de ruedas']);
  });

  it('marcar un tipo de actividad lo emite', () => {
    crear();

    marcar('Senderismo');

    expect(ultimo().tiposActividad).toEqual(['SENDERISMO']);
  });

  it('elegir una zona emite su id; volver a "Cualquiera" lo quita', () => {
    crear();

    elegir('filtro-zona', 'minca');
    expect(ultimo().zonaId).toBe('minca');

    elegir('filtro-zona', '');
    expect(ultimo().zonaId).toBe('');
  });

  it('sin zonas del catálogo no ofrece el filtro de zona y lo demás sigue', () => {
    crear(SIN_FILTROS, []);

    expect(raiz().querySelector('#filtro-zona')).toBeNull();
    expect(raiz().querySelector('#filtro-esfuerzo')).not.toBeNull();
    expect(raiz().querySelector('#filtro-precio')).not.toBeNull();
  });

  it('elegir el esfuerzo máximo lo emite', () => {
    crear();

    elegir('filtro-esfuerzo', 'MODERADO');

    expect(ultimo().nivelEsfuerzoMaximo).toBe('MODERADO');
  });

  it('escribir un precio máximo lo emite como número, y vaciarlo lo quita', () => {
    crear();
    const campo = raiz().querySelector<HTMLInputElement>('#filtro-precio')!;

    campo.value = '150000';
    campo.dispatchEvent(new Event('input'));
    expect(ultimo().precioMaximo).toBe(150000);

    campo.value = '';
    campo.dispatchEvent(new Event('input'));
    expect(ultimo().precioMaximo).toBeNull();
  });

  it('un precio de cero se emite tal cual: la consulta es la que avisa', () => {
    crear();
    const campo = raiz().querySelector<HTMLInputElement>('#filtro-precio')!;

    campo.value = '0';
    campo.dispatchEvent(new Event('input'));

    expect(ultimo().precioMaximo).toBe(0);
  });

  it('marcar la accesibilidad la emite', () => {
    crear();

    marcar('Accesible en silla de ruedas');

    expect(ultimo().accesibleSillaRuedas).toBe(true);
  });

  it('con filtros elegidos lo dice junto al título y ofrece quitarlos; sin ellos, no', () => {
    crear();
    expect(raiz().querySelector('summary .etiqueta')).toBeNull();
    expect(Array.from(raiz().querySelectorAll('button')).some((b) => b.textContent?.includes('Quitar'))).toBe(false);

    crear({ ...SIN_FILTROS, accesibleSillaRuedas: true });

    expect(raiz().querySelector('summary .etiqueta')?.textContent).toBe('Con filtros elegidos');
    raiz().querySelector<HTMLButtonElement>('button')!.click();
    expect(ultimo()).toEqual(SIN_FILTROS);
  });

  it('en inglés cambian los grupos, los tipos y el nombre de las opciones', () => {
    establecerIdioma('en', { recordar: false });
    crear();

    expect(raiz().querySelector('summary')?.textContent).toContain('Refine the search');
    expect(Array.from(raiz().querySelectorAll('legend')).map((l) => l.textContent?.trim())).toEqual(['Offer type', 'Activity type']);
    expect(raiz().querySelector('label[for="filtro-zona"]')?.textContent).toBe('Area');
    expect(raiz().textContent).toContain('Wheelchair accessible');
  });
});
