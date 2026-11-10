import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ContenidoVersion, OfertaPropia, VersionPropia } from '../../../core/api/modelos';
import { erroresInterceptor } from '../../../core/errores/errores.interceptor';
import { establecerIdioma } from '../../../core/idioma/idioma';
import { CLAVE_SESION, aSesionGuardada } from '../../../core/sesion/almacen-sesion';
import { respuestaDeSesion } from '../../../core/sesion/testing';
import { FormularioOferta } from './formulario-oferta';

const ID = 'b00000000000000000000001';
const OFERTA = `/api/catalogo/prestador/ofertas/${ID}`;
const FOTO = {
  id: '3f2504e0-4f89-11d3-9a0c-0305e82c3301',
  url: '/api/catalogo/fotos/3f2504e0-4f89-11d3-9a0c-0305e82c3301/galeria',
  urlMiniatura: '/api/catalogo/fotos/3f2504e0-4f89-11d3-9a0c-0305e82c3301/miniatura',
};

function contenido(nombre: string, extra: Partial<ContenidoVersion> = {}): ContenidoVersion {
  return {
    nombre,
    descripcion: `Descripción de ${nombre}`,
    precioReferencia: 80000,
    unidadPrecio: 'por persona',
    zonaId: 'z1',
    capacidadMaxima: 10,
    fotos: [FOTO],
    tipoActividad: 'SENDERISMO',
    nivelEsfuerzo: 'MODERADO',
    horariosSalida: ['07:30:00'],
    ...extra,
  };
}

function version(
  numero: number,
  estadoModeracion: VersionPropia['estadoModeracion'],
  nombre: string,
  extra: Partial<VersionPropia> = {},
): VersionPropia {
  return { numero, estadoModeracion, enviadaEn: '2026-11-01T10:00:00Z', contenido: contenido(nombre), ...extra };
}

function propia(extra: Partial<OfertaPropia> = {}): OfertaPropia {
  return { id: ID, tipo: 'ACTIVIDAD', retirada: false, ...extra };
}

function problema(status: number, codigo: string) {
  return {
    body: { status, codigo, title: codigo, detail: codigo },
    init: { status, statusText: codigo, headers: { 'Content-Type': 'application/problem+json' } },
  };
}

/**
 * Editar una oferta con el contenido completo de sus versiones (DC-50): el formulario parte de la versión pendiente si existe
 * —en moderación o rechazada— y si no, de la publicada; ya no depende de la ficha pública.
 */
describe('FormularioOferta al editar', () => {
  let fixture: ComponentFixture<FormularioOferta>;
  let http: HttpTestingController;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const texto = () => (raiz().textContent ?? '').replace(/\s+/g, ' ');
  const valor = (id: string) => raiz().querySelector<HTMLInputElement>(`#${id}`)?.value;
  const aviso = () => raiz().querySelector<HTMLElement>('.formulario__aviso');

  /** Abre el formulario de edición y responde a lo que pide: las zonas, las ofertas y la oferta con sus versiones. */
  function abrir(respuesta: OfertaPropia | { status: number; codigo: string }): void {
    const sesion = respuestaDeSesion({ rol: 'PRESTADOR', tipoPrestador: 'AGENCIA_OPERADORA' });
    localStorage.setItem(CLAVE_SESION, JSON.stringify(aSesionGuardada(sesion)));
    fixture = TestBed.createComponent(FormularioOferta);
    fixture.componentRef.setInput('id', ID);
    fixture.detectChanges();
    http.expectOne('/api/catalogo/zonas').flush([{ id: 'z1', nombre: 'Minca', activa: true }]);
    http
      .expectOne((p) => p.url === '/api/catalogo/prestador/ofertas')
      .flush({ contenido: [], pagina: 0, tamano: 100, totalElementos: 0, totalPaginas: 1 });
    const peticion = http.expectOne(OFERTA);
    if ('status' in respuesta) {
      const { body, init } = problema(respuesta.status, respuesta.codigo);
      peticion.flush(body, init);
    } else {
      peticion.flush(respuesta);
    }
    fixture.detectChanges();
  }

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    establecerIdioma('es', { recordar: false });
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'prestador/ofertas', children: [] }]),
        provideHttpClient(withInterceptors([erroresInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    establecerIdioma('es', { recordar: false });
    http.verify();
  });

  it('pide la oferta propia por su id y no la ficha pública del catálogo', () => {
    abrir(propia({ publicada: version(1, 'APROBADA', 'Caminata') }));

    http.expectNone('/api/catalogo/ofertas/' + ID);
    expect(raiz().querySelector('form')).not.toBeNull();
  });

  it('parte de la versión publicada si no hay cambios pendientes, sin ningún aviso', () => {
    abrir(propia({ publicada: version(1, 'APROBADA', 'Caminata a Marinka') }));

    expect(valor('nombre')).toBe('Caminata a Marinka');
    expect(valor('descripcion')).toBe('Descripción de Caminata a Marinka');
    expect(valor('precioReferencia')).toBe('80000');
    expect(valor('capacidadMaxima')).toBe('10');
    expect(valor('horariosSalida')).toBe('07:30');
    expect(aviso()).toBeNull();
    expect(texto()).toContain('La versión publicada sigue visible');
    expect(raiz().querySelectorAll('.miniatura')).toHaveLength(1);
  });

  it('con cambios en revisión parte de la versión pendiente, no de la publicada, y lo avisa', () => {
    abrir(
      propia({
        publicada: version(1, 'APROBADA', 'Nombre publicado'),
        pendiente: version(2, 'EN_MODERACION', 'Nombre en revisión'),
      }),
    );

    expect(valor('nombre')).toBe('Nombre en revisión');
    expect(aviso()?.textContent).toContain('ya tiene cambios en revisión (versión 2)');
    expect(aviso()?.classList).toContain('alerta--aviso');
  });

  it('con una versión rechazada parte de ella y muestra el motivo para corregirla', () => {
    abrir(
      propia({
        publicada: version(1, 'APROBADA', 'Nombre publicado'),
        pendiente: version(2, 'RECHAZADA', 'Nombre rechazado', { motivoRechazo: 'Faltan fotos del sendero' }),
      }),
    );

    expect(valor('nombre')).toBe('Nombre rechazado');
    expect(aviso()?.textContent).toContain('Rechazaron la versión 2');
    expect(aviso()?.textContent).toContain('Motivo: Faltan fotos del sendero');
    expect(aviso()?.classList).toContain('alerta--error');
  });

  it('si el rechazo no trae motivo lo dice', () => {
    abrir(propia({ pendiente: version(1, 'RECHAZADA', 'Sin motivo') }));

    expect(aviso()?.textContent).toContain('Motivo: no se indicó');
  });

  it('una oferta que nunca se aprobó también se puede editar: parte de su primera versión', () => {
    abrir(propia({ pendiente: version(1, 'EN_MODERACION', 'Primera versión') }));

    expect(raiz().querySelector('form')).not.toBeNull();
    expect(valor('nombre')).toBe('Primera versión');
    expect(aviso()?.textContent).toContain('La primera versión de esta oferta (versión 1) todavía está en moderación');
    // No hay versión publicada que siga visible: no se dice.
    expect(texto()).not.toContain('La versión publicada sigue visible');
  });

  it('una oferta retirada no se edita', () => {
    abrir(propia({ retirada: true, publicada: version(1, 'APROBADA', 'Retirada') }));

    expect(raiz().querySelector('form')).toBeNull();
    expect(raiz().querySelector('.alerta--aviso')).not.toBeNull();
  });

  it.each([
    [403, 'ACCESO_DENEGADO'],
    [404, 'OFERTA_NO_ENCONTRADA'],
  ])('si responde %s (la oferta no es suya o no existe) lo explica y no muestra el formulario', (estado, codigo) => {
    abrir({ status: estado, codigo });

    expect(texto()).toContain('No encontramos esta oferta entre las tuyas.');
    expect(raiz().querySelector('form')).toBeNull();
  });

  it('guardar manda el contenido corregido y las fotos de la versión con PUT sobre la oferta', () => {
    abrir(
      propia({
        pendiente: version(2, 'RECHAZADA', 'Nombre rechazado', { motivoRechazo: 'Faltan fotos' }),
      }),
    );
    const nombre = raiz().querySelector<HTMLInputElement>('#nombre')!;
    nombre.value = 'Nombre corregido';
    nombre.dispatchEvent(new Event('input'));

    raiz().querySelector<HTMLButtonElement>('form button[type="submit"]')!.click();
    fixture.detectChanges();

    const peticion = http.expectOne(OFERTA);
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toMatchObject({
      tipo: 'ACTIVIDAD',
      nombre: 'Nombre corregido',
      zonaId: 'z1',
      fotos: [FOTO.id],
    });
    peticion.flush({ id: ID, tipo: 'ACTIVIDAD', retirada: false });
  });

  it('en inglés, el aviso de rechazo cambia de idioma con la pantalla abierta', () => {
    abrir(propia({ pendiente: version(2, 'RECHAZADA', 'Nombre', { motivoRechazo: 'Faltan fotos' }) }));
    expect(aviso()?.textContent).toContain('Rechazaron la versión 2');

    establecerIdioma('en', { recordar: false });
    fixture.detectChanges();

    expect(aviso()?.textContent).toContain('Version 2 of this offer was rejected');
    expect(aviso()?.textContent).toContain('Reason: Faltan fotos');
  });
});
