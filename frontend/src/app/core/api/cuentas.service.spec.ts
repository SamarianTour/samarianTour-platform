import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CuentasService } from './cuentas.service';
import { RegistroRequest } from './modelos-nucleo';

const BASE = '/api/nucleo';

describe('CuentasService', () => {
  let servicio: CuentasService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servicio = TestBed.inject(CuentasService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lee la política de datos vigente', () => {
    let recibida: unknown;

    servicio.politicaVigente().subscribe((politica) => (recibida = politica));

    const peticion = http.expectOne(`${BASE}/politica-datos/vigente`);
    expect(peticion.request.method).toBe('GET');
    const politica = {
      version: 'v1',
      fechaPublicacion: '2026-10-28',
      urlTexto: `${BASE}/politica-datos/v1`,
    };
    peticion.flush(politica);
    expect(recibida).toEqual(politica);
  });

  it('lee el texto de una versión como texto, no como JSON', () => {
    let texto: string | undefined;

    servicio.textoPolitica('v1').subscribe((t) => (texto = t));

    const peticion = http.expectOne(`${BASE}/politica-datos/v1`);
    expect(peticion.request.responseType).toBe('text');
    peticion.flush('# Política\n\nTexto.');
    expect(texto).toBe('# Política\n\nTexto.');
  });

  it('codifica la versión para que no pueda cambiar la ruta', () => {
    servicio.textoPolitica('v1/../../admin').subscribe();

    http.expectOne(`${BASE}/politica-datos/v1%2F..%2F..%2Fadmin`).flush('');
  });

  it('registra una cuenta con POST', () => {
    const solicitud: RegistroRequest = {
      rol: 'TURISTA',
      nombre: 'Laura Gómez',
      correo: 'laura.gomez@correo.co',
      contrasena: 'Playa2026',
      whatsapp: '+573001234567',
      aceptaPolitica: true,
      versionPolitica: 'v1',
    };
    let cuenta: unknown;

    servicio.registrar(solicitud).subscribe((c) => (cuenta = c));

    const peticion = http.expectOne(`${BASE}/cuentas/registro`);
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual(solicitud);
    const creada = {
      id: 'x',
      rol: 'TURISTA',
      nombre: 'Laura Gómez',
      correo: 'laura.gomez@correo.co',
    };
    peticion.flush(creada, { status: 201, statusText: 'Created' });
    expect(cuenta).toEqual(creada);
  });
});
