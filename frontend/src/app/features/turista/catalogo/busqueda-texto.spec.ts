import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { Catalogo } from './catalogo';

const PAGINA_VACIA = { contenido: [], pagina: 0, tamano: 12, totalElementos: 0, totalPaginas: 0 };

describe('Catálogo: búsqueda por texto', () => {
  let http: HttpTestingController;
  let harness: RouterTestingHarness;

  const campo = () => harness.routeNativeElement!.querySelector<HTMLInputElement>('#texto')!;
  const ofertas = () => http.match((p) => p.url === '/api/catalogo/ofertas');

  async function escribir(valor: string): Promise<void> {
    campo().value = valor;
    campo().dispatchEvent(new Event('input'));
    // El formulario espera 350 ms antes de cambiar la URL.
    await new Promise((resolver) => setTimeout(resolver, 450));
    harness.detectChanges();
  }

  beforeEach(async () => {
    localStorage.clear();
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'catalogo', component: Catalogo }]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
  });

  afterEach(() => http.match(() => true).forEach((p) => p.error(new ProgressEvent('error'))));

  it('la ruta crea el catálogo y lee el texto de la URL', async () => {
    const componente = await harness.navigateByUrl('/catalogo?texto=buceo', Catalogo);

    expect(componente).toBeInstanceOf(Catalogo);
    expect(campo().value).toBe('buceo');
    expect(ofertas().some((p) => p.request.params.get('texto') === 'buceo')).toBe(true);
  });

  it('escribir 2 o más caracteres busca con ese texto y lo deja en la URL', async () => {
    await harness.navigateByUrl('/catalogo', Catalogo);
    ofertas().forEach((p) => p.flush(PAGINA_VACIA));

    await escribir('  buce ');

    expect(TestBed.inject(Router).url).toBe('/catalogo?texto=buce');
    expect(ofertas().some((p) => p.request.params.get('texto') === 'buce')).toBe(true);
  });

  it('con un solo carácter avisa, no busca y no borra lo escrito', async () => {
    await harness.navigateByUrl('/catalogo', Catalogo);
    ofertas().forEach((p) => p.flush(PAGINA_VACIA));

    await escribir('b');

    expect(ofertas().length).toBe(0);
    expect(campo().value).toBe('b');
    expect(campo().getAttribute('aria-invalid')).toBe('true');
    expect(harness.routeNativeElement!.textContent).toContain('Escribe al menos 2 caracteres');
  });

  it('"Borrar búsqueda" vacía el campo y quita el texto de la URL', async () => {
    await harness.navigateByUrl('/catalogo?texto=buceo', Catalogo);
    ofertas().forEach((p) => p.flush(PAGINA_VACIA));
    harness.detectChanges();

    harness.routeNativeElement!.querySelector<HTMLButtonElement>('.enlace-borrar')!.click();
    await new Promise((resolver) => setTimeout(resolver, 450));
    harness.detectChanges();

    expect(campo().value).toBe('');
    expect(TestBed.inject(Router).url).toBe('/catalogo');
  });
});
