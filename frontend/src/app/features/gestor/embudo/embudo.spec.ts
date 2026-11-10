import { Embudo, EtapaEmbudo } from '../../../core/api/modelos-nucleo';
import { ANCHO_MENOS_DE_TRES } from '../demanda/demanda';
import {
  ETAPAS,
  anchoDeEtapa,
  anchoDePorcentaje,
  cifrasDeEmbudo,
  conversion,
  embudoVacio,
  embudosPorTipo,
  etapasCalculadas,
} from './embudo';

const ETAPAS_DEL_NUCLEO: EtapaEmbudo[] = [
  { etapa: 'ENVIADAS', solicitudes: 42, porcentaje: 100 },
  { etapa: 'ATENDIDAS', solicitudes: 30, porcentaje: 71.4 },
  { etapa: 'CONFIRMADAS', solicitudes: 21, porcentaje: 50 },
  { etapa: 'PAGADAS', solicitudes: 17, porcentaje: 40.5 },
];

describe('conversion', () => {
  it('es qué parte de la etapa anterior llegó a esta, con un decimal', () => {
    expect(conversion(30, 42)).toBe(71.4);
    expect(conversion(21, 30)).toBe(70);
    expect(conversion(17, 21)).toBe(81);
    expect(conversion(1000, 3000)).toBe(33.3);
  });

  it('un cero entre cifras conocidas sí es 0 %', () => {
    expect(conversion(0, 10)).toBe(0);
  });

  it('si alguna cantidad falta no se inventa nada', () => {
    expect(conversion(null, 10)).toBeNull();
    expect(conversion(10, null)).toBeNull();
    expect(conversion(null, null)).toBeNull();
  });

  it('dividir entre cero no da un número', () => {
    expect(conversion(0, 0)).toBeNull();
    expect(conversion(5, 0)).toBeNull();
  });
});

describe('etapasCalculadas', () => {
  it('trae las cuatro etapas en orden con su conversión desde la anterior', () => {
    const etapas = etapasCalculadas(ETAPAS_DEL_NUCLEO);

    expect(etapas.map((e) => e.etapa)).toEqual([...ETAPAS]);
    expect(etapas.map((e) => e.solicitudes)).toEqual([42, 30, 21, 17]);
    expect(etapas.map((e) => e.porcentaje)).toEqual([100, 71.4, 50, 40.5]);
    expect(etapas.map((e) => e.deLaAnterior)).toEqual([null, 71.4, 70, 81]);
  });

  it('el porcentaje sobre las enviadas es el del núcleo, no se recalcula', () => {
    // 17 de 42 es 40,476…; el núcleo dice 40,5 y eso se muestra.
    expect(etapasCalculadas(ETAPAS_DEL_NUCLEO)[3].porcentaje).toBe(40.5);
  });

  it('reordena si el núcleo manda las etapas desordenadas', () => {
    const etapas = etapasCalculadas([...ETAPAS_DEL_NUCLEO].reverse());

    expect(etapas.map((e) => e.etapa)).toEqual([...ETAPAS]);
    expect(etapas.map((e) => e.solicitudes)).toEqual([42, 30, 21, 17]);
  });

  it('un conteo omitido queda en null, con su porcentaje, y no se estima la conversión a su alrededor', () => {
    const etapas = etapasCalculadas([
      { etapa: 'ENVIADAS', solicitudes: 42, porcentaje: 100 },
      { etapa: 'ATENDIDAS', solicitudes: 30, porcentaje: 71.4 },
      { etapa: 'CONFIRMADAS' },
      { etapa: 'PAGADAS', solicitudes: 0, porcentaje: 0 },
    ]);

    expect(etapas[2]).toEqual({ etapa: 'CONFIRMADAS', solicitudes: null, porcentaje: null, deLaAnterior: null });
    // La que sigue a una omitida tampoco se puede calcular.
    expect(etapas[3].deLaAnterior).toBeNull();
    expect(etapas[3].solicitudes).toBe(0);
  });

  it('una etapa que el núcleo no manda sale vacía, no desaparece', () => {
    const etapas = etapasCalculadas([{ etapa: 'ENVIADAS', solicitudes: 10, porcentaje: 100 }]);

    expect(etapas).toHaveLength(4);
    expect(etapas[1]).toEqual({ etapa: 'ATENDIDAS', solicitudes: null, porcentaje: null, deLaAnterior: null });
  });

  it('con 0 enviadas todo es cero y no hay conversión que calcular', () => {
    const etapas = etapasCalculadas(ETAPAS.map((etapa) => ({ etapa, solicitudes: 0 })));

    expect(etapas.map((e) => e.deLaAnterior)).toEqual([null, null, null, null]);
    expect(embudoVacio(etapas)).toBe(true);
  });
});

describe('anchoDeEtapa', () => {
  const etapa = (solicitudes: number | null, porcentaje: number | null) => ({
    etapa: 'ATENDIDAS' as const,
    solicitudes,
    porcentaje,
    deLaAnterior: null,
  });

  it('es el porcentaje sobre las enviadas', () => {
    expect(anchoDeEtapa(etapa(42, 100))).toBe(100);
    expect(anchoDeEtapa(etapa(30, 71.4))).toBe(71);
  });

  it('una etapa con pocas solicitudes se sigue viendo', () => {
    expect(anchoDeEtapa(etapa(3, 0.4))).toBe(3);
  });

  it('un cero no tiene barra', () => {
    expect(anchoDeEtapa(etapa(0, 0))).toBe(0);
  });

  it('un conteo omitido lleva la marca rayada', () => {
    expect(anchoDeEtapa(etapa(null, null))).toBe(ANCHO_MENOS_DE_TRES);
  });
});

describe('anchoDePorcentaje', () => {
  it('es el porcentaje redondeado, de 0 a 100', () => {
    expect(anchoDePorcentaje(10, 59.5)).toBe(60);
    expect(anchoDePorcentaje(10, 26.2)).toBe(26);
    expect(anchoDePorcentaje(10, 100)).toBe(100);
  });

  it('nunca pasa de 100', () => {
    expect(anchoDePorcentaje(10, 130)).toBe(100);
  });

  it('una cantidad con muy poco porcentaje se sigue viendo', () => {
    expect(anchoDePorcentaje(3, 0.2)).toBe(3);
  });

  it('un cero o un porcentaje que no existe no tienen barra', () => {
    expect(anchoDePorcentaje(0, 0)).toBe(0);
    expect(anchoDePorcentaje(5, null)).toBe(0);
  });

  it('una cantidad omitida lleva la marca rayada, tenga o no porcentaje', () => {
    expect(anchoDePorcentaje(null, null)).toBe(ANCHO_MENOS_DE_TRES);
    expect(anchoDePorcentaje(null, 40)).toBe(ANCHO_MENOS_DE_TRES);
  });
});

describe('cifrasDeEmbudo', () => {
  it('toma las enviadas, las pagadas y el porcentaje de pago de las etapas', () => {
    expect(cifrasDeEmbudo(etapasCalculadas(ETAPAS_DEL_NUCLEO))).toEqual({
      enviadas: 42,
      pagadas: 17,
      conversionTotal: 40.5,
    });
  });

  it('con las pagadas omitidas no hay porcentaje de pago', () => {
    const cifras = cifrasDeEmbudo(
      etapasCalculadas([
        { etapa: 'ENVIADAS', solicitudes: 42, porcentaje: 100 },
        { etapa: 'PAGADAS' },
      ]),
    );

    expect(cifras).toEqual({ enviadas: 42, pagadas: null, conversionTotal: null });
  });
});

describe('embudoVacio', () => {
  it('es vacío solo si hay cero enviadas, no si el conteo fue omitido', () => {
    expect(embudoVacio(etapasCalculadas([{ etapa: 'ENVIADAS', solicitudes: 0 }]))).toBe(true);
    expect(embudoVacio(etapasCalculadas([{ etapa: 'ENVIADAS' }]))).toBe(false);
    expect(embudoVacio(etapasCalculadas(ETAPAS_DEL_NUCLEO))).toBe(false);
  });
});

describe('embudosPorTipo', () => {
  it('calcula las etapas de cada tipo, en el orden del núcleo', () => {
    const embudo: Embudo = {
      desde: '2026-10-01',
      hasta: '2026-10-31',
      etapas: ETAPAS_DEL_NUCLEO,
      porTipo: [
        {
          tipoOferta: 'RESTAURANTE',
          etapas: [
            { etapa: 'ENVIADAS', solicitudes: 4, porcentaje: 100 },
            { etapa: 'ATENDIDAS', solicitudes: 3, porcentaje: 75 },
            { etapa: 'CONFIRMADAS' },
            { etapa: 'PAGADAS', solicitudes: 0, porcentaje: 0 },
          ],
        },
        { tipoOferta: 'ACTIVIDAD', etapas: ETAPAS_DEL_NUCLEO },
      ],
    };

    const tipos = embudosPorTipo(embudo);

    expect(tipos.map((t) => t.tipoOferta)).toEqual(['RESTAURANTE', 'ACTIVIDAD']);
    expect(tipos[0].etapas.map((e) => e.solicitudes)).toEqual([4, 3, null, 0]);
    expect(tipos[0].etapas[1].deLaAnterior).toBe(75);
    expect(tipos[1].etapas[3].deLaAnterior).toBe(81);
  });
});
