import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { Zona } from '../../../core/api/modelos';
import { AvisosService } from '../../../core/avisos/avisos.service';
import { erroresInterceptor } from '../../../core/errores/errores.interceptor';
import { establecerIdioma } from '../../../core/idioma/idioma';
import { CLAVE_SESION, aSesionGuardada } from '../../../core/sesion/almacen-sesion';
import { respuestaDeSesion } from '../../../core/sesion/testing';
import { ZonasAdmin } from './zonas-admin';

const ZONAS = '/api/catalogo/admin/zonas';

function zona(id: string, nombre: string, extra: Partial<Zona> = {}): Zona {
  return {
    id,
    nombre,
    descripcion: `Descripción de ${nombre}`,
    ubicacion: { latitud: 11.27, longitud: -74.19 },
    areaProtegida: false,
    altaAfluencia: false,
    activa: true,
    buenasPracticas: ['No dejar basura'],
    condicionesVisita: 'Llevar agua',
    ...extra,
  };
}

const TAGANGA = zona('z1', 'Taganga', { altaAfluencia: true });
const TAYRONA = zona('z2', 'Tayrona', { areaProtegida: true });
const VIEJA = zona('z3', 'Zona vieja', { activa: false });
const TODAS = [TAYRONA, VIEJA, TAGANGA];

describe('ZonasAdmin (P05.5)', () => {
  let harness: RouterTestingHarness;
  let http: HttpTestingController;
  let mostrar: ReturnType<typeof vi.spyOn>;

  const raiz = () => harness.routeNativeElement as HTMLElement;
  const texto = () => (raiz().textContent ?? '').replace(/[  ]/g, ' ');
  const tarjetas = () => Array.from(raiz().querySelectorAll<HTMLElement>('li.zona'));
  const campo = (id: string) => raiz().querySelector<HTMLInputElement>(`#zona-${id}`)!;
  const botonDe = (tarjeta: number, inicio: string) =>
    Array.from(tarjetas()[tarjeta].querySelectorAll<HTMLButtonElement>('button')).find((b) =>
      (b.textContent ?? '').trim().startsWith(inicio),
    )!;

  async function crear(zonas: Zona[] = TODAS, url = '/admin/zonas'): Promise<void> {
    await harness.navigateByUrl(url, ZonasAdmin);
    harness.detectChanges();
    http.expectOne(ZONAS).flush(zonas);
    harness.detectChanges();
  }

  function escribir(id: string, valor: string): void {
    const entrada = campo(id);
    entrada.value = valor;
    entrada.dispatchEvent(new Event('input'));
    harness.detectChanges();
  }

  function llenarFormulario(): void {
    escribir('nombre', '  Minca ');
    escribir('descripcion', 'Pueblo en la sierra');
    escribir('latitud', '11.14');
    escribir('longitud', '-74.12');
    escribir('buenasPracticas', 'No hacer ruido\n\n Llevar la basura ');
  }

  function enviar(): void {
    raiz().querySelector<HTMLButtonElement>('form button[type="submit"]')!.click();
    harness.detectChanges();
  }

  beforeAll(() => {
    HTMLDialogElement.prototype.showModal ??= function (this: HTMLDialogElement) {
      this.setAttribute('open', '');
    };
    HTMLDialogElement.prototype.close ??= function (this: HTMLDialogElement) {
      this.removeAttribute('open');
      this.dispatchEvent(new Event('close'));
    };
  });

  beforeEach(async () => {
    localStorage.clear();
    sessionStorage.clear();
    establecerIdioma('es', { recordar: false });
    localStorage.setItem(CLAVE_SESION, JSON.stringify(aSesionGuardada(respuestaDeSesion({ rol: 'ADMINISTRADOR' }))));
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'admin/zonas', component: ZonasAdmin }]),
        provideHttpClient(withInterceptors([erroresInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    mostrar = vi.spyOn(TestBed.inject(AvisosService), 'mostrar');
    harness = await RouterTestingHarness.create();
  });

  afterEach(() => {
    // El idioma es global: se deja en español aunque la verificación falle, para no contaminar otras pruebas.
    establecerIdioma('es', { recordar: false });
    http.verify();
  });

  describe('la lista', () => {
    it('muestra las zonas por nombre, con su estado, sus marcas y sus condiciones', async () => {
      await crear();

      expect(tarjetas()).toHaveLength(3);
      expect(tarjetas()[0].querySelector('h3')?.textContent).toBe('Taganga');
      expect(tarjetas()[0].textContent).toContain('Alta afluencia');
      expect(tarjetas()[1].textContent).toContain('Área protegida');
      expect(tarjetas()[2].textContent).toContain('Inactiva');
      expect(tarjetas()[0].textContent).toContain('Ubicación:');
      expect(tarjetas()[0].textContent).toContain('Condiciones de visita: Llevar agua');
      expect(texto()).toContain('3 zonas');
    });

    it('el filtro de la dirección deja solo las inactivas', async () => {
      await crear(TODAS, '/admin/zonas?estado=inactivas');

      expect(tarjetas()).toHaveLength(1);
      expect(tarjetas()[0].querySelector('h3')?.textContent).toBe('Zona vieja');
      expect(raiz().querySelector('.filtros a[aria-current="page"]')?.textContent?.trim()).toBe('Inactivas');
    });

    it('si la consulta falla avisa del error', async () => {
      await harness.navigateByUrl('/admin/zonas', ZonasAdmin);
      harness.detectChanges();
      http.expectOne(ZONAS).error(new ProgressEvent('error'));
      harness.detectChanges();

      expect(raiz().querySelector('.alerta--error')).not.toBeNull();
    });
  });

  describe('registrar', () => {
    it('no envía nada si faltan datos y marca los campos', async () => {
      await crear();

      enviar();

      http.expectNone((p) => p.method === 'POST');
      expect(texto()).toContain('Escribe el nombre de la zona.');
      expect(texto()).toContain('Escribe la latitud.');
      expect(campo('nombre').getAttribute('aria-invalid')).toBe('true');
    });

    it('envía la zona sin espacios sobrantes, agrega la tarjeta y avisa', async () => {
      await crear();
      llenarFormulario();

      enviar();

      const peticion = http.expectOne(ZONAS);
      expect(peticion.request.method).toBe('POST');
      expect(peticion.request.body).toEqual({
        nombre: 'Minca',
        descripcion: 'Pueblo en la sierra',
        ubicacion: { latitud: 11.14, longitud: -74.12 },
        areaProtegida: false,
        altaAfluencia: false,
        buenasPracticas: ['No hacer ruido', 'Llevar la basura'],
      });
      peticion.flush(zona('z4', 'Minca'));
      harness.detectChanges();

      expect(tarjetas()).toHaveLength(4);
      expect(tarjetas().map((t) => t.querySelector('h3')?.textContent)).toContain('Minca');
      expect(campo('nombre').value).toBe('');
      expect(mostrar).toHaveBeenCalledWith('Registraste la zona Minca.');
    });

    it('un nombre repetido se marca en el campo del nombre', async () => {
      await crear();
      llenarFormulario();
      enviar();

      http
        .expectOne(ZONAS)
        .flush({ codigo: 'ZONA_DUPLICADA', titulo: 'Zona duplicada', status: 409 }, { status: 409, statusText: 'Conflict', headers: { 'Content-Type': 'application/problem+json' } });
      harness.detectChanges();

      expect(texto()).toContain('Ya hay una zona con ese nombre. Usa otro.');
      expect(campo('nombre').getAttribute('aria-invalid')).toBe('true');
      expect(mostrar).not.toHaveBeenCalled();
    });

    it('mientras guarda no permite enviar de nuevo', async () => {
      await crear();
      llenarFormulario();

      enviar();
      enviar();

      expect(http.match(ZONAS)).toHaveLength(1);
    });
  });

  describe('editar', () => {
    it('carga los datos de la zona y cancelar vuelve al formulario vacío', async () => {
      await crear();

      botonDe(0, 'Editar').click();
      harness.detectChanges();

      expect(campo('nombre').value).toBe('Taganga');
      expect(campo('altaAfluencia').checked).toBe(true);
      expect(campo('buenasPracticas').value).toBe('No dejar basura');
      expect(texto()).toContain('Editar la zona Taganga');

      Array.from(raiz().querySelectorAll<HTMLButtonElement>('form button'))
        .find((b) => (b.textContent ?? '').includes('Cancelar la edición'))!
        .click();
      harness.detectChanges();

      expect(campo('nombre').value).toBe('');
      expect(texto()).toContain('Registrar una zona');
    });

    it('guarda con PUT mandando todos los datos y conservando su estado', async () => {
      await crear();
      botonDe(1, 'Editar').click();
      harness.detectChanges();
      escribir('nombre', 'Parque Tayrona');

      enviar();

      const peticion = http.expectOne(`${ZONAS}/z2`);
      expect(peticion.request.method).toBe('PUT');
      expect(peticion.request.body).toMatchObject({
        nombre: 'Parque Tayrona',
        descripcion: 'Descripción de Tayrona',
        areaProtegida: true,
        activa: true,
        condicionesVisita: 'Llevar agua',
      });
      peticion.flush({ ...TAYRONA, nombre: 'Parque Tayrona' });
      harness.detectChanges();

      expect(tarjetas().map((t) => t.querySelector('h3')?.textContent)).toContain('Parque Tayrona');
      expect(mostrar).toHaveBeenCalledWith('Guardaste los cambios de la zona Parque Tayrona.');
    });
  });

  describe('inactivar y reactivar', () => {
    it('antes de inactivar pide confirmación y no envía nada', async () => {
      await crear();

      botonDe(0, 'Inactivar').click();
      harness.detectChanges();

      const dialogo = raiz().querySelector('dialog')!;
      expect(dialogo.hasAttribute('open')).toBe(true);
      expect(dialogo.textContent).toContain('¿Inactivar la zona Taganga?');
      http.expectNone((p) => p.method === 'PUT');
    });

    it('al confirmar manda la zona completa con activa en false', async () => {
      await crear();
      botonDe(0, 'Inactivar').click();
      harness.detectChanges();
      raiz().querySelector<HTMLButtonElement>('dialog .boton--peligro')!.click();
      harness.detectChanges();

      const peticion = http.expectOne(`${ZONAS}/z1`);
      expect(peticion.request.method).toBe('PUT');
      expect(peticion.request.body).toMatchObject({
        nombre: 'Taganga',
        descripcion: 'Descripción de Taganga',
        altaAfluencia: true,
        buenasPracticas: ['No dejar basura'],
        activa: false,
      });
      peticion.flush({ ...TAGANGA, activa: false });
      harness.detectChanges();

      expect(tarjetas()[0].textContent).toContain('Inactiva');
      expect(botonDe(0, 'Reactivar')).toBeDefined();
      expect(mostrar).toHaveBeenCalledWith('Inactivaste la zona Taganga. Sus ofertas siguen visibles.');
    });

    it('reactivar manda la zona completa con activa en true, sin pedir confirmación', async () => {
      await crear();

      botonDe(2, 'Reactivar').click();
      harness.detectChanges();

      const peticion = http.expectOne(`${ZONAS}/z3`);
      expect(peticion.request.body).toMatchObject({ nombre: 'Zona vieja', activa: true });
      peticion.flush({ ...VIEJA, activa: true });
      harness.detectChanges();

      expect(tarjetas()[2].textContent).toContain('Activa');
      expect(mostrar).toHaveBeenCalledWith('Reactivaste la zona Zona vieja.');
    });

    it('una zona incompleta no se reenvía tal cual: avisa en su tarjeta', async () => {
      await crear([zona('z9', 'Sin ubicación', { ubicacion: undefined })]);

      botonDe(0, 'Inactivar').click();
      harness.detectChanges();
      raiz().querySelector<HTMLButtonElement>('dialog .boton--peligro')!.click();
      harness.detectChanges();

      http.expectNone((p) => p.method === 'PUT');
      expect(tarjetas()[0].querySelector('[role="alert"]')).not.toBeNull();
    });
  });

  describe('en inglés', () => {
    it('muestra los textos de la pantalla en inglés', async () => {
      establecerIdioma('en', { recordar: false });
      await crear();

      expect(texto()).toContain('Register an area');
      expect(texto()).toContain('3 areas');
      expect(texto()).toContain('Inactive');
    });
  });
});
