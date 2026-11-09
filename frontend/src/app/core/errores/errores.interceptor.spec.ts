import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CodigoError } from '../api/modelos';
import { establecerIdioma } from '../idioma/idioma';
import { traducir } from '../idioma/traducir';
import { ErrorApi, MENSAJES } from './error-api';
import { erroresInterceptor } from './errores.interceptor';

const MENSAJE_GENERICO = 'Algo salió mal. Vuelve a intentarlo en un momento.';
const MENSAJE_SIN_CONEXION =
  'No pudimos conectar con el servidor. Revisa tu conexión y vuelve a intentarlo.';

describe('erroresInterceptor', () => {
  let cliente: HttpClient;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([erroresInterceptor])), provideHttpClientTesting()],
    });
    cliente = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  /** Pide una URL, responde con un problem+json y devuelve el error que recibe quien llamó. */
  function fallar(estado: number, cuerpo: string | object): ErrorApi {
    let recibido: unknown;
    cliente.get('/api/catalogo/ofertas').subscribe({ error: (error: unknown) => (recibido = error) });
    http.expectOne('/api/catalogo/ofertas').flush(cuerpo, { status: estado, statusText: 'Error' });
    expect(recibido).toBeInstanceOf(ErrorApi);
    return recibido as ErrorApi;
  }

  const casos: [CodigoError, number][] = [
    ['NO_AUTENTICADO', 401],
    ['RNT_INACTIVO', 403],
    ['TIPO_OFERTA_NO_PERMITIDO', 403],
    ['PRESTADOR_NO_AUTORIZADO', 403],
    ['DATO_INVALIDO', 400],
    ['ZONA_NO_DISPONIBLE', 400],
    ['OFERTA_NO_ENCONTRADA', 404],
    ['OFERTA_NO_EDITABLE', 409],
    ['CONFLICTO_CONCURRENCIA', 409],
    ['OPINION_NO_HABILITADA', 403],
    ['OPINION_DUPLICADA', 409],
    ['OPINION_NO_ENCONTRADA', 404],
    ['CREDENCIALES_INVALIDAS', 401],
    ['CUENTA_BLOQUEADA', 423],
    ['CUENTA_DESACTIVADA', 403],
    ['SESION_INVALIDA', 401],
    ['CORREO_YA_REGISTRADO', 409],
    ['POLITICA_NO_VIGENTE', 409],
    ['CODIGO_INCORRECTO', 400],
    ['CODIGO_NO_VIGENTE', 409],
    ['CORREO_NO_DISPONIBLE', 503],
    ['CONTRASENA_INCORRECTA', 401],
    ['PRESTADOR_CON_COMPROMISOS', 409],
    ['WHATSAPP_NO_VERIFICADO', 409],
    ['RECOMENDACION_NO_DISPONIBLE', 503],
  ];

  it.each(casos)('traduce %s a un mensaje para el usuario, sin mostrar el código', (codigo, estado) => {
    const error = fallar(estado, { status: estado, codigo, detail: 'detalle técnico' });

    expect(error.estado).toBe(estado);
    expect(error.codigo).toBe(codigo);
    expect(error.mensaje).toBe(MENSAJES[codigo]);
    expect(error.mensaje).not.toContain(codigo);
  });

  it('ningún mensaje queda vacío, muestra su código ni habla del usuario demo', () => {
    for (const [codigo, mensaje] of Object.entries(MENSAJES)) {
      expect(mensaje.trim(), codigo).not.toBe('');
      expect(mensaje, codigo).not.toContain(codigo);
      expect(mensaje.toLowerCase(), codigo).not.toContain('usuario demo');
    }
  });

  describe('idioma', () => {
    it('el mensaje sale en inglés con la interfaz en inglés', () => {
      establecerIdioma('en', { recordar: false });

      const error = fallar(401, { status: 401, codigo: 'NO_AUTENTICADO' });

      expect(error.mensaje).toBe('Log in to continue.');
    });

    it('un error que ya existe cambia de idioma junto con la interfaz', () => {
      const error = fallar(409, { status: 409, codigo: 'OFERTA_NO_EDITABLE' });
      expect(error.mensaje).toBe('Esta oferta está retirada y ya no se puede editar.');

      establecerIdioma('en', { recordar: false });
      expect(error.mensaje).toBe('This offer has been withdrawn and can no longer be edited.');

      establecerIdioma('es', { recordar: false });
      expect(error.mensaje).toBe('Esta oferta está retirada y ya no se puede editar.');
    });

    it('los errores sin código también: genérico y sin conexión', () => {
      establecerIdioma('en', { recordar: false });

      expect(fallar(500, '<html>Bad Gateway</html>').mensaje).toBe(
        'Something went wrong. Please try again in a moment.',
      );
      expect(traducir('errores.sinConexion')).toContain("couldn't connect");
    });

    it('MENSAJES entrega cada texto en el idioma vigente', () => {
      expect(MENSAJES.CORREO_YA_REGISTRADO).toContain('Ya hay una cuenta');

      establecerIdioma('en', { recordar: false });

      expect(MENSAJES.CORREO_YA_REGISTRADO).toContain('There is already an account');
    });

    it('en inglés ningún mensaje queda vacío ni muestra su código', () => {
      establecerIdioma('en', { recordar: false });

      for (const [codigo, mensaje] of Object.entries(MENSAJES)) {
        expect(mensaje.trim(), codigo).not.toBe('');
        expect(mensaje, codigo).not.toContain(codigo);
        expect(mensaje, codigo).not.toMatch(/[áéíóúñ¿¡]/i);
      }
    });
  });

  it('pide iniciar sesión cuando falta el token', () => {
    expect(fallar(401, { status: 401, codigo: 'NO_AUTENTICADO' }).mensaje).toBe('Inicia sesión para continuar.');
  });

  it('conserva hasta cuándo dura el bloqueo de la cuenta (RF-05)', () => {
    const error = fallar(423, {
      status: 423,
      codigo: 'CUENTA_BLOQUEADA',
      bloqueadaHasta: '2026-10-28T15:15:00Z',
    });

    expect(error.extras?.bloqueadaHasta).toBe('2026-10-28T15:15:00Z');
  });

  it('conserva los intentos que quedan para un código de WhatsApp', () => {
    const error = fallar(400, { status: 400, codigo: 'CODIGO_INCORRECTO', intentosRestantes: 2 });

    expect(error.extras?.intentosRestantes).toBe(2);
  });

  it('conserva la lista de compromisos que impide eliminar los datos del prestador', () => {
    const error = fallar(409, {
      status: 409,
      codigo: 'PRESTADOR_CON_COMPROMISOS',
      compromisos: ['2 dispersiones por recibir'],
    });

    expect(error.extras?.compromisos).toEqual(['2 dispersiones por recibir']);
  });

  it('descarta datos extra con un tipo inesperado y no inventa extras si no hay', () => {
    const raro = fallar(409, {
      status: 409,
      codigo: 'PRESTADOR_CON_COMPROMISOS',
      compromisos: [1, 2],
      intentosRestantes: 'dos',
    });
    const comun = fallar(404, { status: 404, codigo: 'OFERTA_NO_ENCONTRADA' });

    expect(raro.extras).toBeUndefined();
    expect(comun.extras).toBeUndefined();
  });

  it('avisa del conflicto de concurrencia con el mensaje acordado', () => {
    const error = fallar(409, { status: 409, codigo: 'CONFLICTO_CONCURRENCIA' });

    expect(error.mensaje).toBe('Alguien más modificó esta oferta, vuelve a intentarlo.');
  });

  it('saca de DATO_INVALIDO el campo afectado y lo que la API dice de él', () => {
    const error = fallar(400, {
      status: 400,
      codigo: 'DATO_INVALIDO',
      detail: 'capacidadMaxima: debe ser mayor que cero',
    });

    expect(error.campo).toBe('capacidadMaxima');
    expect(error.detalleCampo).toBe('debe ser mayor que cero');
  });

  it('deja DATO_INVALIDO sin campo si el detalle no lo nombra', () => {
    const error = fallar(400, { status: 400, codigo: 'DATO_INVALIDO', detail: 'El cuerpo no se pudo leer' });

    expect(error.campo).toBeUndefined();
    expect(error.mensaje).toBe(MENSAJES.DATO_INVALIDO);
  });

  it('reconoce una oferta que ya no existe', () => {
    const error = fallar(404, { status: 404, codigo: 'OFERTA_NO_ENCONTRADA' });

    expect(error.noEncontrado).toBe(true);
    expect(error.mensaje).toBe('Esta oferta ya no está disponible.');
  });

  it('lee el problem+json aunque llegue como texto', () => {
    const error = fallar(403, JSON.stringify({ status: 403, codigo: 'RNT_INACTIVO' }));

    expect(error.codigo).toBe('RNT_INACTIVO');
  });

  it('usa un mensaje genérico cuando la respuesta no trae un código conocido', () => {
    expect(fallar(500, '<html>Bad Gateway</html>').mensaje).toBe(MENSAJE_GENERICO);
    expect(fallar(418, { status: 418, codigo: 'ALGO_NUEVO' }).mensaje).toBe(MENSAJE_GENERICO);
  });

  it('avisa cuando no hay conexión con el servidor', () => {
    let recibido: unknown;
    cliente.get('/api/catalogo/zonas').subscribe({ error: (error: unknown) => (recibido = error) });

    http.expectOne('/api/catalogo/zonas').error(new ProgressEvent('error'));

    expect((recibido as ErrorApi).estado).toBe(0);
    expect((recibido as ErrorApi).mensaje).toBe(MENSAJE_SIN_CONEXION);
  });
});
