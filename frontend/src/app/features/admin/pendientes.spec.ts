import { AvisoBandeja, EventoPagoFallido } from '../../core/api/modelos-nucleo';
import { establecerIdioma } from '../../core/idioma/idioma';
import {
  ETIQUETA_DESTINO_AVISO,
  etiquetaDeEstadoDePago,
  etiquetaDeTipoDeAviso,
  ordenarAvisos,
  ordenarEventos,
  primeraLinea,
  puedeReenviar,
  tieneMasQueLaPrimeraLinea,
} from './pendientes';

function aviso(id: string, creadoEn: string, estado: AvisoBandeja['estado'] = 'RECHAZADO'): AvisoBandeja {
  return { idAviso: id, destino: 'OFERTAS', estado, intentos: 1, creadoEn };
}

function evento(id: string, recibidoEn: string): EventoPagoFallido {
  return { id, referencia: `ST-${id}`, transaccionId: `T-${id}`, estado: 'APROBADA', monto: 300000, intentos: 5, recibidoEn };
}

describe('textos de los avisos', () => {
  beforeEach(() => establecerIdioma('es', { recordar: false }));

  it('el destino tiene nombre en cada idioma', () => {
    expect(ETIQUETA_DESTINO_AVISO.OFERTAS).toBe('Ofertas');
    expect(ETIQUETA_DESTINO_AVISO.OPINIONES).toBe('Opiniones');
    establecerIdioma('en', { recordar: false });
    expect(ETIQUETA_DESTINO_AVISO.OPINIONES).toBe('Reviews');
  });

  it.each([
    ['MODERACION', 'Moderación de oferta'],
    ['MODERACION_OPINION', 'Moderación de opinión'],
    ['HABILITACION', 'Habilitación del RNT'],
    ['HABILITACION_OPINION', 'Habilitación para opinar'],
    ['SUPRESION_TURISTA', 'Supresión de datos del turista'],
    ['SUPRESION_PRESTADOR', 'Supresión de datos del prestador'],
  ])('el tipo %s se llama "%s"', (tipo, nombre) => {
    expect(etiquetaDeTipoDeAviso(tipo)).toBe(nombre);
  });

  it('un tipo que no se conoce se muestra tal cual llega', () => {
    expect(etiquetaDeTipoDeAviso('NUEVO_TIPO')).toBe('NUEVO_TIPO');
  });

  it('un aviso sin tipo se llama solo "Aviso"', () => {
    expect(etiquetaDeTipoDeAviso(undefined)).toBe('Aviso');
    expect(etiquetaDeTipoDeAviso('')).toBe('Aviso');
  });

  it('el tipo sigue el idioma', () => {
    establecerIdioma('en', { recordar: false });

    expect(etiquetaDeTipoDeAviso('SUPRESION_TURISTA')).toBe('Tourist data deletion');
    expect(etiquetaDeTipoDeAviso('HABILITACION')).toBe('RNT authorization');
  });

  it('el estado de la transacción en la pasarela se traduce si se conoce y se deja si no', () => {
    expect(etiquetaDeEstadoDePago('APROBADA')).toBe('Aprobada');
    expect(etiquetaDeEstadoDePago('rechazada')).toBe('Rechazada');
    expect(etiquetaDeEstadoDePago('PENDIENTE_DE_BANCO')).toBe('PENDIENTE_DE_BANCO');
  });
});

describe('ordenar', () => {
  it('los avisos van del más reciente al más antiguo, sin tocar la lista original', () => {
    const lista = [aviso('a', '2026-10-20T10:00:00Z'), aviso('b', '2026-10-27T10:00:00Z'), aviso('c', '2026-10-22T10:00:00Z')];
    const copia = [...lista];

    expect(ordenarAvisos(lista).map((a) => a.idAviso)).toEqual(['b', 'c', 'a']);
    expect(lista).toEqual(copia);
  });

  it('los eventos van del recibido más reciente al más antiguo, sin tocar la lista original', () => {
    const lista = [evento('a', '2026-10-20T10:00:00Z'), evento('b', '2026-10-27T10:00:00Z')];
    const copia = [...lista];

    expect(ordenarEventos(lista).map((e) => e.id)).toEqual(['b', 'a']);
    expect(lista).toEqual(copia);
  });
});

describe('puedeReenviar', () => {
  it('solo un aviso rechazado se puede reenviar', () => {
    expect(puedeReenviar(aviso('a', '2026-10-20T10:00:00Z', 'RECHAZADO'))).toBe(true);
    expect(puedeReenviar(aviso('a', '2026-10-20T10:00:00Z', 'PENDIENTE'))).toBe(false);
    expect(puedeReenviar(aviso('a', '2026-10-20T10:00:00Z', 'ENVIADO'))).toBe(false);
  });
});

describe('primeraLinea', () => {
  it('es la primera línea con texto, sin espacios en los extremos', () => {
    expect(primeraLinea('  HttpClientErrorException: 422 Unprocessable Entity  \nDetalle largo\notra línea')).toBe(
      'HttpClientErrorException: 422 Unprocessable Entity',
    );
  });

  it('salta las líneas en blanco del principio', () => {
    expect(primeraLinea('\n\n  \nSegunda\nTercera')).toBe('Segunda');
  });

  it('recorta lo muy largo con puntos suspensivos', () => {
    const larga = 'x'.repeat(300);

    expect(primeraLinea(larga, 50)).toBe(`${'x'.repeat(49)}…`);
    expect(primeraLinea(larga, 50)).toHaveLength(50);
  });

  it('un mensaje corto no se recorta', () => {
    expect(primeraLinea('corto', 50)).toBe('corto');
  });

  it.each([undefined, null, '', '   ', '\n\n'])('sin mensaje (%j) queda vacía', (texto) => {
    expect(primeraLinea(texto)).toBe('');
  });
});

describe('tieneMasQueLaPrimeraLinea', () => {
  it('una sola línea corta no tiene más que mostrar', () => {
    expect(tieneMasQueLaPrimeraLinea('Error simple')).toBe(false);
  });

  it('varias líneas, o una muy larga, se pueden ver completas', () => {
    expect(tieneMasQueLaPrimeraLinea('Error\nCausa: el catálogo no tiene la oferta')).toBe(true);
    expect(tieneMasQueLaPrimeraLinea('x'.repeat(300), 50)).toBe(true);
  });

  it('sin mensaje no hay nada que ver', () => {
    expect(tieneMasQueLaPrimeraLinea(undefined)).toBe(false);
    expect(tieneMasQueLaPrimeraLinea('  ')).toBe(false);
  });
});
