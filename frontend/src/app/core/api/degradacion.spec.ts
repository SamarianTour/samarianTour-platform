import { HttpErrorResponse } from '@angular/common/http';
import { Observable, Subject, TimeoutError, of, throwError } from 'rxjs';
import { ErrorApi } from '../errores/error-api';
import {
  LIMITE_ALERTAS_PAGINA_MS,
  LIMITE_RECOMENDAR_MS,
  LIMITE_SECCION_MS,
  Resultado,
  conDegradacion,
  motivoNoDisponible,
} from './degradacion';

const errorApi = (estado: number) => new ErrorApi(estado, undefined, 'errores.generico');

/** Recoge lo que emite un observable y si terminó. */
function recoger<T>(origen: Observable<Resultado<T>>) {
  const resultados: Resultado<T>[] = [];
  const estado = { completo: false };
  origen.subscribe({ next: (r) => resultados.push(r), complete: () => (estado.completo = true) });
  return { resultados, estado };
}

describe('los límites de tiempo', () => {
  it('son los acordados: 10 s para recomendar, 8 s para la página de alertas y 3 s para una sección de la ficha', () => {
    expect(LIMITE_RECOMENDAR_MS).toBe(10_000);
    expect(LIMITE_ALERTAS_PAGINA_MS).toBe(8_000);
    expect(LIMITE_SECCION_MS).toBe(3_000);
  });

  it('recomendar da más tiempo que lo que tarda el servicio en el peor caso (catálogo 3 s + IA 6 s)', () => {
    expect(LIMITE_RECOMENDAR_MS).toBeGreaterThan(3_000 + 6_000);
  });
});

describe('motivoNoDisponible', () => {
  it('pasar del límite de tiempo es "tiempo"', () => {
    expect(motivoNoDisponible(new TimeoutError())).toBe('tiempo');
  });

  it('no tener conexión es "conexion", como ErrorApi o como respuesta HTTP sin estado', () => {
    expect(motivoNoDisponible(errorApi(0))).toBe('conexion');
    expect(motivoNoDisponible(new HttpErrorResponse({ status: 0 }))).toBe('conexion');
  });

  it.each([502, 503, 504])('un %s es "servicio": el servicio no está', (estado) => {
    expect(motivoNoDisponible(errorApi(estado))).toBe('servicio');
    expect(motivoNoDisponible(new HttpErrorResponse({ status: estado }))).toBe('servicio');
  });

  it.each([400, 401, 404, 409, 500])('un %s es un error propio de la consulta, no "no disponible"', (estado) => {
    expect(motivoNoDisponible(errorApi(estado))).toBeNull();
  });

  it('un fallo del propio código no se disfraza de "el servicio no está"', () => {
    expect(motivoNoDisponible(new TypeError('x'))).toBeNull();
    expect(motivoNoDisponible('texto')).toBeNull();
    expect(motivoNoDisponible(undefined)).toBeNull();
  });
});

describe('conDegradacion', () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it('lo que llega a tiempo sale como datos', () => {
    const { resultados, estado } = recoger(of({ valor: 1 }).pipe(conDegradacion(1_000)));

    expect(resultados).toEqual([{ estado: 'ok', datos: { valor: 1 } }]);
    expect(estado.completo).toBe(true);
  });

  it('pasado el límite sin respuesta, el servicio no está disponible por "tiempo" y la espera termina', () => {
    const origen = new Subject<string>();
    const { resultados, estado } = recoger(origen.pipe(conDegradacion(3_000)));

    vi.advanceTimersByTime(2_999);
    expect(resultados).toEqual([]);

    vi.advanceTimersByTime(1);
    expect(resultados).toEqual([{ estado: 'no-disponible', motivo: 'tiempo' }]);
    expect(estado.completo).toBe(true);
  });

  it('al pasarse del límite cancela la consulta que seguía en curso', () => {
    const origen = new Subject<string>();
    recoger(origen.pipe(conDegradacion(1_000)));
    expect(origen.observed).toBe(true);

    vi.advanceTimersByTime(1_000);

    expect(origen.observed).toBe(false);
  });

  it('una respuesta que llega después del límite ya no cambia nada', () => {
    const origen = new Subject<string>();
    const { resultados } = recoger(origen.pipe(conDegradacion(1_000)));

    vi.advanceTimersByTime(1_000);
    origen.next('tarde');

    expect(resultados).toEqual([{ estado: 'no-disponible', motivo: 'tiempo' }]);
  });

  it('una respuesta justo a tiempo cuenta', () => {
    const origen = new Subject<string>();
    const { resultados } = recoger(origen.pipe(conDegradacion(1_000)));

    vi.advanceTimersByTime(999);
    origen.next('a tiempo');
    origen.complete();
    vi.advanceTimersByTime(5_000);

    expect(resultados).toEqual([{ estado: 'ok', datos: 'a tiempo' }]);
  });

  it.each([502, 503, 504])('un %s es "no disponible" por "servicio"', (estado) => {
    const { resultados } = recoger(throwError(() => errorApi(estado)).pipe(conDegradacion(1_000)));

    expect(resultados).toEqual([{ estado: 'no-disponible', motivo: 'servicio' }]);
  });

  it('la falta de conexión es "no disponible" por "conexion"', () => {
    const { resultados } = recoger(throwError(() => errorApi(0)).pipe(conDegradacion(1_000)));

    expect(resultados).toEqual([{ estado: 'no-disponible', motivo: 'conexion' }]);
  });

  it('un error propio de la consulta (un 404, un dato inválido) sigue siendo un error, con su ErrorApi', () => {
    const fallo = errorApi(404);
    const { resultados } = recoger(throwError(() => fallo).pipe(conDegradacion(1_000)));

    expect(resultados).toEqual([{ estado: 'error', error: fallo }]);
  });

  it('nunca falla: la pantalla siempre recibe un resultado', () => {
    const errores: unknown[] = [];
    throwError(() => new TypeError('x'))
      .pipe(conDegradacion(1_000))
      .subscribe({ error: (e) => errores.push(e) });

    expect(errores).toEqual([]);
  });
});
