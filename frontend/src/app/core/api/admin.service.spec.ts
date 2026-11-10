import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AdminService } from './admin.service';

const BASE = '/api/nucleo/admin';

describe('AdminService', () => {
  let servicio: AdminService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servicio = TestBed.inject(AdminService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('pide todos los prestadores sin el parámetro sinOperador', () => {
    servicio.prestadores().subscribe();

    const peticion = http.expectOne((p) => p.url === `${BASE}/prestadores`);
    expect(peticion.request.method).toBe('GET');
    expect(peticion.request.params.has('sinOperador')).toBe(false);
    peticion.flush([]);
  });

  it('pide solo los que faltan por asignar con sinOperador=true', () => {
    servicio.prestadores(true).subscribe();

    const peticion = http.expectOne((p) => p.url === `${BASE}/prestadores`);
    expect(peticion.request.params.get('sinOperador')).toBe('true');
    peticion.flush([]);
  });

  it('pide las alertas de sesgo abiertas', () => {
    let alertas: unknown;

    servicio.alertasSesgo().subscribe((a) => (alertas = a));

    const peticion = http.expectOne(`${BASE}/alertas-sesgo`);
    expect(peticion.request.method).toBe('GET');
    peticion.flush([{ prestadorId: 'p1', indice: 3 }]);
    expect(alertas).toEqual([{ prestadorId: 'p1', indice: 3 }]);
  });

  it('pide las cuentas internas', () => {
    servicio.cuentasInternas().subscribe();

    const peticion = http.expectOne(`${BASE}/cuentas-internas`);
    expect(peticion.request.method).toBe('GET');
    peticion.flush([]);
  });

  it('asigna el operador con PUT y solo el identificador del operador en el cuerpo', () => {
    let prestador: unknown;

    servicio.asignarOperador('50000000-0000-0000-0000-000000000005', 'op-1').subscribe((p) => (prestador = p));

    const peticion = http.expectOne(`${BASE}/prestadores/50000000-0000-0000-0000-000000000005/operador`);
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual({ operadorId: 'op-1' });
    peticion.flush({ id: '50000000-0000-0000-0000-000000000005', operadorId: 'op-1' });
    expect(prestador).toEqual({ id: '50000000-0000-0000-0000-000000000005', operadorId: 'op-1' });
  });

  it('codifica el identificador del prestador en la dirección', () => {
    servicio.asignarOperador('a/b', 'op-1').subscribe();

    http.expectOne(`${BASE}/prestadores/a%2Fb/operador`).flush({});
  });

  it('sincroniza con el RNT con POST y sin cuerpo', () => {
    let resultado: unknown;

    servicio.sincronizarRnt().subscribe((r) => (resultado = r));

    const peticion = http.expectOne(`${BASE}/rnt/sincronizacion`);
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toBeNull();
    peticion.flush({ revisados: 5, retirados: 1, restituidos: 0, sinRespuesta: 0, interrumpida: false });
    expect(resultado).toEqual({ revisados: 5, retirados: 1, restituidos: 0, sinRespuesta: 0, interrumpida: false });
  });

  it('la sincronización no tiene tiempo máximo: responde aunque tarde mucho', () => {
    vi.useFakeTimers();
    try {
      let resultado: unknown;
      let fallo: unknown;
      servicio.sincronizarRnt().subscribe({ next: (r) => (resultado = r), error: (e) => (fallo = e) });
      const peticion = http.expectOne(`${BASE}/rnt/sincronizacion`);

      // Pasan 30 minutos sin respuesta: nada del lado del navegador debe haber cortado la petición.
      vi.advanceTimersByTime(30 * 60_000);
      expect(peticion.cancelled).toBe(false);
      expect(fallo).toBeUndefined();

      peticion.flush({ revisados: 5, retirados: 0, restituidos: 0, sinRespuesta: 0, interrumpida: false });
      expect(resultado).toMatchObject({ revisados: 5 });
    } finally {
      vi.useRealTimers();
    }
  });

  it('decide una oferta con el número de la versión revisada y el motivo del rechazo', () => {
    let decision: unknown;

    servicio
      .moderarOferta('b00000000000000000000017', { numeroVersion: 2, decision: 'RECHAZAR', motivo: 'Falta el punto de encuentro' })
      .subscribe((d) => (decision = d));

    const peticion = http.expectOne(`${BASE}/moderacion/ofertas/b00000000000000000000017/decision`);
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual({ numeroVersion: 2, decision: 'RECHAZAR', motivo: 'Falta el punto de encuentro' });
    peticion.flush({ tipo: 'OFERTA', moderadoId: 'b00000000000000000000017', decision: 'RECHAZAR' });
    expect(decision).toMatchObject({ tipo: 'OFERTA', decision: 'RECHAZAR' });
  });

  it('decide una opinión sin número de versión', () => {
    servicio.moderarOpinion('op-9', { decision: 'APROBAR' }).subscribe();

    const peticion = http.expectOne(`${BASE}/moderacion/opiniones/op-9/decision`);
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual({ decision: 'APROBAR' });
    peticion.flush({ tipo: 'OPINION', moderadoId: 'op-9', decision: 'APROBAR' });
  });

  it('codifica el identificador de lo que se modera en la dirección', () => {
    servicio.moderarOpinion('a/b', { decision: 'APROBAR' }).subscribe();

    http.expectOne(`${BASE}/moderacion/opiniones/a%2Fb/decision`).flush({});
  });

  it('crea una cuenta interna con POST y el rol, el nombre y el correo', () => {
    let creada: unknown;

    servicio
      .crearCuentaInterna({ rol: 'OPERADOR_DISPONIBILIDAD', nombre: 'Valentina Ríos', correo: 'valentina@samariantour.co' })
      .subscribe((c) => (creada = c));

    const peticion = http.expectOne(`${BASE}/cuentas-internas`);
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual({
      rol: 'OPERADOR_DISPONIBILIDAD',
      nombre: 'Valentina Ríos',
      correo: 'valentina@samariantour.co',
    });
    peticion.flush({ id: 'c1', estado: 'ACTIVA', contrasenaEstablecida: false });
    expect(creada).toMatchObject({ id: 'c1', contrasenaEstablecida: false });
  });

  it('desactiva y reactiva una cuenta con POST y sin cuerpo', () => {
    servicio.desactivarCuentaInterna('c1').subscribe();
    const baja = http.expectOne(`${BASE}/cuentas-internas/c1/desactivacion`);
    expect(baja.request.method).toBe('POST');
    expect(baja.request.body).toBeNull();
    baja.flush({ id: 'c1', estado: 'DESACTIVADA' });

    servicio.reactivarCuentaInterna('c1').subscribe();
    const alta = http.expectOne(`${BASE}/cuentas-internas/c1/reactivacion`);
    expect(alta.request.method).toBe('POST');
    expect(alta.request.body).toBeNull();
    alta.flush({ id: 'c1', estado: 'ACTIVA' });
  });

  it('codifica el identificador de la cuenta en la dirección', () => {
    servicio.desactivarCuentaInterna('a/b').subscribe();

    http.expectOne(`${BASE}/cuentas-internas/a%2Fb/desactivacion`).flush({});
  });

  it('pide la comisión vigente con su historial', () => {
    let comision: unknown;

    servicio.comision().subscribe((c) => (comision = c));

    const peticion = http.expectOne(`${BASE}/comision`);
    expect(peticion.request.method).toBe('GET');
    peticion.flush({ porcentaje: 4, vigenteDesde: '2026-01-01T00:00:00Z', historial: [] });
    expect(comision).toMatchObject({ porcentaje: 4 });
  });

  it('cambia la comisión con PUT y solo el porcentaje', () => {
    let comision: unknown;

    servicio.cambiarComision(5.5).subscribe((c) => (comision = c));

    const peticion = http.expectOne(`${BASE}/comision`);
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual({ porcentaje: 5.5 });
    peticion.flush({ porcentaje: 5.5, vigenteDesde: '2026-10-27T15:00:00Z', historial: [] });
    expect(comision).toMatchObject({ porcentaje: 5.5 });
  });

  it('pide los pagos tardíos', () => {
    servicio.pagosTardios().subscribe();

    const peticion = http.expectOne(`${BASE}/pagos-tardios`);
    expect(peticion.request.method).toBe('GET');
    peticion.flush([]);
  });
});
