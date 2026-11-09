import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { CLAVE_SESION, aSesionGuardada } from '../../../core/sesion/almacen-sesion';
import { SesionService } from '../../../core/sesion/sesion.service';
import { respuestaDeSesion } from '../../../core/sesion/testing';
import { MisOpiniones } from './mis-opiniones';

const URL = '/api/catalogo/turista/opiniones';

const PAGINA = {
  contenido: [
    {
      id: 'op1',
      ofertaId: 'b00000000000000000000001',
      nombreOferta: 'Caminata a Marinka',
      calificacion: 5,
      comentario: 'Una experiencia inolvidable en la montaña.',
      fecha: '2026-11-01T10:00:00Z',
      estado: 'APROBADA',
    },
    {
      id: 'op2',
      ofertaId: 'b00000000000000000000002',
      nombreOferta: 'Snorkel en Bahía Concha',
      calificacion: 2,
      comentario: 'No cumplió lo prometido en el horario.',
      fecha: '2026-11-02T10:00:00Z',
      estado: 'RECHAZADA',
      motivoRechazo: 'Incluye datos de contacto',
    },
  ],
  pagina: 0,
  tamano: 10,
  totalElementos: 2,
  totalPaginas: 1,
};

describe('MisOpiniones (según la sesión)', () => {
  let fixture: ComponentFixture<MisOpiniones>;
  let http: HttpTestingController;

  const texto = () => (fixture.nativeElement as HTMLElement).textContent ?? '';

  function crear(): void {
    localStorage.setItem(
      CLAVE_SESION,
      JSON.stringify(aSesionGuardada(respuestaDeSesion({ rol: 'TURISTA' }))),
    );
    fixture = TestBed.createComponent(MisOpiniones);
    fixture.detectChanges();
  }

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('pide las opiniones de la cuenta de la sesión y las muestra con su estado y el motivo del rechazo', () => {
    crear();

    const peticion = http.expectOne((p) => p.url === URL);
    expect(peticion.request.params.get('page')).toBe('0');
    peticion.flush(PAGINA);
    fixture.detectChanges();

    expect(texto()).toContain('Caminata a Marinka');
    expect(texto()).toContain('Publicada');
    expect(texto()).toContain('Rechazada');
    expect(texto()).toContain('Motivo del rechazo:');
    expect(texto()).toContain('Incluye datos de contacto');
  });

  it('ya no pide elegir un turista en el usuario demo', () => {
    crear();
    http.expectOne((p) => p.url === URL).flush(PAGINA);
    fixture.detectChanges();

    expect(texto()).not.toContain('Usuario demo');
    expect(texto()).not.toContain('Elige un turista');
  });

  it('con la lista vacía invita a opinar desde la ficha de una oferta pagada', () => {
    crear();

    http.expectOne((p) => p.url === URL).flush({ ...PAGINA, contenido: [], totalElementos: 0 });
    fixture.detectChanges();

    expect(texto()).toContain('Todavía no tienes opiniones');
  });

  it('muestra el error del servidor', () => {
    crear();

    http
      .expectOne((p) => p.url === URL)
      .flush({ status: 403, codigo: 'ACCESO_DENEGADO' }, { status: 403, statusText: 'Forbidden' });
    fixture.detectChanges();

    expect(texto()).toContain('no tiene permiso');
  });

  it('editar los datos personales no vuelve a pedir la lista', () => {
    crear();
    http.expectOne((p) => p.url === URL).flush(PAGINA);
    const sesion = TestBed.inject(SesionService);

    sesion.actualizarCuenta({ ...sesion.cuenta()!, nombre: 'Otro nombre' });
    fixture.detectChanges();

    http.expectNone((p) => p.url === URL);
  });
});
