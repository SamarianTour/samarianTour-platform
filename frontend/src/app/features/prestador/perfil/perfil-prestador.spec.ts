import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { Foto, PerfilPublico } from '../../../core/api/modelos';
import { AvisosService } from '../../../core/avisos/avisos.service';
import { erroresInterceptor } from '../../../core/errores/errores.interceptor';
import { establecerIdioma } from '../../../core/idioma/idioma';
import { CLAVE_SESION, aSesionGuardada } from '../../../core/sesion/almacen-sesion';
import { respuestaDeSesion } from '../../../core/sesion/testing';
import { PerfilPrestador } from './perfil-prestador';

const URL = '/api/catalogo/prestador/perfil';

function foto(id: string): Foto {
  return { id, url: `/api/catalogo/fotos/${id}/galeria`, urlMiniatura: `/api/catalogo/fotos/${id}/miniatura` };
}

function perfil(extra: Partial<PerfilPublico> = {}): PerfilPublico {
  return {
    nombreComercial: 'Mar y Selva',
    descripcion: 'Caminatas y kayak por la sierra.',
    fotos: [],
    horariosAtencion: 'Lunes a sábado',
    sitioWeb: 'https://marayselva.co',
    redesSociales: ['https://instagram.com/marayselva'],
    ...extra,
  };
}

function problema(status: number, codigo: string, detail = codigo) {
  return {
    body: { status, codigo, title: codigo, detail },
    init: { status, statusText: codigo, headers: { 'Content-Type': 'application/problem+json' } },
  };
}

describe('PerfilPrestador (P03.2)', () => {
  let harness: RouterTestingHarness;
  let http: HttpTestingController;
  let mostrar: ReturnType<typeof vi.spyOn>;

  const raiz = () => harness.routeNativeElement as HTMLElement;
  const texto = () => (raiz().textContent ?? '').replace(/[  ]/g, ' ').replace(/\s+/g, ' ');
  const campo = (id: string) => raiz().querySelector<HTMLInputElement>(`#perfil-${id}`)!;
  const selector = () => raiz().querySelector('app-selector-fotos');
  const miniaturas = () => raiz().querySelectorAll('.miniatura');
  const botonGuardar = () => raiz().querySelector<HTMLButtonElement>('.formulario form button[type="submit"]')!;

  /** Abre la pantalla y responde al perfil (`null` = 404, todavía sin perfil). */
  async function crear(existente: PerfilPublico | null): Promise<void> {
    await harness.navigateByUrl('/prestador/perfil', PerfilPrestador);
    harness.detectChanges();
    const peticion = http.expectOne(URL);
    if (existente) {
      peticion.flush(existente);
    } else {
      const { body, init } = problema(404, 'PERFIL_NO_ENCONTRADO');
      peticion.flush(body, init);
    }
    harness.detectChanges();
  }

  function escribir(id: string, valor: string): void {
    const entrada = campo(id);
    entrada.value = valor;
    entrada.dispatchEvent(new Event('input'));
    harness.detectChanges();
  }

  function guardar(): void {
    botonGuardar().click();
    harness.detectChanges();
  }

  function elegirFoto(nombre: string): void {
    const entrada = raiz().querySelector<HTMLInputElement>('app-selector-fotos input[type="file"]')!;
    Object.defineProperty(entrada, 'files', {
      value: [new File(['x'], nombre, { type: 'image/jpeg' })],
      configurable: true,
    });
    entrada.dispatchEvent(new Event('change'));
    harness.detectChanges();
  }

  beforeEach(async () => {
    localStorage.clear();
    sessionStorage.clear();
    establecerIdioma('es', { recordar: false });
    localStorage.setItem(CLAVE_SESION, JSON.stringify(aSesionGuardada(respuestaDeSesion({ rol: 'PRESTADOR' }))));
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'prestador/perfil', component: PerfilPrestador }]),
        provideHttpClient(withInterceptors([erroresInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    mostrar = vi.spyOn(TestBed.inject(AvisosService), 'mostrar');
    harness = await RouterTestingHarness.create();
  });

  afterEach(() => {
    establecerIdioma('es', { recordar: false });
    http.verify();
  });

  describe('sin perfil', () => {
    it('invita a crearlo y deja las fotos para después', async () => {
      await crear(null);

      expect(texto()).toContain('Todavía no tienes perfil público');
      expect(campo('descripcion').value).toBe('');
      expect(selector()).toBeNull();
      expect(texto()).toContain('Guarda primero los datos del perfil para poder agregar fotos.');
    });

    it('solo la descripción es obligatoria: sin ella no se envía nada', async () => {
      await crear(null);

      guardar();

      http.expectNone((p) => p.method === 'PUT');
      expect(texto()).toContain('Escribe la descripción de tu perfil.');
      expect(campo('descripcion').getAttribute('aria-invalid')).toBe('true');
    });

    it('un sitio web o una red sin http o https no se envía', async () => {
      await crear(null);
      escribir('descripcion', 'Hola');
      escribir('sitioWeb', 'marayselva.co');
      escribir('redesSociales', 'https://instagram.com/x\n@marayselva');

      guardar();

      http.expectNone((p) => p.method === 'PUT');
      expect(texto()).toContain('Escribe una dirección web que empiece por http:// o https://.');
      expect(texto()).toContain('Cada red social va en su propia línea');
    });

    it('con solo la descripción crea el perfil, avisa y habilita las fotos', async () => {
      await crear(null);
      escribir('descripcion', ' Caminatas y kayak ');

      guardar();
      guardar();

      const peticion = http.expectOne(URL);
      expect(peticion.request.method).toBe('PUT');
      expect(peticion.request.body).toEqual({ descripcion: 'Caminatas y kayak', redesSociales: [] });
      peticion.flush(perfil({ nombreComercial: undefined, horariosAtencion: undefined, sitioWeb: undefined, redesSociales: [] }));
      harness.detectChanges();

      expect(mostrar).toHaveBeenCalledWith('Creaste tu perfil público. Ya lo ven los turistas.');
      expect(selector()).not.toBeNull();
      expect(texto()).not.toContain('Todavía no tienes perfil público');
    });

    it('si la API señala un campo, el mensaje va en ese campo', async () => {
      await crear(null);
      escribir('descripcion', 'Hola');
      guardar();

      const { body, init } = problema(400, 'DATO_INVALIDO', 'sitioWeb: la dirección no es válida');
      http.expectOne(URL).flush(body, init);
      harness.detectChanges();

      expect(texto()).toContain('la dirección no es válida');
      expect(campo('sitioWeb').getAttribute('aria-invalid')).toBe('true');
    });
  });

  describe('con perfil', () => {
    it('el formulario nace con los datos y las redes una por línea', async () => {
      await crear(perfil({ redesSociales: ['https://a.co', 'https://b.co'] }));

      expect(campo('nombreComercial').value).toBe('Mar y Selva');
      expect(campo('descripcion').value).toBe('Caminatas y kayak por la sierra.');
      expect(campo('horariosAtencion').value).toBe('Lunes a sábado');
      expect(campo('sitioWeb').value).toBe('https://marayselva.co');
      expect(campo('redesSociales').value).toBe('https://a.co\nhttps://b.co');
    });

    it('muestra sus fotos, sin botones de orden ni portada', async () => {
      await crear(perfil({ fotos: [foto('a'), foto('b')] }));

      expect(miniaturas()).toHaveLength(2);
      expect(raiz().querySelectorAll('[data-accion="antes"], [data-accion="despues"]')).toHaveLength(0);
      expect(raiz().querySelector('.miniatura__portada')).toBeNull();
    });

    it('guardar cambios manda todo con PUT y avisa', async () => {
      await crear(perfil({ fotos: [foto('a')] }));
      escribir('descripcion', 'Nueva descripción');

      guardar();

      const peticion = http.expectOne(URL);
      expect(peticion.request.body).toEqual({
        nombreComercial: 'Mar y Selva',
        descripcion: 'Nueva descripción',
        horariosAtencion: 'Lunes a sábado',
        sitioWeb: 'https://marayselva.co',
        redesSociales: ['https://instagram.com/marayselva'],
      });
      peticion.flush(perfil({ descripcion: 'Nueva descripción', fotos: [foto('a')] }));
      harness.detectChanges();

      expect(mostrar).toHaveBeenCalledWith('Guardaste tu perfil público.');
      // El perfil conserva las fotos que ya tenía.
      expect(miniaturas()).toHaveLength(1);
    });

    it('dejar un campo opcional en blanco lo borra del perfil', async () => {
      await crear(perfil());
      escribir('sitioWeb', '');
      escribir('redesSociales', '');

      guardar();

      const cuerpo = http.expectOne(URL).request.body;
      expect('sitioWeb' in cuerpo).toBe(false);
      expect(cuerpo.redesSociales).toEqual([]);
      http.match(URL).forEach((p) => p.flush(perfil()));
    });

    it('si otra pestaña lo cambió al mismo tiempo, avisa y vuelve a leer el perfil', async () => {
      await crear(perfil());
      escribir('descripcion', 'Mi cambio');
      guardar();

      const { body, init } = problema(409, 'CONFLICTO_CONCURRENCIA');
      http.expectOne(URL).flush(body, init);
      harness.detectChanges();

      expect(raiz().querySelector('.alerta--error')).not.toBeNull();
      http.expectOne(URL).flush(perfil({ descripcion: 'Cambio de la otra pestaña' }));
      harness.detectChanges();
      // Lo que el prestador estaba escribiendo no se pierde.
      expect(campo('descripcion').value).toBe('Mi cambio');
    });
  });

  describe('las fotos', () => {
    it('sube con las rutas del perfil, no con las de las ofertas, y la agrega al final', async () => {
      await crear(perfil({ fotos: [foto('a')] }));

      elegirFoto('nueva.jpg');

      const peticion = http.expectOne(`${URL}/fotos`);
      expect(peticion.request.method).toBe('POST');
      expect((peticion.request.body as FormData).get('foto')).toBeInstanceOf(File);
      peticion.flush(perfil({ fotos: [foto('a'), foto('b')] }), { status: 201, statusText: 'Created' });
      harness.detectChanges();

      expect(miniaturas()).toHaveLength(2);
      expect(texto()).toContain('Foto agregada. El perfil tiene 2 de 6 fotos.');
      http.expectNone('/api/catalogo/prestador/fotos');
    });

    it('quitar una foto la borra en el servidor y sale de la lista', async () => {
      await crear(perfil({ fotos: [foto('a'), foto('b')] }));

      raiz().querySelector<HTMLButtonElement>('[data-foto="a"][data-accion="quitar"]')!.click();
      harness.detectChanges();

      const peticion = http.expectOne(`${URL}/fotos/a`);
      expect(peticion.request.method).toBe('DELETE');
      peticion.flush(null, { status: 204, statusText: 'No Content' });
      harness.detectChanges();

      expect(miniaturas()).toHaveLength(1);
    });

    it('si el servidor no la quita, se queda y se explica', async () => {
      await crear(perfil({ fotos: [foto('a')] }));

      raiz().querySelector<HTMLButtonElement>('[data-accion="quitar"]')!.click();
      harness.detectChanges();
      const { body, init } = problema(409, 'CONFLICTO_CONCURRENCIA');
      http.expectOne(`${URL}/fotos/a`).flush(body, init);
      harness.detectChanges();

      expect(miniaturas()).toHaveLength(1);
      expect(raiz().querySelector('app-selector-fotos .alerta--aviso')).not.toBeNull();
    });
  });

  it('si no puede leer el perfil lo avisa', async () => {
    await harness.navigateByUrl('/prestador/perfil', PerfilPrestador);
    harness.detectChanges();
    http.expectOne(URL).error(new ProgressEvent('error'));
    harness.detectChanges();

    expect(raiz().querySelector('.alerta--error')).not.toBeNull();
    expect(campo('descripcion')).toBeNull();
  });

  it('en inglés cambian los textos y los de las fotos', async () => {
    establecerIdioma('en', { recordar: false });
    await crear(perfil({ fotos: [foto('a')] }));

    expect(texto()).toContain('Profile details');
    expect(raiz().querySelector('label[for="perfil-descripcion"]')?.textContent).toContain('Description');
    expect(raiz().querySelector('app-selector-fotos legend')?.textContent).toContain('Profile photos');
    expect(botonGuardar().textContent?.trim()).toBe('Save profile');
  });
});
