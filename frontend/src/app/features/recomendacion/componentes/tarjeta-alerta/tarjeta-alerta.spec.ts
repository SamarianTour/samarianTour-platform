import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Alerta } from '../../../../core/api/modelos-recomendacion';
import { establecerIdioma } from '../../../../core/idioma/idioma';
import { TarjetaAlerta } from './tarjeta-alerta';

const IDEAM: Alerta = {
  id: 'ideam-hidrologica-a1',
  tipo: 'HIDROLOGICA',
  nivel: 'AMARILLA',
  titulo: 'Alerta hidrológica en la subzona Río Piedras - Río Manzanares',
  descripcion: 'Antes de visitar ríos y quebradas, consulta las indicaciones de las autoridades locales.',
  fuente: 'IDEAM, alertas hidrológicas vigentes',
};

const SIMULADA: Alerta = {
  id: 'simulada-tayrona-cierre',
  tipo: 'CIERRE',
  nivel: 'NARANJA',
  titulo: 'Cierre temporal del parque',
  descripcion: 'Consulta la fecha de reapertura antes de viajar.',
  fuente: 'Parques Nacionales (simulado)',
  emitidaEn: '2026-10-24T21:42:10.512Z',
  vigenteHasta: '2026-10-27T21:42:10.512Z',
};

describe('TarjetaAlerta', () => {
  let fixture: ComponentFixture<TarjetaAlerta>;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const normal = (texto: string | null | undefined) => (texto ?? '').replace(/[  ]/g, ' ').replace(/\s+/g, ' ').trim();

  /** Cada dato es un par etiqueta/valor (`dt`/`dd`); el espacio entre ambos lo pone el estilo. */
  const datos = () =>
    Array.from(raiz().querySelectorAll('dl > div')).map(
      (d) => `${normal(d.querySelector('dt')?.textContent)} ${normal(d.querySelector('dd')?.textContent)}`,
    );

  function crear(alerta: Alerta): void {
    fixture = TestBed.createComponent(TarjetaAlerta);
    fixture.componentRef.setInput('alerta', alerta);
    fixture.detectChanges();
  }

  beforeEach(() => establecerIdioma('es', { recordar: false }));
  afterEach(() => establecerIdioma('es', { recordar: false }));

  it('muestra el nivel con su nombre, el tipo, el título, la descripción y la fuente', () => {
    crear(IDEAM);

    expect(normal(raiz().querySelector('.tarjeta-alerta__nivel')?.textContent)).toBe('Nivel: Amarilla');
    expect(raiz().querySelector('.tarjeta-alerta__tipo')?.textContent).toBe('Hidrológica');
    expect(raiz().querySelector('h3')?.textContent).toBe(IDEAM.titulo);
    expect(raiz().querySelector('.tarjeta-alerta__descripcion')?.textContent).toBe(IDEAM.descripcion);
    expect(datos()).toEqual(['Fuente: IDEAM, alertas hidrológicas vigentes']);
  });

  it.each([
    ['AMARILLA', 'tarjeta-alerta--amarilla', 'Amarilla'],
    ['NARANJA', 'tarjeta-alerta--naranja', 'Naranja'],
    ['ROJA', 'tarjeta-alerta--roja', 'Roja'],
  ] as const)('el nivel %s se distingue por color, icono y nombre', (nivel, clase, nombre) => {
    crear({ ...IDEAM, nivel });

    expect(raiz().querySelector('article')?.classList).toContain(clase);
    expect(raiz().querySelector('.tarjeta-alerta__nivel app-icono')).not.toBeNull();
    expect(raiz().querySelector('.tarjeta-alerta__nivel')?.textContent).toContain(nombre);
  });

  it('la fuente simulada se muestra tal cual, sin presentarse como oficial', () => {
    crear(SIMULADA);

    expect(raiz().querySelector('dl')?.textContent).toContain('Parques Nacionales (simulado)');
    expect(raiz().textContent).not.toContain('IDEAM');
  });

  it('muestra cuándo se emitió y hasta cuándo vale, solo si el servicio lo da', () => {
    crear(SIMULADA);
    expect(datos()).toHaveLength(3);
    expect(datos()[1]).toMatch(/^Emitida: 24 oct 2026/);
    expect(datos()[2]).toMatch(/^Vigente hasta: 27 oct 2026/);
    expect(raiz().querySelectorAll('time')).toHaveLength(2);

    crear(IDEAM);
    expect(raiz().querySelectorAll('dl > div')).toHaveLength(1);
    expect(raiz().querySelector('time')).toBeNull();
  });

  it('sin descripción no dibuja un párrafo vacío', () => {
    crear({ ...IDEAM, descripcion: undefined });

    expect(raiz().querySelector('.tarjeta-alerta__descripcion')).toBeNull();
  });

  it('el título y la descripción vienen del servicio y se muestran tal cual; lo demás sigue el idioma', () => {
    establecerIdioma('en', { recordar: false });
    crear({ ...SIMULADA, titulo: 'Temporary park closure', descripcion: 'Check the reopening date before you travel.' });

    expect(raiz().querySelector('h3')?.textContent).toBe('Temporary park closure');
    expect(normal(raiz().querySelector('.tarjeta-alerta__nivel')?.textContent)).toBe('Level: Orange');
    expect(raiz().querySelector('.tarjeta-alerta__tipo')?.textContent).toBe('Closure');
    expect(datos()[0]).toBe('Source: Parques Nacionales (simulado)');
    expect(datos()[1]).toMatch(/^Issued: Oct 24, 2026/);
    expect(datos()[2]).toMatch(/^Valid until: Oct 27, 2026/);
  });
});
