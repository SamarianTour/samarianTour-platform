import { Recomendaciones } from '../../core/api/modelos-recomendacion';
import { ErrorApi } from '../../core/errores/error-api';
import {
  BIENVENIDA,
  ConsultaEnviada,
  Mensaje,
  contenidoDeResultado,
  hayBusqueda,
  reemplazar,
  vistaDeRespuesta,
} from './conversacion';

const CONSULTA: ConsultaEnviada = { texto: 'aves' };

const oferta = (id: string) => ({
  posicion: 1,
  oferta: { id, nombre: id, tipo: 'ACTIVIDAD' as const, precioReferencia: 1, unidadPrecio: 'por persona' as const },
  criterios: [],
  explicacion: 'x',
});

describe('contenidoDeResultado', () => {
  it('con datos son las recomendaciones', () => {
    const datos: Recomendaciones = { motor: 'REGLAS', recomendaciones: [oferta('a')] };

    expect(contenidoDeResultado(CONSULTA, { estado: 'ok', datos })).toEqual({ tipo: 'resultado', consulta: CONSULTA, resultado: datos });
  });

  it('si el servicio no está disponible, lo dice con el motivo y guarda la consulta para reintentar', () => {
    expect(contenidoDeResultado(CONSULTA, { estado: 'no-disponible', motivo: 'tiempo' })).toEqual({
      tipo: 'no-disponible',
      consulta: CONSULTA,
      motivo: 'tiempo',
    });
  });

  it('un error propio de la consulta es un error', () => {
    const error = new ErrorApi(400, 'DATO_INVALIDO', 'errores.codigo.DATO_INVALIDO');

    expect(contenidoDeResultado(CONSULTA, { estado: 'error', error })).toEqual({ tipo: 'error', consulta: CONSULTA, error });
  });
});

describe('reemplazar', () => {
  const mensajes: Mensaje[] = [
    BIENVENIDA,
    { id: 1, autor: 'turista', consulta: CONSULTA },
    { id: 2, autor: 'asistente', contenido: { tipo: 'buscando', consulta: CONSULTA } },
  ];

  it('cambia lo que dice el mensaje del asistente con ese id y deja los demás', () => {
    const nuevos = reemplazar(mensajes, 2, { tipo: 'no-disponible', consulta: CONSULTA, motivo: 'servicio' });

    expect(nuevos[2]).toEqual({ id: 2, autor: 'asistente', contenido: { tipo: 'no-disponible', consulta: CONSULTA, motivo: 'servicio' } });
    expect(nuevos[0]).toBe(mensajes[0]);
    expect(nuevos[1]).toBe(mensajes[1]);
  });

  it('no cambia el mensaje del turista aunque tenga ese id', () => {
    const nuevos = reemplazar(mensajes, 1, { tipo: 'bienvenida' });

    expect(nuevos[1]).toBe(mensajes[1]);
  });

  it('no modifica la lista original', () => {
    reemplazar(mensajes, 2, { tipo: 'bienvenida' });

    expect((mensajes[2] as { contenido: { tipo: string } }).contenido.tipo).toBe('buscando');
  });
});

describe('hayBusqueda', () => {
  it('es verdadero mientras algún mensaje está buscando', () => {
    expect(hayBusqueda([BIENVENIDA, { id: 1, autor: 'asistente', contenido: { tipo: 'buscando', consulta: CONSULTA } }])).toBe(true);
  });

  it('es falso con la bienvenida, las respuestas y los avisos', () => {
    expect(hayBusqueda([BIENVENIDA])).toBe(false);
    expect(
      hayBusqueda([
        { id: 1, autor: 'turista', consulta: CONSULTA },
        { id: 2, autor: 'asistente', contenido: { tipo: 'no-disponible', consulta: CONSULTA, motivo: 'tiempo' } },
      ]),
    ).toBe(false);
  });
});

describe('vistaDeRespuesta', () => {
  it('sin ofertas es "ninguna", con o sin aviso', () => {
    expect(vistaDeRespuesta({ motor: 'REGLAS', recomendaciones: [] })).toBe('ninguna');
    expect(vistaDeRespuesta({ motor: 'REGLAS', aviso: 'POCAS_COINCIDENCIAS', recomendaciones: [] })).toBe('ninguna');
  });

  it('con el aviso de pocas coincidencias y 1 o 2 ofertas es "pocas"', () => {
    expect(vistaDeRespuesta({ motor: 'IA', aviso: 'POCAS_COINCIDENCIAS', recomendaciones: [oferta('a'), oferta('b')] })).toBe('pocas');
  });

  it('de 3 a 5 ofertas es "completa"', () => {
    expect(vistaDeRespuesta({ motor: 'IA', recomendaciones: [oferta('a'), oferta('b'), oferta('c')] })).toBe('completa');
  });
});
