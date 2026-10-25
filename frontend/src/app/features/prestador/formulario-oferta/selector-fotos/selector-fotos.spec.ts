import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  TestRequest,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Foto } from '../../../../core/api/modelos';
import { SelectorFotos } from './selector-fotos';

const URL = '/api/catalogo/prestador/fotos';

function foto(id: string): Foto {
  return {
    id,
    url: `/api/catalogo/fotos/${id}/galeria`,
    urlMiniatura: `/api/catalogo/fotos/${id}/miniatura`,
  };
}

function archivo(nombre: string, tipo = 'image/jpeg'): File {
  return new File(['contenido'], nombre, { type: tipo });
}

describe('SelectorFotos', () => {
  let fixture: ComponentFixture<SelectorFotos>;
  let http: HttpTestingController;

  const ids = () => fixture.componentInstance.fotos().map((una) => una.id);
  const texto = () => (fixture.nativeElement as HTMLElement).textContent ?? '';

  function elegir(archivos: File[]): void {
    const entrada = (fixture.nativeElement as HTMLElement).querySelector<HTMLInputElement>(
      'input[type="file"]',
    )!;
    Object.defineProperty(entrada, 'files', { value: archivos, configurable: true });
    entrada.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  }

  function pulsar(selector: string): void {
    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>(selector)!.click();
    fixture.detectChanges();
  }

  function responder(peticion: TestRequest, id: string): void {
    peticion.flush(foto(id), { status: 201, statusText: 'Created' });
    fixture.detectChanges();
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(SelectorFotos);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('sube las fotos elegidas una por una y las agrega en el orden en que se eligieron', () => {
    elegir([archivo('uno.jpg'), archivo('dos.jpg')]);

    // La segunda no sale hasta que termina la primera.
    const primera = http.expectOne(URL);
    expect((primera.request.body as FormData).get('foto')).toBeInstanceOf(File);
    expect(fixture.componentInstance.subiendo()).toBe(true);
    responder(primera, 'a');
    responder(http.expectOne(URL), 'b');

    expect(ids()).toEqual(['a', 'b']);
    expect(fixture.componentInstance.subiendo()).toBe(false);
    expect(texto()).toContain('2 de 6');
  });

  it('no sube un archivo que no es foto y dice por qué', () => {
    elegir([archivo('animada.gif', 'image/gif')]);

    http.expectNone(URL);
    expect(texto()).toContain('No se subió «animada.gif»: solo se aceptan fotos JPG, PNG o WebP.');
    expect(ids()).toEqual([]);
  });

  it('no sube más fotos de las que caben en la oferta', () => {
    fixture.componentRef.setInput('fotos', ['a', 'b', 'c', 'd', 'e'].map(foto));
    fixture.detectChanges();

    elegir([archivo('uno.jpg'), archivo('dos.jpg')]);

    responder(http.expectOne(URL), 'f');
    http.expectNone(URL);
    expect(ids()).toHaveLength(6);
    expect(texto()).toContain('No se subió «dos.jpg»: una oferta lleva máximo 6 fotos.');
    expect(texto()).toContain('Llegaste al máximo de 6 fotos.');
  });

  it('deja la subida fallida como pendiente, con lo que dice la API, y permite reintentarla', () => {
    elegir([archivo('pesada.jpg')]);

    http
      .expectOne(URL)
      .flush(
        { status: 400, detail: 'foto: pesa más de 5 MB', codigo: 'DATO_INVALIDO' },
        { status: 400, statusText: 'Bad Request' },
      );
    fixture.detectChanges();

    expect(ids()).toEqual([]);
    expect(fixture.componentInstance.subiendo()).toBe(false);
    expect(texto()).toContain('Pesa más de 5 MB.');

    pulsar('.miniatura--pendiente .miniatura__boton');
    expect(fixture.componentInstance.subiendo()).toBe(true);
    responder(http.expectOne(URL), 'a');

    expect(ids()).toEqual(['a']);
    expect(texto()).not.toContain('Pesa más de 5 MB.');
  });

  it('descarta una subida fallida sin llamar a la API', () => {
    elegir([archivo('mala.jpg')]);
    http.expectOne(URL).flush(null, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    pulsar('.miniatura--pendiente .miniatura__boton--quitar');

    expect(texto()).not.toContain('mala.jpg');
  });

  it('marca la primera foto como portada', () => {
    fixture.componentRef.setInput('fotos', ['a', 'b'].map(foto));
    fixture.detectChanges();

    const portadas = (fixture.nativeElement as HTMLElement).querySelectorAll('.miniatura__portada');
    expect(portadas).toHaveLength(1);
    expect(portadas[0].closest('li')?.querySelector('img')?.getAttribute('src')).toBe(
      '/api/catalogo/fotos/a/miniatura',
    );
  });

  it('cambia el orden con los botones y la foto que pasa al frente es la portada', () => {
    fixture.componentRef.setInput('fotos', ['a', 'b', 'c'].map(foto));
    fixture.detectChanges();

    pulsar('[data-foto="c"][data-accion="antes"]');
    expect(ids()).toEqual(['a', 'c', 'b']);
    pulsar('[data-foto="c"][data-accion="antes"]');
    expect(ids()).toEqual(['c', 'a', 'b']);
    expect(texto()).toContain('Ahora es la portada.');
  });

  it('no deja mover antes la primera foto ni después la última', () => {
    fixture.componentRef.setInput('fotos', ['a', 'b'].map(foto));
    fixture.detectChanges();
    const boton = (selector: string) =>
      (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>(selector)!;

    expect(boton('[data-foto="a"][data-accion="antes"]').disabled).toBe(true);
    expect(boton('[data-foto="b"][data-accion="despues"]').disabled).toBe(true);
    expect(boton('[data-foto="a"][data-accion="despues"]').disabled).toBe(false);
  });

  it('quita una foto y la siguiente pasa a ser la portada', () => {
    fixture.componentRef.setInput('fotos', ['a', 'b'].map(foto));
    fixture.detectChanges();

    pulsar('[data-foto="a"][data-accion="quitar"]');

    expect(ids()).toEqual(['b']);
    expect(texto()).toContain('Foto quitada.');
  });

  it('muestra el error del formulario junto al campo', () => {
    fixture.componentRef.setInput('error', 'Agrega al menos una foto.');
    fixture.detectChanges();

    const campo = (fixture.nativeElement as HTMLElement).querySelector('fieldset')!;
    expect(campo.getAttribute('aria-invalid')).toBe('true');
    expect(campo.querySelector('#fotos-error')?.textContent).toContain('Agrega al menos una foto.');
  });
});
