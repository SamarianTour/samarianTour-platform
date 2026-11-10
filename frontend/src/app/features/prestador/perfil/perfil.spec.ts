import { PerfilPublico } from '../../../core/api/modelos';
import {
  VALORES_PERFIL_VACIOS,
  ValoresPerfil,
  construirPerfil,
  esUrlWeb,
  lineas,
  validarPerfil,
  valoresDePerfil,
} from './perfil';

const COMPLETO: ValoresPerfil = {
  nombreComercial: ' Mar y Selva Expediciones ',
  descripcion: ' Agencia de caminatas y kayak ',
  horariosAtencion: ' Lunes a sábado ',
  sitioWeb: ' https://marayselva.co ',
  redesSociales: 'https://instagram.com/marayselva\n\n  https://facebook.com/marayselva  \n',
};

describe('las direcciones web', () => {
  it.each(['https://marayselva.co', 'http://localhost:4200/x', 'https://instagram.com/mar_y_selva?a=1'])(
    '%s es válida',
    (url) => expect(esUrlWeb(url)).toBe(true),
  );

  it.each(['marayselva.co', 'www.marayselva.co', 'ftp://marayselva.co', 'javascript:alert(1)', 'https://', '', 'hola mundo'])(
    '"%s" no es válida',
    (url) => expect(esUrlWeb(url)).toBe(false),
  );
});

describe('las líneas', () => {
  it('se parten, se limpian y no dejan líneas vacías', () => {
    expect(lineas(' a \r\n\n b\n')).toEqual(['a', 'b']);
    expect(lineas('')).toEqual([]);
  });
});

describe('la validación del perfil', () => {
  it('solo la descripción es obligatoria', () => {
    expect(validarPerfil(VALORES_PERFIL_VACIOS)).toEqual({ descripcion: 'descripcionRequerida' });
    expect(validarPerfil({ ...VALORES_PERFIL_VACIOS, descripcion: 'Hola' })).toEqual({});
    expect(validarPerfil({ ...VALORES_PERFIL_VACIOS, descripcion: '   ' })).toEqual({ descripcion: 'descripcionRequerida' });
  });

  it('el perfil completo es válido', () => {
    expect(validarPerfil(COMPLETO)).toEqual({});
  });

  it('un sitio web escrito sin http o https se rechaza', () => {
    expect(validarPerfil({ ...COMPLETO, sitioWeb: 'marayselva.co' })).toEqual({ sitioWeb: 'sitioInvalido' });
  });

  it('una sola red inválida rechaza todas, con un solo motivo', () => {
    expect(validarPerfil({ ...COMPLETO, redesSociales: 'https://instagram.com/x\n@marayselva' })).toEqual({
      redesSociales: 'redesInvalidas',
    });
  });
});

describe('la petición', () => {
  it('manda los textos sin espacios sobrantes y cada red en su lugar', () => {
    expect(construirPerfil(COMPLETO)).toEqual({
      nombreComercial: 'Mar y Selva Expediciones',
      descripcion: 'Agencia de caminatas y kayak',
      horariosAtencion: 'Lunes a sábado',
      sitioWeb: 'https://marayselva.co',
      redesSociales: ['https://instagram.com/marayselva', 'https://facebook.com/marayselva'],
    });
  });

  it('lo que se deja en blanco no viaja (guardar reemplaza los datos)', () => {
    const cuerpo = construirPerfil({ ...VALORES_PERFIL_VACIOS, descripcion: 'Hola' });

    expect(cuerpo).toEqual({ descripcion: 'Hola', redesSociales: [] });
    expect('nombreComercial' in cuerpo).toBe(false);
    expect('sitioWeb' in cuerpo).toBe(false);
    expect('horariosAtencion' in cuerpo).toBe(false);
  });

  it('no manda fotos: se suben y se quitan aparte', () => {
    expect('fotos' in construirPerfil(COMPLETO)).toBe(false);
  });
});

describe('lo que el formulario muestra del perfil', () => {
  it('trae todos los datos, con las redes una por línea', () => {
    const perfil: PerfilPublico = {
      nombreComercial: 'Mar y Selva',
      descripcion: 'Caminatas',
      fotos: [],
      horariosAtencion: '8 a 6',
      sitioWeb: 'https://marayselva.co',
      redesSociales: ['https://a.co', 'https://b.co'],
    };

    expect(valoresDePerfil(perfil)).toEqual({
      nombreComercial: 'Mar y Selva',
      descripcion: 'Caminatas',
      horariosAtencion: '8 a 6',
      sitioWeb: 'https://marayselva.co',
      redesSociales: 'https://a.co\nhttps://b.co',
    });
  });

  it('lo que falta queda vacío', () => {
    expect(valoresDePerfil({ descripcion: 'Hola', fotos: [], redesSociales: [] })).toEqual({
      ...VALORES_PERFIL_VACIOS,
      descripcion: 'Hola',
    });
  });
});
