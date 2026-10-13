import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CodigoError } from '../api/modelos';
import { ErrorApi, MENSAJES, MENSAJE_GENERICO, MENSAJE_SIN_CONEXION } from './error-api';
import { erroresInterceptor } from './errores.interceptor';

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
  ];

  it.each(casos)('traduce %s a un mensaje para el usuario, sin mostrar el código', (codigo, estado) => {
    const error = fallar(estado, { status: estado, codigo, detail: 'detalle técnico' });

    expect(error.estado).toBe(estado);
    expect(error.codigo).toBe(codigo);
    expect(error.mensaje).toBe(MENSAJES[codigo]);
    expect(error.mensaje).not.toContain(codigo);
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
