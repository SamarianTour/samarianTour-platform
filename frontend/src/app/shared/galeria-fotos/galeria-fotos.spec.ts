import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Foto } from '../../core/api/modelos';
import { GaleriaFotos } from './galeria-fotos';

function foto(id: string): Foto {
  return {
    id,
    url: `/api/catalogo/fotos/${id}/galeria`,
    urlMiniatura: `/api/catalogo/fotos/${id}/miniatura`,
  };
}

function fotos(cantidad: number): Foto[] {
  return Array.from({ length: cantidad }, (_, i) => foto(`f${i + 1}`));
}

describe('GaleriaFotos', () => {
  let fixture: ComponentFixture<GaleriaFotos>;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const mosaicos = () => Array.from(raiz().querySelectorAll<HTMLButtonElement>('.mosaico'));
  const visor = () => raiz().querySelector<HTMLDialogElement>('dialog')!;
  const fotoDelVisor = () => raiz().querySelector<HTMLImageElement>('.visor__foto')!;
  const pie = () => raiz().querySelector('.visor__pie')!.textContent?.trim();

  function mostrar(cantidad: number): void {
    fixture.componentRef.setInput('fotos', fotos(cantidad));
    fixture.componentRef.setInput('nombre', 'Caminata a Marinka');
    fixture.detectChanges();
  }

  function pulsar(elemento: HTMLElement): void {
    elemento.click();
    fixture.detectChanges();
  }

  const siguiente = () =>
    pulsar(raiz().querySelector<HTMLButtonElement>('.visor__nav:not(.visor__nav--anterior)')!);
  const anterior = () => pulsar(raiz().querySelector<HTMLButtonElement>('.visor__nav--anterior')!);

  beforeAll(() => {
    // jsdom no trae el comportamiento del <dialog>: se imita lo mínimo que usa el componente.
    HTMLDialogElement.prototype.showModal ??= function (this: HTMLDialogElement) {
      this.setAttribute('open', '');
    };
    HTMLDialogElement.prototype.close ??= function (this: HTMLDialogElement) {
      this.removeAttribute('open');
      this.dispatchEvent(new Event('close'));
    };
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(GaleriaFotos);
  });

  it('muestra una sola foto, grande y sin etiqueta de "ver las fotos"', () => {
    mostrar(1);

    expect(mosaicos()).toHaveLength(1);
    expect(raiz().querySelector('.mosaico__mas')).toBeNull();
    expect(raiz().querySelector('.galeria--una')).not.toBeNull();
  });

  it('muestra dos fotos lado a lado', () => {
    mostrar(2);

    expect(mosaicos()).toHaveLength(2);
    expect(raiz().querySelector('.galeria--dos')).not.toBeNull();
  });

  it('con tres fotos arma el mosaico completo, sin etiqueta de total', () => {
    mostrar(3);

    expect(mosaicos()).toHaveLength(3);
    expect(raiz().querySelector('.galeria--mosaico')).not.toBeNull();
    expect(mosaicos()[2].querySelector('.mosaico__mas:not(.mosaico__mas--movil)')).toBeNull();
  });

  it('con más de tres fotos muestra solo tres y una etiqueta con el total en la tercera', () => {
    mostrar(5);

    expect(mosaicos()).toHaveLength(3);
    expect(mosaicos()[2].querySelector('.mosaico__mas')?.textContent).toContain('Ver las 5 fotos');
  });

  it('la foto grande usa el tamaño de galería y las pequeñas la miniatura', () => {
    mostrar(3);

    const [grande, segunda] = mosaicos().map((mosaico) =>
      mosaico.querySelector('img')!.getAttribute('src'),
    );
    expect(grande).toBe('/api/catalogo/fotos/f1/galeria');
    expect(segunda).toBe('/api/catalogo/fotos/f2/miniatura');
  });

  it('cada foto se anuncia a los lectores de pantalla con su posición', () => {
    mostrar(4);

    expect(mosaicos()[1].textContent).toContain('Ver foto 2 de 4');
  });

  it('al pulsar una foto abre el visor en esa foto, en tamaño de galería', () => {
    mostrar(5);

    pulsar(mosaicos()[2]);

    expect(visor().hasAttribute('open')).toBe(true);
    expect(pie()).toBe('3 de 5');
    expect(fotoDelVisor().getAttribute('src')).toBe('/api/catalogo/fotos/f3/galeria');
    expect(fotoDelVisor().getAttribute('alt')).toBe('Foto 3 de 5 de «Caminata a Marinka»');
  });

  it('avanza y retrocede de forma circular', () => {
    mostrar(3);
    pulsar(mosaicos()[0]);

    anterior();
    expect(pie()).toBe('3 de 3');
    siguiente();
    expect(pie()).toBe('1 de 3');
    siguiente();
    expect(pie()).toBe('2 de 3');
  });

  it('también navega con las flechas del teclado', () => {
    mostrar(3);
    pulsar(mosaicos()[0]);

    visor().dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight', bubbles: true }));
    fixture.detectChanges();
    expect(pie()).toBe('2 de 3');
    visor().dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowLeft', bubbles: true }));
    fixture.detectChanges();
    expect(pie()).toBe('1 de 3');
  });

  it('con una sola foto no ofrece botones para navegar ni responde a las flechas', () => {
    mostrar(1);
    pulsar(mosaicos()[0]);

    expect(raiz().querySelector('.visor__nav')).toBeNull();
    visor().dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight', bubbles: true }));
    fixture.detectChanges();
    expect(pie()).toBe('1 de 1');
  });

  it('bloquea el desplazamiento de la página mientras el visor está abierto y lo libera al cerrar', () => {
    mostrar(2);

    pulsar(mosaicos()[0]);
    expect(document.body.style.overflow).toBe('hidden');
    pulsar(raiz().querySelector<HTMLButtonElement>('.visor__cerrar')!);

    expect(visor().hasAttribute('open')).toBe(false);
    expect(document.body.style.overflow).toBe('');
  });

  it('un clic sobre el fondo cierra el visor, pero uno sobre la foto no', () => {
    mostrar(2);
    pulsar(mosaicos()[0]);

    pulsar(fotoDelVisor());
    expect(visor().hasAttribute('open')).toBe(true);
    pulsar(visor());

    expect(visor().hasAttribute('open')).toBe(false);
  });
});
