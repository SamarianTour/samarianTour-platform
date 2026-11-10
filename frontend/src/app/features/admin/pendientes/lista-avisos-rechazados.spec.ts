import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AvisoBandeja } from '../../../core/api/modelos-nucleo';
import { AvisosService } from '../../../core/avisos/avisos.service';
import { erroresInterceptor } from '../../../core/errores/errores.interceptor';
import { establecerIdioma } from '../../../core/idioma/idioma';
import { CLAVE_SESION, aSesionGuardada } from '../../../core/sesion/almacen-sesion';
import { respuestaDeSesion } from '../../../core/sesion/testing';
import { ListaAvisosRechazados } from './lista-avisos-rechazados';

const RECHAZADOS = '/api/nucleo/admin/bandeja/rechazados';

function aviso(id: string, extra: Partial<AvisoBandeja> = {}): AvisoBandeja {
  return {
    idAviso: id,
    destino: 'OFERTAS',
    tipo: 'MODERACION',
    estado: 'RECHAZADO',
    intentos: 1,
    ultimoError: '422 Unprocessable Entity: la oferta b00000000000000000000017 no tiene nada en moderación',
    creadoEn: '2026-10-26T15:00:00',
    ...extra,
  };
}

describe('ListaAvisosRechazados', () => {
  let fixture: ComponentFixture<ListaAvisosRechazados>;
  let http: HttpTestingController;
  let mostrar: ReturnType<typeof vi.spyOn>;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const texto = () => (raiz().textContent ?? '').replace(/[  ]/g, ' ');
  const tarjetas = () => Array.from(raiz().querySelectorAll<HTMLElement>('li.elemento'));
  const boton = (tarjeta: number) => tarjetas()[tarjeta].querySelector<HTMLButtonElement>('.acciones button')!;

  function crear(avisos: AvisoBandeja[]): void {
    fixture = TestBed.createComponent(ListaAvisosRechazados);
    fixture.detectChanges();
    http.expectOne(RECHAZADOS).flush(avisos);
    fixture.detectChanges();
  }

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    establecerIdioma('es', { recordar: false });
    localStorage.setItem(
      CLAVE_SESION,
      JSON.stringify(aSesionGuardada(respuestaDeSesion({ rol: 'ADMINISTRADOR' }))),
    );
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([erroresInterceptor])), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    mostrar = vi.spyOn(TestBed.inject(AvisosService), 'mostrar');
  });

  afterEach(() => http.verify());

  it('pide los avisos rechazados', () => {
    fixture = TestBed.createComponent(ListaAvisosRechazados);
    fixture.detectChanges();

    expect(http.expectOne(RECHAZADOS).request.method).toBe('GET');
    http.match(() => true).forEach((p) => p.flush([]));
  });

  it('explica que un aviso rechazado no se reintenta solo y que hay que corregir la causa antes de reenviarlo', () => {
    crear([aviso('a1')]);

    expect(texto()).toContain('no se reintenta solo, porque fallaría igual');
    expect(texto()).toContain('Reenvía solo después de corregir la causa');
    expect(texto()).toContain('en los siguientes 15 segundos');
  });

  it('muestra el tipo, el destino, el identificador, los intentos y cuándo se creó', () => {
    crear([aviso('aviso-0002', { intentos: 3, destino: 'OPINIONES', tipo: 'MODERACION_OPINION' })]);

    const t = tarjetas()[0];
    expect(t.querySelector('h3')?.textContent).toBe('Moderación de opinión');
    expect(t.querySelector('.etiqueta')?.textContent?.trim()).toBe('Para Opiniones');
    expect(t.textContent).toContain('Aviso aviso-0002');
    expect(t.textContent).toContain('3 intentos');
    expect(t.textContent).toContain('26 oct 2026');
  });

  it('con un solo intento usa el singular', () => {
    crear([aviso('a1', { intentos: 1 })]);

    expect(tarjetas()[0].textContent).toContain('1 intento');
    expect(tarjetas()[0].textContent).not.toContain('1 intentos');
  });

  it('muestra lo que respondió el catálogo, en una línea y, si hay más, el mensaje completo aparte', () => {
    crear([
      aviso('a1', { ultimoError: 'Error corto' }),
      aviso('a2', { ultimoError: 'HttpClientErrorException: 409 Conflict\nCausa: la versión ya no es vigente', creadoEn: '2026-10-25T10:00:00' }),
    ]);

    expect(tarjetas()[0].querySelector('.respuesta__linea')?.textContent?.trim()).toBe('Error corto');
    expect(tarjetas()[0].querySelector('details')).toBeNull();
    expect(tarjetas()[1].querySelector('.respuesta__linea')?.textContent?.trim()).toBe(
      'HttpClientErrorException: 409 Conflict',
    );
    expect(tarjetas()[1].querySelector('details pre')?.textContent).toContain('la versión ya no es vigente');
  });

  it('si el catálogo no dejó mensaje lo dice', () => {
    crear([aviso('a1', { ultimoError: undefined })]);

    expect(tarjetas()[0].textContent).toContain('El catálogo no dejó un mensaje.');
  });

  it('un tipo que no conoce se muestra tal cual y uno ausente se llama "Aviso"', () => {
    crear([aviso('a1', { tipo: 'TIPO_NUEVO' }), aviso('a2', { tipo: undefined, creadoEn: '2026-10-25T10:00:00' })]);

    expect(tarjetas()[0].querySelector('h3')?.textContent).toBe('TIPO_NUEVO');
    expect(tarjetas()[1].querySelector('h3')?.textContent).toBe('Aviso');
  });

  it('pone primero el más reciente y cuenta cuántos hay', () => {
    crear([aviso('vieja', { creadoEn: '2026-10-20T10:00:00' }), aviso('nueva', { creadoEn: '2026-10-27T10:00:00' })]);

    expect(tarjetas().map((t) => t.textContent!.includes('Aviso nueva'))).toEqual([true, false]);
    expect(texto()).toContain('2 avisos rechazados');
  });

  it('sin avisos rechazados lo dice', () => {
    crear([]);

    expect(tarjetas()).toHaveLength(0);
    expect(texto()).toContain('No hay avisos rechazados');
  });

  it('si la consulta falla avisa del error', () => {
    fixture = TestBed.createComponent(ListaAvisosRechazados);
    fixture.detectChanges();
    http.expectOne(RECHAZADOS).error(new ProgressEvent('error'));
    fixture.detectChanges();

    expect(raiz().querySelector('.alerta--error')).not.toBeNull();
  });

  describe('reenviar', () => {
    it('reenvía con POST, quita el aviso de la lista y avisa que la bandeja lo enviará', () => {
      crear([aviso('a1'), aviso('a2', { creadoEn: '2026-10-25T10:00:00' })]);

      boton(0).click();
      fixture.detectChanges();
      expect(boton(0).textContent?.trim()).toBe('Reenviando…');
      const peticion = http.expectOne(`${RECHAZADOS.replace('rechazados', 'a1/reenvio')}`);
      expect(peticion.request.method).toBe('POST');
      expect(peticion.request.body).toBeNull();
      peticion.flush({ ...aviso('a1'), estado: 'PENDIENTE', intentos: 0 });
      fixture.detectChanges();

      expect(tarjetas()).toHaveLength(1);
      expect(tarjetas()[0].textContent).toContain('Aviso a2');
      expect(mostrar).toHaveBeenCalledWith('Reenviaste el aviso. La bandeja lo enviará en los siguientes 15 segundos.');
    });

    it('el botón dice a qué aviso se refiere, y mientras reenvía no deja reenviar otro ni repetir', () => {
      crear([aviso('a1'), aviso('a2', { creadoEn: '2026-10-25T10:00:00' })]);
      expect(boton(0).getAttribute('aria-label')).toBe('Reenviar el aviso a1');

      boton(0).click();
      boton(0).click();
      fixture.detectChanges();

      expect(http.match((p) => p.url.endsWith('/reenvio'))).toHaveLength(1);
      expect(boton(1).disabled).toBe(true);
    });

    it.each(['AVISO_NO_RECHAZADO', 'AVISO_NO_ENCONTRADO'])(
      '%s lo explica en la tarjeta y vuelve a leer la lista',
      (codigo) => {
        crear([aviso('a1')]);

        boton(0).click();
        http
          .expectOne((p) => p.url.endsWith('/a1/reenvio'))
          .flush({ codigo }, { status: codigo === 'AVISO_NO_ENCONTRADO' ? 404 : 409, statusText: 'Error' });
        fixture.detectChanges();

        expect(tarjetas()[0].querySelector('.campo__error')).not.toBeNull();
        http.expectOne(RECHAZADOS).flush([aviso('a1')]);
        fixture.detectChanges();
        expect(tarjetas()[0].querySelector('.campo__error')).not.toBeNull();
      },
    );

    it('un error cualquiera se avisa en la tarjeta y deja reintentar', () => {
      crear([aviso('a1')]);

      boton(0).click();
      http.expectOne((p) => p.url.endsWith('/a1/reenvio')).error(new ProgressEvent('error'));
      fixture.detectChanges();

      expect(tarjetas()[0].querySelector('.campo__error')).not.toBeNull();
      expect(boton(0).disabled).toBe(false);
      expect(tarjetas()).toHaveLength(1);
    });

    it('un aviso que no está rechazado no se puede reenviar', () => {
      crear([aviso('a1', { estado: 'PENDIENTE' })]);

      expect(boton(0).disabled).toBe(true);
    });
  });

  it('vuelve a leer la lista al regresar a la pestaña', () => {
    crear([aviso('a1')]);

    document.dispatchEvent(new Event('visibilitychange'));

    http.expectOne(RECHAZADOS).flush([aviso('a1'), aviso('a3', { creadoEn: '2026-10-27T10:00:00' })]);
    fixture.detectChanges();
    expect(tarjetas()).toHaveLength(2);
  });

  it('también se ve en inglés', () => {
    establecerIdioma('en', { recordar: false });
    crear([aviso('a1', { tipo: 'SUPRESION_TURISTA', destino: 'OPINIONES', intentos: 2 })]);

    const t = tarjetas()[0];
    expect(texto()).toContain('Rejected notices to the catalog');
    expect(texto()).toContain('1 rejected notice');
    expect(t.querySelector('h3')?.textContent).toBe('Tourist data deletion');
    expect(t.querySelector('.etiqueta')?.textContent?.trim()).toBe('For Reviews');
    expect(t.textContent).toContain('2 attempts');
    expect(boton(0).textContent?.trim()).toBe('Resend');
  });
});
