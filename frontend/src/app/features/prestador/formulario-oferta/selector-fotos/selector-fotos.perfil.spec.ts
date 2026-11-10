import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { Foto } from '../../../../core/api/modelos';
import { ErrorApi } from '../../../../core/errores/error-api';
import { erroresInterceptor } from '../../../../core/errores/errores.interceptor';
import { establecerIdioma } from '../../../../core/idioma/idioma';
import { TextosFotos } from '../fotos-oferta';
import { SelectorFotos } from './selector-fotos';

/**
 * El selector de fotos lo usan las ofertas y el perfil público (P03.2). Estas pruebas cubren el modo del perfil, con entradas;
 * el de las ofertas, sin ellas, lo cubre `selector-fotos.spec.ts`.
 */
const TEXTOS_PERFIL: TextosFotos = {
  leyenda: 'prestador.perfil.fotos.leyenda',
  ayuda: 'prestador.perfil.fotos.ayuda',
  alt: 'prestador.perfil.fotos.alt',
  altPortada: 'prestador.perfil.fotos.alt',
  agregada: 'prestador.perfil.fotos.agregada',
  quitada: 'prestador.perfil.fotos.quitada',
  quitadaSinFotos: 'prestador.perfil.fotos.quitadaSinFotos',
  motivoMaximo: 'prestador.perfil.fotos.motivoMaximo',
};

function foto(id: string): Foto {
  return { id, url: `/api/catalogo/fotos/${id}/galeria`, urlMiniatura: `/api/catalogo/fotos/${id}/miniatura` };
}

function archivo(nombre: string): File {
  return new File(['contenido'], nombre, { type: 'image/jpeg' });
}

describe('SelectorFotos en modo perfil', () => {
  let fixture: ComponentFixture<SelectorFotos>;
  let http: HttpTestingController;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const texto = () => (raiz().textContent ?? '').replace(/\s+/g, ' ');
  const ids = () => fixture.componentInstance.fotos().map((una) => una.id);
  const botones = (accion: string) => raiz().querySelectorAll<HTMLButtonElement>(`[data-accion="${accion}"]`);

  function elegir(archivos: File[]): void {
    const entrada = raiz().querySelector<HTMLInputElement>('input[type="file"]')!;
    Object.defineProperty(entrada, 'files', { value: archivos, configurable: true });
    entrada.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  }

  function configurar(entradas: Record<string, unknown> = {}): void {
    fixture.componentRef.setInput('textos', TEXTOS_PERFIL);
    fixture.componentRef.setInput('ordenable', false);
    fixture.componentRef.setInput('obligatorio', false);
    for (const [nombre, valor] of Object.entries(entradas)) {
      fixture.componentRef.setInput(nombre, valor);
    }
    fixture.detectChanges();
  }

  beforeEach(() => {
    establecerIdioma('es', { recordar: false });
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([erroresInterceptor])), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(SelectorFotos);
  });

  afterEach(() => {
    establecerIdioma('es', { recordar: false });
    http.verify();
  });

  it('dice "del perfil" y no pide una foto como mínimo', () => {
    configurar();

    expect(raiz().querySelector('legend')?.textContent).toContain('Fotos del perfil');
    expect(raiz().querySelector('legend .requerido')).toBeNull();
    expect(texto()).toContain('Puedes tener hasta 6 fotos');
    expect(texto()).not.toContain('portada');
  });

  it('sin orden: no hay botones de mover ni portada', () => {
    configurar({ fotos: [foto('a'), foto('b')] });

    expect(botones('antes')).toHaveLength(0);
    expect(botones('despues')).toHaveLength(0);
    expect(botones('quitar')).toHaveLength(2);
    expect(raiz().querySelector('.miniatura__portada')).toBeNull();
    expect(raiz().querySelector('img')?.getAttribute('alt')).toBe('Foto 1 de 2 del perfil');
  });

  it('sube con la función que le dan, no con la de las ofertas', () => {
    const subirCon = vi.fn().mockReturnValue(of(foto('nueva')));
    configurar({ subirCon });

    elegir([archivo('uno.jpg')]);

    expect(subirCon).toHaveBeenCalledTimes(1);
    expect(subirCon.mock.calls[0][0]).toBeInstanceOf(File);
    expect(ids()).toEqual(['nueva']);
    http.expectNone(() => true);
    expect(texto()).toContain('El perfil tiene 1 de 6 fotos.');
  });

  it('al llegar al máximo dice que el perfil lleva máximo 6 fotos', () => {
    configurar({ fotos: ['a', 'b', 'c', 'd', 'e'].map(foto), subirCon: () => of(foto('f')) });

    elegir([archivo('uno.jpg'), archivo('dos.jpg')]);

    expect(ids()).toHaveLength(6);
    expect(texto()).toContain('No se subió «dos.jpg»: el perfil lleva máximo 6 fotos.');
  });

  describe('quitar', () => {
    it('sin función para quitar, la foto sale solo de la lista (como en una oferta)', () => {
      configurar({ fotos: [foto('a'), foto('b')] });

      botones('quitar')[0].click();
      fixture.detectChanges();

      expect(ids()).toEqual(['b']);
      http.expectNone(() => true);
    });

    it('con la función para quitar, la foto sale de la lista cuando el servidor responde', () => {
      const pendiente = new Subject<unknown>();
      const quitarCon = vi.fn().mockReturnValue(pendiente);
      configurar({ fotos: [foto('a'), foto('b')], quitarCon });

      botones('quitar')[0].click();
      fixture.detectChanges();

      expect(quitarCon).toHaveBeenCalledWith(foto('a'));
      // Mientras responde, la foto sigue y su botón queda deshabilitado.
      expect(ids()).toEqual(['a', 'b']);
      expect(botones('quitar')[0].disabled).toBe(true);

      pendiente.next(undefined);
      pendiente.complete();
      fixture.detectChanges();

      expect(ids()).toEqual(['b']);
      expect(texto()).toContain('Foto quitada. El perfil tiene 1 de 6 fotos.');
    });

    it('quitar la última foto lo anuncia con el texto del perfil', () => {
      configurar({ fotos: [foto('a')], quitarCon: () => of(undefined) });

      botones('quitar')[0].click();
      fixture.detectChanges();

      expect(ids()).toEqual([]);
      expect(texto()).toContain('Foto quitada. El perfil no tiene fotos.');
    });

    it('si el servidor falla, la foto se queda y se explica por qué', () => {
      const fallo = new ErrorApi(409, 'CONFLICTO_CONCURRENCIA', 'errores.codigo.CONFLICTO_CONCURRENCIA');
      configurar({ fotos: [foto('a')], quitarCon: () => throwError(() => fallo) });

      botones('quitar')[0].click();
      fixture.detectChanges();

      expect(ids()).toEqual(['a']);
      expect(raiz().querySelector('.alerta--aviso')?.textContent).toContain(fallo.mensaje);
      expect(botones('quitar')[0].disabled).toBe(false);
    });
  });

  it('en inglés usa los textos del perfil', () => {
    establecerIdioma('en', { recordar: false });
    configurar({ fotos: [foto('a')] });

    expect(raiz().querySelector('legend')?.textContent).toContain('Profile photos');
    expect(raiz().querySelector('img')?.getAttribute('alt')).toBe('Photo 1 of 1 of the profile');
  });
});
