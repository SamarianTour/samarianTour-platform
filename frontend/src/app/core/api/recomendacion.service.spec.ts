import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { erroresInterceptor } from '../errores/errores.interceptor';
import { establecerIdioma } from '../idioma/idioma';
import { Resultado } from './degradacion';
import { Alternativas, AlertasZona, Recomendaciones, ResumenAlertas } from './modelos-recomendacion';
import { RecomendacionService } from './recomendacion.service';

const BASE = '/api/recomendacion';

const RECOMENDACIONES: Recomendaciones = {
  motor: 'REGLAS',
  recomendaciones: [
    {
      posicion: 1,
      oferta: { id: 'b1', nombre: 'Aves en Minca', tipo: 'ACTIVIDAD', precioReferencia: 120000, unidadPrecio: 'por persona' },
      criterios: ['TIPO_ACTIVIDAD'],
      explicacion: 'Coincide con: avistamiento de fauna.',
    },
  ],
};

function problema(status: number, codigo: string) {
  return {
    body: { status, codigo, title: codigo, detail: codigo },
    init: { status, statusText: codigo, headers: { 'Content-Type': 'application/problem+json' } },
  };
}

describe('RecomendacionService', () => {
  let servicio: RecomendacionService;
  let http: HttpTestingController;

  beforeEach(() => {
    establecerIdioma('es', { recordar: false });
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([erroresInterceptor])), provideHttpClientTesting()],
    });
    servicio = TestBed.inject(RecomendacionService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    vi.useRealTimers();
    establecerIdioma('es', { recordar: false });
    http.verify();
  });

  describe('recomendar', () => {
    it('manda la consulta con POST en el cuerpo, no en la dirección, con el idioma vigente', () => {
      let resultado: Resultado<Recomendaciones> | undefined;

      servicio.recomendar({ texto: 'Quiero ver aves', precioMaximo: 150000 }).subscribe((r) => (resultado = r));

      const peticion = http.expectOne(`${BASE}/recomendaciones`);
      expect(peticion.request.method).toBe('POST');
      expect(peticion.request.url).not.toContain('aves');
      expect(peticion.request.body).toEqual({ texto: 'Quiero ver aves', precioMaximo: 150000, idioma: 'es' });
      peticion.flush(RECOMENDACIONES);
      expect(resultado).toEqual({ estado: 'ok', datos: RECOMENDACIONES });
    });

    it('el idioma que ya trae la consulta se respeta', () => {
      establecerIdioma('en', { recordar: false });

      servicio.recomendar({ texto: 'aves', idioma: 'es' }).subscribe();

      const peticion = http.expectOne(`${BASE}/recomendaciones`);
      expect(peticion.request.body.idioma).toBe('es');
      peticion.flush(RECOMENDACIONES);
    });

    it('el idioma de la interfaz va en el cuerpo', () => {
      establecerIdioma('en', { recordar: false });

      servicio.recomendar({ texto: 'birds' }).subscribe();

      const peticion = http.expectOne(`${BASE}/recomendaciones`);
      expect(peticion.request.body.idioma).toBe('en');
      peticion.flush(RECOMENDACIONES);
    });

    it('espera hasta 10 s: la recomendación puede tardar hasta unos 9 s y responde', () => {
      vi.useFakeTimers();
      let resultado: Resultado<Recomendaciones> | undefined;
      servicio.recomendar({ texto: 'aves' }).subscribe((r) => (resultado = r));
      const peticion = http.expectOne(`${BASE}/recomendaciones`);

      vi.advanceTimersByTime(9_000);
      expect(resultado).toBeUndefined();
      peticion.flush(RECOMENDACIONES);

      expect(resultado).toEqual({ estado: 'ok', datos: RECOMENDACIONES });
    });

    it('a los 10 s sin respuesta cancela la petición y el servicio queda "no disponible" por tiempo', () => {
      vi.useFakeTimers();
      let resultado: Resultado<Recomendaciones> | undefined;
      servicio.recomendar({ texto: 'aves' }).subscribe((r) => (resultado = r));
      const peticion = http.expectOne(`${BASE}/recomendaciones`);

      vi.advanceTimersByTime(10_000);

      expect(resultado).toEqual({ estado: 'no-disponible', motivo: 'tiempo' });
      expect(peticion.cancelled).toBe(true);
    });

    it.each([502, 503])('un %s es "no disponible"', (estado) => {
      let resultado: Resultado<Recomendaciones> | undefined;
      servicio.recomendar({ texto: 'aves' }).subscribe((r) => (resultado = r));

      const { body, init } = problema(estado, 'RECOMENDACION_NO_DISPONIBLE');
      http.expectOne(`${BASE}/recomendaciones`).flush(body, init);

      expect(resultado).toEqual({ estado: 'no-disponible', motivo: 'servicio' });
    });

    it('sin conexión es "no disponible"', () => {
      let resultado: Resultado<Recomendaciones> | undefined;
      servicio.recomendar({ texto: 'aves' }).subscribe((r) => (resultado = r));

      http.expectOne(`${BASE}/recomendaciones`).error(new ProgressEvent('error'));

      expect(resultado).toEqual({ estado: 'no-disponible', motivo: 'conexion' });
    });

    it('un dato inválido (400) es un error de la consulta, no "no disponible"', () => {
      let resultado: Resultado<Recomendaciones> | undefined;
      servicio.recomendar({ texto: 'x'.repeat(600) }).subscribe((r) => (resultado = r));

      const { body, init } = problema(400, 'DATO_INVALIDO');
      http.expectOne(`${BASE}/recomendaciones`).flush(body, init);

      expect(resultado?.estado).toBe('error');
    });
  });

  describe('alertas', () => {
    it('pide todas las alertas con GET y el idioma como parámetro', () => {
      let resultado: Resultado<ResumenAlertas> | undefined;
      const resumen: ResumenAlertas = { actualizadoEn: '2026-10-24T23:42:10Z', zonas: [] };

      servicio.alertas().subscribe((r) => (resultado = r));

      const peticion = http.expectOne((p) => p.url === `${BASE}/alertas`);
      expect(peticion.request.method).toBe('GET');
      expect(peticion.request.params.get('idioma')).toBe('es');
      peticion.flush(resumen);
      expect(resultado).toEqual({ estado: 'ok', datos: resumen });
    });

    it('con la interfaz en inglés pide el título y la descripción en inglés', () => {
      establecerIdioma('en', { recordar: false });

      servicio.alertas().subscribe();

      const peticion = http.expectOne((p) => p.url === `${BASE}/alertas`);
      expect(peticion.request.params.get('idioma')).toBe('en');
      peticion.flush({ actualizadoEn: '2026-10-24T23:42:10Z', zonas: [] });
    });

    it('la página de alertas espera hasta 8 s', () => {
      vi.useFakeTimers();
      let resultado: Resultado<ResumenAlertas> | undefined;
      servicio.alertas().subscribe((r) => (resultado = r));
      const peticion = http.expectOne((p) => p.url === `${BASE}/alertas`);

      vi.advanceTimersByTime(7_999);
      expect(resultado).toBeUndefined();
      vi.advanceTimersByTime(1);

      expect(resultado).toEqual({ estado: 'no-disponible', motivo: 'tiempo' });
      expect(peticion.cancelled).toBe(true);
    });

    it('ALERTAS_NO_DISPONIBLES (503) es "no disponible"', () => {
      let resultado: Resultado<ResumenAlertas> | undefined;
      servicio.alertas().subscribe((r) => (resultado = r));

      const { body, init } = problema(503, 'ALERTAS_NO_DISPONIBLES');
      http.expectOne((p) => p.url === `${BASE}/alertas`).flush(body, init);

      expect(resultado).toEqual({ estado: 'no-disponible', motivo: 'servicio' });
    });
  });

  describe('alertasDeZona', () => {
    it('pide las alertas de una zona, con su id codificado', () => {
      let resultado: Resultado<AlertasZona> | undefined;
      const zona: AlertasZona = { zonaId: 'a/1', actualizadoEn: '2026-10-24T23:42:10Z', alertas: [] };

      servicio.alertasDeZona('a/1').subscribe((r) => (resultado = r));

      const peticion = http.expectOne((p) => p.url === `${BASE}/zonas/a%2F1/alertas`);
      expect(peticion.request.method).toBe('GET');
      expect(peticion.request.params.get('idioma')).toBe('es');
      peticion.flush(zona);
      expect(resultado).toEqual({ estado: 'ok', datos: zona });
    });

    it('una sección de la ficha espera solo 3 s', () => {
      vi.useFakeTimers();
      let resultado: Resultado<AlertasZona> | undefined;
      servicio.alertasDeZona('z1').subscribe((r) => (resultado = r));
      const peticion = http.expectOne((p) => p.url === `${BASE}/zonas/z1/alertas`);

      vi.advanceTimersByTime(2_999);
      expect(resultado).toBeUndefined();
      vi.advanceTimersByTime(1);

      expect(resultado).toEqual({ estado: 'no-disponible', motivo: 'tiempo' });
      expect(peticion.cancelled).toBe(true);
    });
  });

  describe('alternativas', () => {
    it('pide las alternativas de una oferta con GET y el idioma', () => {
      let resultado: Resultado<Alternativas> | undefined;
      const alternativas: Alternativas = { aplica: false, alternativas: [] };

      servicio.alternativas('b1').subscribe((r) => (resultado = r));

      const peticion = http.expectOne((p) => p.url === `${BASE}/ofertas/b1/alternativas`);
      expect(peticion.request.method).toBe('GET');
      expect(peticion.request.params.get('idioma')).toBe('es');
      peticion.flush(alternativas);
      expect(resultado).toEqual({ estado: 'ok', datos: alternativas });
    });

    it('una sección de la ficha espera solo 3 s', () => {
      vi.useFakeTimers();
      let resultado: Resultado<Alternativas> | undefined;
      servicio.alternativas('b1').subscribe((r) => (resultado = r));
      const peticion = http.expectOne((p) => p.url === `${BASE}/ofertas/b1/alternativas`);

      vi.advanceTimersByTime(3_000);

      expect(resultado).toEqual({ estado: 'no-disponible', motivo: 'tiempo' });
      expect(peticion.cancelled).toBe(true);
    });

    it('una oferta que no está entre las visibles (404) es un error de la consulta', () => {
      let resultado: Resultado<Alternativas> | undefined;
      servicio.alternativas('b1').subscribe((r) => (resultado = r));

      const { body, init } = problema(404, 'OFERTA_NO_ENCONTRADA');
      http.expectOne((p) => p.url === `${BASE}/ofertas/b1/alternativas`).flush(body, init);

      expect(resultado?.estado).toBe('error');
    });

    it('con un límite propio, ese manda', () => {
      vi.useFakeTimers();
      let resultado: Resultado<Alternativas> | undefined;
      servicio.alternativas('b1', 500).subscribe((r) => (resultado = r));
      http.expectOne((p) => p.url === `${BASE}/ofertas/b1/alternativas`);

      vi.advanceTimersByTime(500);

      expect(resultado).toEqual({ estado: 'no-disponible', motivo: 'tiempo' });
    });
  });
});
