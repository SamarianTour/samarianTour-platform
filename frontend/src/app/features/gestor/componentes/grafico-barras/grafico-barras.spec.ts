import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BarraGrafico, GraficoBarras } from './grafico-barras';

const BARRAS: BarraGrafico[] = [
  {
    id: 'a',
    etiqueta: 'Parque Tayrona',
    detalle: 'Actividad',
    marcas: [{ texto: 'Área protegida', clase: 'etiqueta--protegida' }],
    ancho: 100,
    valor: '18',
  },
  { id: 'b', etiqueta: 'Minca', detalle: 'Alojamiento', ancho: 8, valor: 'menos de 3', omitida: true },
];

describe('GraficoBarras', () => {
  let fixture: ComponentFixture<GraficoBarras>;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const barras = () => Array.from(raiz().querySelectorAll<HTMLElement>('.barra'));

  beforeEach(() => {
    fixture = TestBed.createComponent(GraficoBarras);
    fixture.componentRef.setInput('barras', BARRAS);
    fixture.componentRef.setInput('etiqueta', 'Resumen del gráfico');
    fixture.detectChanges();
  });

  it('es una sola imagen para los lectores de pantalla, con el resumen como nombre', () => {
    const grafico = raiz().querySelector('.barras')!;

    expect(grafico.getAttribute('role')).toBe('img');
    expect(grafico.getAttribute('aria-label')).toBe('Resumen del gráfico');
  });

  it('dibuja una barra por dato, con su nombre, su detalle y su valor escritos', () => {
    expect(barras()).toHaveLength(2);
    expect(barras()[0].querySelector('.barra__etiqueta')?.textContent).toBe('Parque Tayrona');
    expect(barras()[0].querySelector('.barra__detalle')?.textContent).toBe('Actividad');
    expect(barras()[0].querySelector('.barra__valor')?.textContent).toBe('18');
  });

  it('el largo de la barra es su ancho', () => {
    const relleno = (i: number) => barras()[i].querySelector<HTMLElement>('.barra__relleno')!.style.width;

    expect(relleno(0)).toBe('100%');
    expect(relleno(1)).toBe('8%');
  });

  it('muestra las marcas de la barra con su clase', () => {
    const marca = barras()[0].querySelector('.etiqueta');

    expect(marca?.textContent).toBe('Área protegida');
    expect(marca?.classList).toContain('etiqueta--protegida');
    expect(barras()[1].querySelector('.etiqueta')).toBeNull();
  });

  it('un conteo omitido va rayado y dice "menos de 3": el color no es lo único que informa', () => {
    expect(barras()[1].classList).toContain('barra--omitida');
    expect(barras()[1].querySelector('.barra__valor')?.textContent).toBe('menos de 3');
    expect(barras()[0].classList).not.toContain('barra--omitida');
  });

  it('la pista y el relleno son decoración: el valor está escrito aparte', () => {
    // El gráfico entero es una imagen; lo que cuenta está en el texto y en la tabla de datos.
    expect(raiz().querySelectorAll('.barra__valor')).toHaveLength(2);
  });
});
