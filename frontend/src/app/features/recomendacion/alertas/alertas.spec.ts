import { Alerta, NivelAlerta, ResumenAlertas, ZonaConAlertas } from '../../../core/api/modelos-recomendacion';
import { nivelMaximo, totalDeAlertas, zonasPorGravedad } from './alertas';

function alerta(id: string, nivel: NivelAlerta): Alerta {
  return { id, tipo: 'HIDROLOGICA', nivel, titulo: id, fuente: 'IDEAM, alertas hidrológicas vigentes' };
}

function zona(zonaId: string, ...niveles: NivelAlerta[]): ZonaConAlertas {
  return { zonaId, alertas: niveles.map((nivel, i) => alerta(`${zonaId}-${i}`, nivel)) };
}

const resumen = (...zonas: ZonaConAlertas[]): ResumenAlertas => ({ actualizadoEn: '2026-10-24T23:42:10.512Z', zonas });

describe('lógica de las alertas', () => {
  describe('nivelMaximo', () => {
    it('es el nivel más grave de la zona', () => {
      expect(nivelMaximo(zona('z', 'AMARILLA', 'ROJA', 'NARANJA'))).toBe('ROJA');
      expect(nivelMaximo(zona('z', 'AMARILLA', 'NARANJA'))).toBe('NARANJA');
    });

    it('con una sola alerta es el de esa alerta', () => {
      expect(nivelMaximo(zona('z', 'AMARILLA'))).toBe('AMARILLA');
      expect(nivelMaximo(zona('z', 'ROJA'))).toBe('ROJA');
    });
  });

  describe('zonasPorGravedad', () => {
    it('pone arriba las zonas con el nivel más grave', () => {
      const ordenadas = zonasPorGravedad(
        resumen(zona('a', 'AMARILLA'), zona('b', 'ROJA'), zona('c', 'NARANJA')),
      );

      expect(ordenadas.map((z) => z.zonaId)).toEqual(['b', 'c', 'a']);
    });

    it('a igual gravedad conserva el orden del servicio', () => {
      const ordenadas = zonasPorGravedad(
        resumen(zona('a', 'NARANJA'), zona('b', 'AMARILLA'), zona('c', 'NARANJA'), zona('d', 'AMARILLA')),
      );

      expect(ordenadas.map((z) => z.zonaId)).toEqual(['a', 'c', 'b', 'd']);
    });

    it('no modifica el resumen recibido', () => {
      const original = resumen(zona('a', 'AMARILLA'), zona('b', 'ROJA'));

      zonasPorGravedad(original);

      expect(original.zonas.map((z) => z.zonaId)).toEqual(['a', 'b']);
    });

    it('sin zonas devuelve una lista vacía', () => {
      expect(zonasPorGravedad(resumen())).toEqual([]);
    });
  });

  describe('totalDeAlertas', () => {
    it('suma las alertas de todas las zonas', () => {
      expect(totalDeAlertas(resumen(zona('a', 'AMARILLA', 'ROJA'), zona('b', 'NARANJA')))).toBe(3);
    });

    it('sin zonas es cero', () => {
      expect(totalDeAlertas(resumen())).toBe(0);
    });
  });
});
