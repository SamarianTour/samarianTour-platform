import {
  CLAVE_IDIOMA,
  IDIOMAS,
  LOCALE,
  elegirIdiomaInicial,
  establecerIdioma,
  esIdioma,
  idiomaActual,
  idiomaDelNavegador,
} from './idioma';

describe('idiomaDelNavegador', () => {
  it.each([
    ['es', 'es'],
    ['es-CO', 'es'],
    ['es-419', 'es'],
    ['es_ES', 'es'],
    ['ES-mx', 'es'],
    ['en', 'en'],
    ['en-US', 'en'],
    ['en-GB', 'en'],
    ['EN', 'en'],
  ] as const)('%s es %s', (lenguaje, esperado) => {
    expect(idiomaDelNavegador(lenguaje)).toBe(esperado);
  });

  it.each(['fr-FR', 'pt-BR', 'de', 'zh-CN', 'english', 'espanol', 'enx-US', '', null, undefined])(
    'un idioma que no es es ni en (%j) cae en español',
    (lenguaje) => {
      expect(idiomaDelNavegador(lenguaje)).toBe('es');
    },
  );
});

describe('elegirIdiomaInicial', () => {
  it('la elección guardada manda sobre el navegador', () => {
    expect(elegirIdiomaInicial('en', 'es-CO')).toBe('en');
    expect(elegirIdiomaInicial('es', 'en-US')).toBe('es');
  });

  it('sin elección guardada usa el idioma del navegador', () => {
    expect(elegirIdiomaInicial(null, 'en-US')).toBe('en');
    expect(elegirIdiomaInicial(null, 'es-CO')).toBe('es');
  });

  it('sin elección guardada y con un navegador en otro idioma, usa español', () => {
    expect(elegirIdiomaInicial(null, 'fr-FR')).toBe('es');
    expect(elegirIdiomaInicial(null, undefined)).toBe('es');
  });

  it.each(['fr', 'EN', 'english', '', 'null'])(
    'ignora un valor guardado que no es es ni en (%j)',
    (guardado) => {
      expect(elegirIdiomaInicial(guardado, 'en-US')).toBe('en');
      expect(elegirIdiomaInicial(guardado, 'es-CO')).toBe('es');
    },
  );
});

describe('esIdioma', () => {
  it('reconoce solo es y en', () => {
    expect(IDIOMAS.every(esIdioma)).toBe(true);
    expect(['fr', 'ES', '', null, undefined, 1].some(esIdioma)).toBe(false);
  });
});

describe('LOCALE', () => {
  it('cada idioma tiene su formato regional', () => {
    expect(LOCALE).toEqual({ es: 'es-CO', en: 'en-US' });
  });
});

describe('establecerIdioma', () => {
  afterEach(() => localStorage.removeItem(CLAVE_IDIOMA));

  it('cambia el idioma vigente al instante', () => {
    establecerIdioma('en');
    expect(idiomaActual()).toBe('en');

    establecerIdioma('es');
    expect(idiomaActual()).toBe('es');
  });

  it('lo recuerda para la próxima visita', () => {
    establecerIdioma('en');

    expect(localStorage.getItem(CLAVE_IDIOMA)).toBe('en');
  });

  it('con recordar: false cambia solo el idioma de esta sesión', () => {
    establecerIdioma('en', { recordar: false });

    expect(idiomaActual()).toBe('en');
    expect(localStorage.getItem(CLAVE_IDIOMA)).toBeNull();
  });

  it('si el navegador no deja guardar, el cambio igual se aplica', () => {
    const guardar = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('almacenamiento bloqueado');
    });

    expect(() => establecerIdioma('en')).not.toThrow();
    expect(idiomaActual()).toBe('en');
    guardar.mockRestore();
  });
});
