import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ContenidoVersion, OfertaPropia, VersionPropia } from '../../../core/api/modelos';
import { erroresInterceptor } from '../../../core/errores/errores.interceptor';
import { establecerIdioma } from '../../../core/idioma/idioma';
import { CLAVE_SESION, aSesionGuardada } from '../../../core/sesion/almacen-sesion';
import { respuestaDeSesion } from '../../../core/sesion/testing';
import { DetalleOferta } from './detalle-oferta';

const ID = 'b00000000000000000000001';
const OFERTA = `/api/catalogo/prestador/ofertas/${ID}`;
const FOTO = {
  id: 'f1',
  url: '/api/catalogo/fotos/f1/galeria',
  urlMiniatura: '/api/catalogo/fotos/f1/miniatura',
};

function contenido(nombre: string, precio: number, extra: Partial<ContenidoVersion> = {}): ContenidoVersion {
  return {
    nombre,
    descripcion: `Descripción de ${nombre}`,
    precioReferencia: precio,
    unidadPrecio: 'por persona',
    zonaId: 'z1',
    capacidadMaxima: 12,
    fotos: [FOTO],
    ...extra,
  };
}

function version(
  numero: number,
  estadoModeracion: VersionPropia['estadoModeracion'],
  nombre: string,
  precio: number,
  extra: Partial<VersionPropia> = {},
): VersionPropia {
  return {
    numero,
    estadoModeracion,
    enviadaEn: '2026-11-01T10:00:00Z',
    contenido: contenido(nombre, precio),
    ...extra,
  };
}

function problema(status: number, codigo: string) {
  return {
    body: { status, codigo, title: codigo, detail: codigo },
    init: { status, statusText: codigo, headers: { 'Content-Type': 'application/problem+json' } },
  };
}

describe('DetalleOferta (DC-50)', () => {
  let fixture: ComponentFixture<DetalleOferta>;
  let http: HttpTestingController;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const texto = () => (raiz().textContent ?? '').replace(/[  ]/g, ' ').replace(/\s+/g, ' ');
  const acciones = () =>
    Array.from(raiz().querySelectorAll<HTMLAnchorElement>('.acciones a')).map((a) => [a.textContent?.trim(), a.getAttribute('href')]);

  function abrir(respuesta: OfertaPropia | { status: number; codigo: string }): void {
    localStorage.setItem(
      CLAVE_SESION,
      JSON.stringify(aSesionGuardada(respuestaDeSesion({ rol: 'PRESTADOR', tipoPrestador: 'AGENCIA_OPERADORA' }))),
    );
    fixture = TestBed.createComponent(DetalleOferta);
    fixture.componentRef.setInput('id', ID);
    fixture.detectChanges();
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
        provideRouter([]),
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

  it('pide la oferta por su id, una sola petición, sin recorrer la lista', () => {
    abrir({ id: ID, tipo: 'ACTIVIDAD', retirada: false, publicada: version(1, 'APROBADA', 'Caminata', 70000) });

    http.expectNone((p) => p.url === '/api/catalogo/prestador/ofertas');
    expect(raiz().querySelector('h1')?.textContent).toBe('Caminata');
  });

  it('el precio lleva la unidad que entrega la API, no la que correspondería al tipo de oferta', () => {
    abrir({
      id: ID,
      tipo: 'ACTIVIDAD',
      retirada: false,
      publicada: { ...version(1, 'APROBADA', 'Nombre publicado', 70000), contenido: contenido('Nombre publicado', 70000, { unidadPrecio: 'por noche y unidad' }) },
    });

    expect(raiz().querySelector('.versiones .renglon__precio')?.textContent).toContain('por noche y unidad');
  });

  it('muestra el contenido de cada versión: nombre, precio, capacidad, descripción y fotos', () => {
    abrir({
      id: ID,
      tipo: 'ACTIVIDAD',
      retirada: false,
      publicada: version(1, 'APROBADA', 'Nombre publicado', 70000),
      pendiente: version(2, 'EN_MODERACION', 'Nombre pendiente', 80000),
    });

    const versiones = Array.from(raiz().querySelectorAll('.versiones section'));
    expect(versiones).toHaveLength(2);
    expect(versiones[0].textContent).toContain('Nombre publicado');
    expect(versiones[0].textContent).toContain('70.000');
    expect(versiones[1].textContent).toContain('Nombre pendiente');
    expect(versiones[1].textContent).toContain('80.000');
    expect(versiones[1].textContent).toContain('Descripción de Nombre pendiente');
    expect(versiones[1].textContent).toContain('12');
    expect(versiones[1].textContent).toContain('1 foto');
    expect(versiones[1].querySelectorAll('.fotos img')).toHaveLength(1);
  });

  it('una versión sin fotos (anterior a DC-47) lo dice', () => {
    abrir({
      id: ID,
      tipo: 'ACTIVIDAD',
      retirada: false,
      publicada: { ...version(1, 'APROBADA', 'Vieja', 70000), contenido: contenido('Vieja', 70000, { fotos: [] }) },
    });

    expect(texto()).toContain('Sin fotos');
  });

  describe('para corregir', () => {
    it('una versión rechazada muestra su motivo y ofrece "Corregir la versión rechazada"', () => {
      abrir({
        id: ID,
        tipo: 'ACTIVIDAD',
        retirada: false,
        publicada: version(1, 'APROBADA', 'Publicada', 70000),
        pendiente: version(2, 'RECHAZADA', 'Rechazada', 80000, { motivoRechazo: 'Faltan fotos' }),
      });

      expect(texto()).toContain('Motivo del rechazo');
      expect(texto()).toContain('Faltan fotos');
      expect(acciones()[0]).toEqual(['Corregir la versión rechazada', `/prestador/ofertas/${ID}/editar`]);
    });

    it('una oferta que solo tiene su primera versión en moderación también se puede editar', () => {
      abrir({ id: ID, tipo: 'ACTIVIDAD', retirada: false, pendiente: version(1, 'EN_MODERACION', 'Primera', 70000) });

      expect(acciones()).toEqual([['Editar oferta', `/prestador/ofertas/${ID}/editar`]]);
      expect(texto()).not.toContain('Podrás editarla cuando tenga una versión publicada');
    });

    it('una oferta publicada se edita y se puede ver en el catálogo', () => {
      abrir({ id: ID, tipo: 'ACTIVIDAD', retirada: false, publicada: version(1, 'APROBADA', 'Publicada', 70000) });

      expect(acciones()).toEqual([
        ['Editar oferta', `/prestador/ofertas/${ID}/editar`],
        ['Ver en el catálogo', `/ofertas/${ID}`],
      ]);
    });

    it('una oferta retirada no se edita ni se ve en el catálogo', () => {
      abrir({ id: ID, tipo: 'ACTIVIDAD', retirada: true, publicada: version(1, 'APROBADA', 'Retirada', 70000) });

      expect(acciones()).toEqual([]);
      expect(texto()).toContain('Retirada');
    });
  });

  describe('si no se puede leer', () => {
    it.each([
      [403, 'ACCESO_DENEGADO'],
      [404, 'OFERTA_NO_ENCONTRADA'],
    ])('con %s (no es suya o no existe) dice que no la encuentra', (estado, codigo) => {
      abrir({ status: estado, codigo });

      expect(texto()).toContain('No encontramos esta oferta entre las tuyas');
      expect(raiz().querySelector('.alerta--error')).toBeNull();
    });

    it('otro fallo se muestra como error', () => {
      abrir({ status: 500, codigo: 'ERROR_INTERNO' });

      expect(raiz().querySelector('.alerta--error')).not.toBeNull();
      expect(texto()).not.toContain('No encontramos esta oferta entre las tuyas');
    });
  });

  it('en inglés cambian el contenido de las versiones y la acción de corregir', () => {
    establecerIdioma('en', { recordar: false });
    abrir({
      id: ID,
      tipo: 'ACTIVIDAD',
      retirada: false,
      pendiente: version(1, 'RECHAZADA', 'Rechazada', 80000, { motivoRechazo: 'Faltan fotos' }),
    });

    expect(texto()).toContain('Maximum capacity');
    expect(texto()).toContain('1 photo');
    expect(acciones()[0][0]).toBe('Fix the rejected version');
  });
});
