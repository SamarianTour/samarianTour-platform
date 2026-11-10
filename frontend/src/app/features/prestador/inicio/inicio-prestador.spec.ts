import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { OfertaPrestador } from '../../../core/api/modelos';
import { PrestadorPropio } from '../../../core/api/modelos-nucleo';
import { erroresInterceptor } from '../../../core/errores/errores.interceptor';
import { establecerIdioma } from '../../../core/idioma/idioma';
import { CLAVE_SESION, aSesionGuardada } from '../../../core/sesion/almacen-sesion';
import { respuestaDeSesion } from '../../../core/sesion/testing';
import { InicioPrestador } from './inicio-prestador';

const REGISTRO = '/api/nucleo/prestador/registro';
const OFERTAS = '/api/catalogo/prestador/ofertas';

function prestador(extra: Partial<PrestadorPropio> = {}): PrestadorPropio {
  return {
    id: 'p1',
    numeroRnt: '910006',
    tipo: 'AGENCIA_OPERADORA',
    categoria: 'MICRO_PEQUENO',
    razonSocial: 'MAR Y SELVA S.A.S.',
    estadoRnt: 'ACTIVO',
    ultimaSincronizacion: '2026-11-09T15:00:00Z',
    habilitado: true,
    whatsapp: '+573001110202',
    whatsappVerificado: true,
    ...extra,
  };
}

const VERSION = {
  numero: 1,
  estadoModeracion: 'APROBADA',
  enviadaEn: '2026-11-01T10:00:00Z',
  nombre: 'Caminata',
  precioReferencia: 70000,
  unidadPrecio: 'por persona',
} as const;

function oferta(id: string, extra: Partial<OfertaPrestador> = {}): OfertaPrestador {
  return { id, tipo: 'ACTIVIDAD', retirada: false, publicada: VERSION, ...extra };
}

function pagina(contenido: OfertaPrestador[]) {
  return { contenido, pagina: 0, tamano: 100, totalElementos: contenido.length, totalPaginas: 1 };
}

describe('InicioPrestador (P03.0)', () => {
  let harness: RouterTestingHarness;
  let http: HttpTestingController;

  const raiz = () => harness.routeNativeElement as HTMLElement;
  const texto = () => (raiz().textContent ?? '').replace(/[  ]/g, ' ').replace(/\s+/g, ' ');
  const aviso = () => raiz().querySelector<HTMLElement>('.aviso-rnt');
  /** El valor que la tarjeta del registro muestra junto a un rótulo, por ejemplo "Estado del RNT". */
  const dato = (rotulo: string) => {
    const dt = Array.from(raiz().querySelectorAll('dt')).find((e) => e.textContent?.trim() === rotulo);
    return dt?.nextElementSibling?.textContent?.replace(/\s+/g, ' ').trim();
  };

  /** Abre el inicio y responde al registro (`null` = 404, todavía sin RNT) y a las ofertas. */
  async function crear(registro: PrestadorPropio | null, ofertas: OfertaPrestador[] = []): Promise<void> {
    await harness.navigateByUrl('/prestador', InicioPrestador);
    harness.detectChanges();
    if (registro) {
      http.expectOne(REGISTRO).flush(registro);
    } else {
      http.expectOne(REGISTRO).flush(
        { status: 404, codigo: 'PRESTADOR_NO_REGISTRADO', title: 'x', detail: 'x' },
        { status: 404, statusText: 'Not Found', headers: { 'Content-Type': 'application/problem+json' } },
      );
    }
    http.expectOne((p) => p.url === OFERTAS).flush(pagina(ofertas));
    harness.detectChanges();
  }

  beforeEach(async () => {
    localStorage.clear();
    sessionStorage.clear();
    establecerIdioma('es', { recordar: false });
    const sesion = respuestaDeSesion({ rol: 'PRESTADOR', tipoPrestador: 'AGENCIA_OPERADORA' });
    localStorage.setItem(
      CLAVE_SESION,
      JSON.stringify(aSesionGuardada({ ...sesion, cuenta: { ...sesion.cuenta, nombre: 'Mar y Selva' } })),
    );
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'prestador', component: InicioPrestador }]),
        provideHttpClient(withInterceptors([erroresInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
  });

  afterEach(() => {
    establecerIdioma('es', { recordar: false });
    http.verify();
  });

  it('saluda por el nombre de la cuenta', async () => {
    await crear(prestador());

    expect(raiz().querySelector('h1')?.textContent).toBe('Hola, Mar y Selva');
  });

  describe('con el RNT activo', () => {
    it('muestra su registro y no pone ningún aviso', async () => {
      await crear(prestador());

      expect(aviso()).toBeNull();
      expect(dato('Estado del RNT')).toBe('Activo');
      expect(dato('Número de RNT')).toBe('910006');
      expect(dato('Razón social')).toBe('MAR Y SELVA S.A.S.');
      expect(dato('Tipo de prestador')).toBe('Agencia operadora');
      expect(dato('Última sincronización')).toMatch(/2026/);
      expect(dato('WhatsApp de contacto')).toBe('Verificado');
    });

    it('si el RNT nunca respondió lo dice', async () => {
      await crear(prestador({ ultimaSincronizacion: undefined }));

      expect(dato('Última sincronización')).toBe('Aún no ha respondido');
    });
  });

  describe('si el RNT no sirve para publicar', () => {
    it.each([
      ['INACTIVO', 'Tu RNT está inactivo'],
      ['NO_ENCONTRADO', 'No encontramos tu RNT'],
    ] as const)('con el RNT %s explica que las ofertas no se ven y lleva a la sección RNT', async (estado, titulo) => {
      await crear(prestador({ estadoRnt: estado, habilitado: false }));

      expect(aviso()?.textContent).toContain(titulo);
      expect(aviso()?.textContent).toContain('Tus ofertas no se muestran en el catálogo');
      expect(aviso()?.classList).toContain('alerta--aviso');
      expect(aviso()?.querySelector('a')?.getAttribute('href')).toBe('/prestador/rnt');
    });

    it('sin haber registrado el RNT (como agencia-sin-rnt) pide registrarlo', async () => {
      await crear(null);

      expect(aviso()?.textContent).toContain('Registra tu RNT para publicar');
      expect(aviso()?.textContent).toContain('ninguna se muestra en el catálogo');
      expect(aviso()?.querySelector('a')?.getAttribute('href')).toBe('/prestador/rnt');
      expect(dato('Estado del RNT')).toBe('Sin registrar');
      expect(dato('WhatsApp de contacto')).toBe('Registra primero tu RNT');
      expect(dato('Número de RNT')).toBeUndefined();
    });

    it('con la verificación pendiente avisa con calma: no hay nada que corregir', async () => {
      await crear(prestador({ estadoRnt: 'VERIFICACION_PENDIENTE', habilitado: false }));

      expect(aviso()?.textContent).toContain('Estamos verificando tu RNT');
      expect(aviso()?.classList).toContain('alerta--info');
      expect(aviso()?.textContent).toContain('no tienes que hacer nada');
    });
  });

  describe('el WhatsApp', () => {
    it.each([
      [{ whatsapp: undefined, whatsappVerificado: undefined }, 'Sin configurar'],
      [{ whatsapp: '+573001110202', whatsappVerificado: false }, 'Sin verificar'],
      [{ whatsapp: '+573001110202', whatsappVerificado: true }, 'Verificado'],
    ] as const)('se resume en el registro (%j)', async (datos, esperado) => {
      await crear(prestador(datos));

      expect(dato('WhatsApp de contacto')).toBe(esperado);
    });

    it('nunca muestra el número', async () => {
      await crear(prestador());

      expect(texto()).not.toContain('3001110202');
    });
  });

  describe('las ofertas', () => {
    it('cuenta las ofertas de cada estado', async () => {
      await crear(prestador(), [
        oferta('1'),
        oferta('2'),
        oferta('3', { pendiente: { ...VERSION, estadoModeracion: 'EN_MODERACION' } }),
        oferta('4', { publicada: undefined, pendiente: { ...VERSION, estadoModeracion: 'RECHAZADA', motivoRechazo: 'x' } }),
      ]);

      const cifras = Array.from(raiz().querySelectorAll('.cifra')).map((c) => [
        c.querySelector('.cifra__valor')?.textContent,
        c.querySelector('.etiqueta')?.textContent?.trim(),
      ]);
      expect(cifras).toEqual([
        ['2', 'Publicada'],
        ['1', 'Publicada con cambios en revisión'],
        ['0', 'En moderación'],
        ['1', 'Rechazada'],
        ['0', 'Retirada'],
      ]);
      expect(texto()).toContain('4 ofertas en total');
    });

    it('sin ofertas invita a publicar la primera', async () => {
      await crear(prestador(), []);

      expect(texto()).toContain('Todavía no tienes ofertas');
      expect(raiz().querySelector('.cifras')).toBeNull();
    });

    it('lleva a "Mis ofertas" y a publicar una nueva', async () => {
      await crear(prestador(), [oferta('1')]);

      const enlaces = Array.from(raiz().querySelectorAll<HTMLAnchorElement>('a')).map((a) => a.getAttribute('href'));
      expect(enlaces).toContain('/prestador/ofertas');
      expect(enlaces).toContain('/prestador/ofertas/nueva');
    });
  });

  describe('si una consulta falla', () => {
    it('el registro con error no esconde las ofertas', async () => {
      await harness.navigateByUrl('/prestador', InicioPrestador);
      harness.detectChanges();
      http.expectOne(REGISTRO).error(new ProgressEvent('error'));
      http.expectOne((p) => p.url === OFERTAS).flush(pagina([oferta('1')]));
      harness.detectChanges();

      expect(raiz().querySelector('#inicio-registro')?.closest('section')?.querySelector('.alerta--error')).not.toBeNull();
      expect(raiz().querySelector('.cifras')).not.toBeNull();
      // Con el registro sin leer no se sabe nada del RNT: no se afirma nada falso.
      expect(aviso()).toBeNull();
    });

    it('las ofertas con error no esconden el registro', async () => {
      await harness.navigateByUrl('/prestador', InicioPrestador);
      harness.detectChanges();
      http.expectOne(REGISTRO).flush(prestador());
      http.expectOne((p) => p.url === OFERTAS).error(new ProgressEvent('error'));
      harness.detectChanges();

      expect(dato('Número de RNT')).toBe('910006');
      expect(raiz().querySelector('#inicio-ofertas')?.closest('section')?.querySelector('.alerta--error')).not.toBeNull();
    });
  });

  describe('los accesos', () => {
    it('llevan a cada sección del panel', async () => {
      await crear(prestador());

      const accesos = Array.from(raiz().querySelectorAll<HTMLAnchorElement>('.acceso')).map((a) => [
        a.querySelector('strong')?.textContent,
        a.getAttribute('href'),
      ]);
      expect(accesos).toEqual([
        ['Mi RNT', '/prestador/rnt'],
        ['Perfil público', '/prestador/perfil'],
        ['WhatsApp de contacto', '/prestador/whatsapp'],
        ['Cuenta de cobro', '/prestador/cuenta-cobro'],
      ]);
    });

    it('la cuenta de cobro aclara que la comisión no se suma al turista', async () => {
      await crear(prestador());

      expect(texto()).toContain('nunca se suma al turista');
    });
  });

  it('en inglés cambian el saludo, el aviso y las etiquetas', async () => {
    establecerIdioma('en', { recordar: false });
    await crear(prestador({ estadoRnt: 'NO_ENCONTRADO', habilitado: false }));

    expect(raiz().querySelector('h1')?.textContent).toBe('Hello, Mar y Selva');
    expect(aviso()?.textContent).toContain("We couldn't find your RNT");
    expect(aviso()?.textContent).toContain('Your offers are not shown in the catalog');
    expect(dato('RNT status')).toBe('Not found');
    expect(dato('Provider type')).toBe('Tour operator agency');
  });
});
