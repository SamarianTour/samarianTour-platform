import {
  FiltrosRecomendacion,
  MAXIMO_TEXTO,
  SIN_FILTROS,
  construirConsulta,
  hayFiltros,
  validarConsulta,
} from './consulta';

const con = (parte: Partial<FiltrosRecomendacion>): FiltrosRecomendacion => ({ ...SIN_FILTROS, ...parte });

describe('hayFiltros', () => {
  it('sin nada elegido no hay filtros', () => {
    expect(hayFiltros(SIN_FILTROS)).toBe(false);
  });

  it.each([
    ['un tipo de oferta', con({ tiposOferta: ['ACTIVIDAD'] })],
    ['un tipo de actividad', con({ tiposActividad: ['SENDERISMO'] })],
    ['una zona', con({ zonaId: 'z1' })],
    ['accesibilidad', con({ accesibleSillaRuedas: true })],
    ['un esfuerzo máximo', con({ nivelEsfuerzoMaximo: 'BAJO' })],
    ['un precio máximo', con({ precioMaximo: 100000 })],
  ])('hay filtros con %s', (_nombre, filtros) => {
    expect(hayFiltros(filtros)).toBe(true);
  });
});

describe('validarConsulta', () => {
  it('con texto sirve', () => {
    expect(validarConsulta('quiero ver aves', SIN_FILTROS)).toBeNull();
  });

  it('con solo un filtro, sin texto, sirve', () => {
    expect(validarConsulta('', con({ zonaId: 'z1' }))).toBeNull();
    expect(validarConsulta('   ', con({ accesibleSillaRuedas: true }))).toBeNull();
  });

  it('sin texto ni filtros está vacía', () => {
    expect(validarConsulta('', SIN_FILTROS)).toBe('vacia');
    expect(validarConsulta('   \n ', SIN_FILTROS)).toBe('vacia');
  });

  it('el texto llega a 500 caracteres sin contar los espacios de los extremos', () => {
    expect(validarConsulta('a'.repeat(MAXIMO_TEXTO), SIN_FILTROS)).toBeNull();
    expect(validarConsulta(`  ${'a'.repeat(MAXIMO_TEXTO)}  `, SIN_FILTROS)).toBeNull();
    expect(validarConsulta('a'.repeat(MAXIMO_TEXTO + 1), SIN_FILTROS)).toBe('textoLargo');
  });

  it('el precio máximo debe ser mayor que cero', () => {
    expect(validarConsulta('aves', con({ precioMaximo: 0 }))).toBe('precioInvalido');
    expect(validarConsulta('aves', con({ precioMaximo: -5 }))).toBe('precioInvalido');
    expect(validarConsulta('aves', con({ precioMaximo: Number.NaN }))).toBe('precioInvalido');
    expect(validarConsulta('aves', con({ precioMaximo: 1 }))).toBeNull();
  });
});

describe('construirConsulta', () => {
  it('solo manda el texto, limpio, si no hay filtros', () => {
    expect(construirConsulta('  quiero ver aves  ', SIN_FILTROS)).toEqual({ texto: 'quiero ver aves' });
  });

  it('no manda lo que está vacío ni los filtros apagados', () => {
    const consulta = construirConsulta('', con({ zonaId: 'z1' }));

    expect(consulta).toEqual({ zonaId: 'z1' });
    expect('texto' in consulta).toBe(false);
    expect('accesibleSillaRuedas' in consulta).toBe(false);
    expect('tiposOferta' in consulta).toBe(false);
  });

  it('manda todos los filtros elegidos', () => {
    const consulta = construirConsulta(
      'aves',
      con({
        tiposOferta: ['ACTIVIDAD', 'PAQUETE'],
        tiposActividad: ['AVISTAMIENTO_FAUNA'],
        zonaId: 'minca',
        accesibleSillaRuedas: true,
        nivelEsfuerzoMaximo: 'MODERADO',
        precioMaximo: 150000,
      }),
    );

    expect(consulta).toEqual({
      texto: 'aves',
      tiposOferta: ['ACTIVIDAD', 'PAQUETE'],
      tiposActividad: ['AVISTAMIENTO_FAUNA'],
      zonaId: 'minca',
      accesibleSillaRuedas: true,
      nivelEsfuerzoMaximo: 'MODERADO',
      precioMaximo: 150000,
    });
  });

  it('no manda el idioma: lo agrega el servicio', () => {
    expect('idioma' in construirConsulta('aves', SIN_FILTROS)).toBe(false);
  });

  it('copia las listas: cambiar los filtros después no cambia la consulta enviada', () => {
    const tipos: ('ACTIVIDAD' | 'PAQUETE')[] = ['ACTIVIDAD'];
    const consulta = construirConsulta('aves', con({ tiposOferta: tipos }));

    tipos.push('PAQUETE');

    expect(consulta.tiposOferta).toEqual(['ACTIVIDAD']);
  });
});
