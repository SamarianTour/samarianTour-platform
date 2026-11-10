import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ErrorApi } from '../errores/error-api';
import { erroresInterceptor } from '../errores/errores.interceptor';
import { PerfilPublico } from './modelos';
import { PerfilPrestadorService } from './perfil-prestador.service';

const BASE = '/api/catalogo/prestador/perfil';

const PERFIL: PerfilPublico = { nombreComercial: 'Mar y Selva', descripcion: 'Caminatas', fotos: [], redesSociales: [] };

function problema(status: number, codigo: string) {
  return {
    body: { status, codigo, title: codigo, detail: codigo },
    init: { status, statusText: codigo, headers: { 'Content-Type': 'application/problem+json' } },
  };
}

describe('PerfilPrestadorService', () => {
  let servicio: PerfilPrestadorService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([erroresInterceptor])), provideHttpClientTesting()],
    });
    servicio = TestBed.inject(PerfilPrestadorService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('consulta su perfil con GET', () => {
    let recibido: PerfilPublico | null | undefined;

    servicio.consultar().subscribe((perfil) => (recibido = perfil));

    const peticion = http.expectOne(BASE);
    expect(peticion.request.method).toBe('GET');
    peticion.flush(PERFIL);
    expect(recibido).toEqual(PERFIL);
  });

  it('si todavía no tiene perfil devuelve null en vez de fallar', () => {
    let recibido: PerfilPublico | null | undefined;
    let fallo: unknown;

    servicio.consultar().subscribe({ next: (perfil) => (recibido = perfil), error: (e) => (fallo = e) });

    const { body, init } = problema(404, 'PERFIL_NO_ENCONTRADO');
    http.expectOne(BASE).flush(body, init);
    expect(recibido).toBeNull();
    expect(fallo).toBeUndefined();
  });

  it('cualquier otro fallo al consultar sigue siendo un fallo', () => {
    let fallo: unknown;

    servicio.consultar().subscribe({ error: (e) => (fallo = e) });

    const { body, init } = problema(403, 'ACCESO_DENEGADO');
    http.expectOne(BASE).flush(body, init);
    expect(fallo).toBeInstanceOf(ErrorApi);
  });

  it('guarda el perfil con PUT', () => {
    const cuerpo = { descripcion: 'Caminatas', redesSociales: [] };

    servicio.guardar(cuerpo).subscribe();

    const peticion = http.expectOne(BASE);
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual(cuerpo);
    peticion.flush(PERFIL);
  });

  it('sube una foto como multipart con el campo "foto"', () => {
    const archivo = new File(['x'], 'uno.jpg', { type: 'image/jpeg' });

    servicio.subirFoto(archivo).subscribe();

    const peticion = http.expectOne(`${BASE}/fotos`);
    expect(peticion.request.method).toBe('POST');
    expect((peticion.request.body as FormData).get('foto')).toBe(archivo);
    peticion.flush(PERFIL, { status: 201, statusText: 'Created' });
  });

  it('quita una foto con DELETE y codifica su identificador', () => {
    servicio.quitarFoto('a/b').subscribe();

    const peticion = http.expectOne(`${BASE}/fotos/a%2Fb`);
    expect(peticion.request.method).toBe('DELETE');
    peticion.flush(null, { status: 204, statusText: 'No Content' });
  });
});
