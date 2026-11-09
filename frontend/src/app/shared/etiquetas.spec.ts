import { establecerIdioma } from '../core/idioma/idioma';
import { ESTADO_OFERTA } from '../features/prestador/estado-oferta';
import {
  CALIFICACION,
  ESTADO_MODERACION,
  ESTADO_OPINION,
  MODALIDAD,
  NIVEL_ESFUERZO,
  ROL,
  TIPO_ACTIVIDAD,
  TIPO_OFERTA,
  TIPO_PRESTADOR,
  UNIDAD_PRECIO,
} from './etiquetas';

const TABLAS = {
  ROL,
  TIPO_OFERTA,
  TIPO_ACTIVIDAD,
  NIVEL_ESFUERZO,
  MODALIDAD,
  ESTADO_MODERACION,
  TIPO_PRESTADOR,
  UNIDAD_PRECIO,
  ESTADO_OPINION,
  CALIFICACION,
  ESTADO_OFERTA,
} as const;

describe('etiquetas de las enumeraciones', () => {
  it('en español dicen lo de siempre', () => {
    expect(ROL.OPERADOR_DISPONIBILIDAD).toBe('Operador de disponibilidad');
    expect(TIPO_OFERTA.ALOJAMIENTO).toBe('Alojamiento');
    expect(TIPO_ACTIVIDAD.BUCEO_SNORKEL).toBe('Buceo y snorkel');
    expect(UNIDAD_PRECIO.ALOJAMIENTO).toBe('por noche y unidad');
    expect(CALIFICACION[5]).toBe('Excelente');
    expect(ESTADO_OFERTA.PUBLICADA_CON_CAMBIOS).toBe('Publicada con cambios en revisión');
  });

  it('en inglés usan el glosario', () => {
    establecerIdioma('en', { recordar: false });

    expect(ROL.TURISTA).toBe('Tourist');
    expect(ROL.PRESTADOR).toBe('Provider');
    expect(ROL.OPERADOR_DISPONIBILIDAD).toBe('Availability operator');
    expect(ROL.GESTOR_DESTINO).toBe('Destination manager');
    expect(ROL.ADMINISTRADOR).toBe('Administrator');
    expect(TIPO_PRESTADOR.AGENCIA_OPERADORA).toBe('Tour operator agency');
    expect(TIPO_PRESTADOR.GUIA_INDEPENDIENTE).toBe('Independent guide');
    expect(TIPO_OFERTA.ACTIVIDAD).toBe('Activity');
    expect(TIPO_OFERTA.PAQUETE).toBe('Package');
    expect(TIPO_OFERTA.ALOJAMIENTO).toBe('Accommodation');
    expect(TIPO_OFERTA.RESTAURANTE).toBe('Restaurant');
    expect(UNIDAD_PRECIO.RESTAURANTE).toBe('average per person');
    expect(CALIFICACION[5]).toBe('Excellent');
  });

  it('cambian al instante cuando cambia el idioma, con la misma tabla', () => {
    expect(NIVEL_ESFUERZO.ALTA_EXIGENCIA).toBe('Alta exigencia');

    establecerIdioma('en', { recordar: false });
    expect(NIVEL_ESFUERZO.ALTA_EXIGENCIA).toBe('Demanding');

    establecerIdioma('es', { recordar: false });
    expect(NIVEL_ESFUERZO.ALTA_EXIGENCIA).toBe('Alta exigencia');
  });

  it.each(['es', 'en'] as const)('en %s ninguna etiqueta queda vacía ni es una clave', (idioma) => {
    establecerIdioma(idioma, { recordar: false });

    for (const [nombre, tabla] of Object.entries(TABLAS)) {
      const valores = Object.entries(tabla as Record<string, string>);
      expect(valores.length, nombre).toBeGreaterThan(0);
      for (const [valor, texto] of valores) {
        expect(texto.trim(), `${nombre}.${valor}`).not.toBe('');
        expect(texto, `${nombre}.${valor}`).not.toMatch(/^etiquetas\./);
      }
    }
  });
});
