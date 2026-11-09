import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { VisorPolitica } from './visor-politica';

const URL = '/api/nucleo/politica-datos';

describe('VisorPolitica', () => {
  let fixture: ComponentFixture<VisorPolitica>;
  let http: HttpTestingController;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const dialogo = () => raiz().querySelector<HTMLDialogElement>('dialog')!;
  const texto = () => raiz().querySelector('.visor__texto');

  function abrir(): void {
    fixture.componentInstance.abrir();
    fixture.detectChanges();
  }

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
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(VisorPolitica);
    fixture.componentRef.setInput('version', 'v1');
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('no pide el texto mientras no se abre', () => {
    http.expectNone(`${URL}/v1`);
    expect(dialogo().hasAttribute('open')).toBe(false);
  });

  it('al abrirse pide el texto de la versión y mientras tanto avisa que carga', () => {
    abrir();

    expect(dialogo().hasAttribute('open')).toBe(true);
    expect(raiz().textContent).toContain('Versión v1');
    expect(raiz().textContent).toContain('Cargando el texto…');
    http.expectOne(`${URL}/v1`).flush('# Política');
  });

  it('muestra el texto respetando los saltos de línea', () => {
    abrir();

    http.expectOne(`${URL}/v1`).flush('# Política de datos\n\nPrimera línea.\nSegunda línea.');
    fixture.detectChanges();

    // Los saltos de línea llegan intactos al elemento; el CSS (white-space: pre-wrap) es el que los muestra.
    expect(texto()?.textContent).toBe('# Política de datos\n\nPrimera línea.\nSegunda línea.');
  });

  it('muestra el texto como texto: el HTML que contenga no se ejecuta ni se dibuja', () => {
    abrir();

    http.expectOne(`${URL}/v1`).flush('<img src=x onerror="alert(1)"><script>alert(2)</script>');
    fixture.detectChanges();

    expect(raiz().querySelector('img')).toBeNull();
    expect(raiz().querySelector('script')).toBeNull();
    expect(texto()?.textContent).toContain('<script>alert(2)</script>');
  });

  it('al reabrirlo no vuelve a pedir el mismo texto', () => {
    abrir();
    http.expectOne(`${URL}/v1`).flush('Texto');
    fixture.detectChanges();
    dialogo().querySelector<HTMLButtonElement>('.visor__pie button')!.click();

    abrir();

    http.expectNone(`${URL}/v1`);
    expect(texto()?.textContent).toBe('Texto');
  });

  it('si la versión vigente cambió, pide el texto de la nueva', () => {
    abrir();
    http.expectOne(`${URL}/v1`).flush('Texto uno');
    fixture.detectChanges();

    fixture.componentRef.setInput('version', 'v2');
    abrir();

    http.expectOne(`${URL}/v2`).flush('Texto dos');
    fixture.detectChanges();
    expect(texto()?.textContent).toBe('Texto dos');
  });

  it('si falla, lo dice y permite intentarlo de nuevo', () => {
    abrir();
    http.expectOne(`${URL}/v1`).error(new ProgressEvent('error'));
    fixture.detectChanges();

    expect(raiz().textContent).toContain('No pudimos conectar con el servidor');
    raiz().querySelector<HTMLButtonElement>('.visor__cuerpo button')!.click();
    http.expectOne(`${URL}/v1`).flush('Ahora sí');
    fixture.detectChanges();

    expect(texto()?.textContent).toBe('Ahora sí');
  });

  it('el botón Cerrar lo cierra', () => {
    abrir();
    http.expectOne(`${URL}/v1`).flush('Texto');

    dialogo().querySelector<HTMLButtonElement>('.visor__pie button')!.click();

    expect(dialogo().hasAttribute('open')).toBe(false);
  });
});
