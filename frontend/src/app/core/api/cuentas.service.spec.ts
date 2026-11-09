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

  it('consulta los datos personales', () => {
    let cuenta: unknown;

    servicio.consultarCuenta().subscribe((c) => (cuenta = c));

    const peticion = http.expectOne(`${BASE}/cuenta`);
    expect(peticion.request.method).toBe('GET');
    peticion.flush({
      id: 'x',
      rol: 'TURISTA',
      nombre: 'Laura',
      correo: 'laura@demo.samariantour.co',
    });
    expect(cuenta).toMatchObject({ nombre: 'Laura' });
  });

  it('guarda el nombre y el WhatsApp con PUT', () => {
    let cuenta: unknown;

    servicio
      .guardarDatos({ nombre: 'Laura Gómez Ruiz', whatsapp: '+573001234567' })
      .subscribe((c) => (cuenta = c));

    const peticion = http.expectOne(`${BASE}/cuenta`);
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual({
      nombre: 'Laura Gómez Ruiz',
      whatsapp: '+573001234567',
    });
    peticion.flush({ id: 'x', rol: 'TURISTA', nombre: 'Laura Gómez Ruiz', correo: 'a@b.co' });
    expect(cuenta).toMatchObject({ nombre: 'Laura Gómez Ruiz' });
  });

  it('el turista elimina sus datos confirmando con su contraseña', () => {
    let terminó = false;

    servicio.suprimirTurista('Demo1234').subscribe(() => (terminó = true));

    const peticion = http.expectOne(`${BASE}/turista/supresion`);
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual({ contrasena: 'Demo1234' });
    peticion.flush(null, { status: 204, statusText: 'No Content' });
    expect(terminó).toBe(true);
  });

  it('el prestador elimina sus datos por su propia ruta', () => {
    let terminó = false;

    servicio.suprimirPrestador('Demo1234').subscribe(() => (terminó = true));

    const peticion = http.expectOne(`${BASE}/prestador/supresion`);
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual({ contrasena: 'Demo1234' });
    peticion.flush(null, { status: 204, statusText: 'No Content' });
    expect(terminó).toBe(true);
  });

  it('pide otro código de WhatsApp con POST y sin cuerpo', () => {
    let terminó = false;

    servicio.enviarCodigoWhatsapp().subscribe(() => (terminó = true));

    const peticion = http.expectOne(`${BASE}/turista/whatsapp/codigo`);
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toBeNull();
    peticion.flush(null, { status: 202, statusText: 'Accepted' });
    expect(terminó).toBe(true);
  });

  it('verifica el WhatsApp con el código de 6 dígitos', () => {
    let cuenta: unknown;

    servicio.verificarWhatsapp('048213').subscribe((c) => (cuenta = c));

    const peticion = http.expectOne(`${BASE}/turista/whatsapp/verificacion`);
    expect(peticion.request.body).toEqual({ codigo: '048213' });
    peticion.flush({
      id: 'x',
      rol: 'TURISTA',
      nombre: 'Laura',
      correo: 'a@b.co',
      whatsappVerificado: true,
    });
    expect(cuenta).toMatchObject({ whatsappVerificado: true });
  });

  it('pide el enlace para restablecer la contraseña con el correo', () => {
    let terminó = false;

    servicio
      .solicitarRestablecimiento('laura@demo.samariantour.co')
      .subscribe(() => (terminó = true));

    const peticion = http.expectOne(`${BASE}/cuentas/contrasena/restablecimiento`);
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual({ correo: 'laura@demo.samariantour.co' });
    peticion.flush(null, { status: 202, statusText: 'Accepted' });
    expect(terminó).toBe(true);
  });

  it('restablece la contraseña con el token del enlace', () => {
    let terminó = false;

    servicio
      .restablecerContrasena('token-del-correo', 'Playa2027')
      .subscribe(() => (terminó = true));

    const peticion = http.expectOne(`${BASE}/cuentas/contrasena`);
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual({ token: 'token-del-correo', contrasena: 'Playa2027' });
    peticion.flush(null, { status: 204, statusText: 'No Content' });
    expect(terminó).toBe(true);
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
