import { Foto } from '../../../core/api/modelos';
import {
  DatosArchivo,
  MAXIMO_BYTES,
  MAXIMO_FOTOS,
  agregar,
  clasificarArchivos,
  cuposLibres,
  motivoDeRechazo,
  mover,
  quitar,
} from './fotos-oferta';

function foto(id: string): Foto {
  return {
    id,
    url: `/api/catalogo/fotos/${id}/galeria`,
    urlMiniatura: `/api/catalogo/fotos/${id}/miniatura`,
  };
}

function archivo(name: string, type = 'image/jpeg', size = 1000): DatosArchivo {
  return { name, type, size };
}

const ids = (fotos: readonly Foto[]) => fotos.map((una) => una.id);

describe('motivoDeRechazo', () => {
  it.each(['image/jpeg', 'image/png', 'image/webp'])('acepta %s', (tipo) => {
    expect(motivoDeRechazo(archivo('a', tipo))).toBeNull();
  });

  it('rechaza un formato que no es JPG, PNG ni WebP', () => {
    expect(motivoDeRechazo(archivo('a.gif', 'image/gif'))).toContain('JPG, PNG o WebP');
    expect(motivoDeRechazo(archivo('a.pdf', 'application/pdf'))).toContain('JPG, PNG o WebP');
  });

  it('deja que decida la API cuando el sistema no informa el tipo', () => {
    expect(motivoDeRechazo(archivo('a.jpg', ''))).toBeNull();
  });

  it('acepta justo 5 MB y rechaza un byte más', () => {
    expect(motivoDeRechazo(archivo('a', 'image/png', MAXIMO_BYTES))).toBeNull();
    expect(motivoDeRechazo(archivo('a', 'image/png', MAXIMO_BYTES + 1))).toBe('pesa más de 5 MB');
  });

  it('rechaza un archivo vacío', () => {
    expect(motivoDeRechazo(archivo('a', 'image/png', 0))).toBe('el archivo está vacío');
  });
});

describe('cuposLibres', () => {
  it('descuenta las fotos que ya tiene la oferta', () => {
    expect(cuposLibres(0)).toBe(MAXIMO_FOTOS);
    expect(cuposLibres(4)).toBe(2);
  });

  it('nunca es negativo', () => {
    expect(cuposLibres(MAXIMO_FOTOS)).toBe(0);
    expect(cuposLibres(MAXIMO_FOTOS + 2)).toBe(0);
  });
});

describe('clasificarArchivos', () => {
  it('acepta los archivos válidos en el orden elegido', () => {
    const resultado = clasificarArchivos([archivo('uno'), archivo('dos')], 6);

    expect(resultado.aceptados.map((a) => a.name)).toEqual(['uno', 'dos']);
    expect(resultado.avisos).toEqual([]);
  });

  it('avisa por cada archivo inválido, con su nombre y el motivo, y sube los demás', () => {
    const resultado = clasificarArchivos(
      [
        archivo('bien'),
        archivo('animada.gif', 'image/gif'),
        archivo('grande', 'image/png', MAXIMO_BYTES + 1),
      ],
      6,
    );

    expect(resultado.aceptados.map((a) => a.name)).toEqual(['bien']);
    expect(resultado.avisos).toEqual([
      'No se subió «animada.gif»: solo se aceptan fotos JPG, PNG o WebP.',
      'No se subió «grande»: pesa más de 5 MB.',
    ]);
  });

  it('deja fuera los últimos archivos cuando no caben todos', () => {
    const resultado = clasificarArchivos([archivo('a'), archivo('b'), archivo('c')], 2);

    expect(resultado.aceptados.map((a) => a.name)).toEqual(['a', 'b']);
    expect(resultado.avisos).toEqual(['No se subió «c»: una oferta lleva máximo 6 fotos.']);
  });

  it('no sube nada si la oferta ya está llena', () => {
    const resultado = clasificarArchivos([archivo('a')], 0);

    expect(resultado.aceptados).toEqual([]);
    expect(resultado.avisos).toHaveLength(1);
  });

  it('un archivo inválido no gasta cupo', () => {
    const resultado = clasificarArchivos([archivo('mal', 'image/gif'), archivo('bien')], 1);

    expect(resultado.aceptados.map((a) => a.name)).toEqual(['bien']);
  });
});

describe('agregar', () => {
  it('pone la foto nueva al final', () => {
    expect(ids(agregar([foto('a')], foto('b')))).toEqual(['a', 'b']);
  });

  it('no repite una foto que ya está', () => {
    expect(ids(agregar([foto('a'), foto('b')], foto('a')))).toEqual(['a', 'b']);
  });

  it('no pasa del máximo de fotos', () => {
    const llena = ['a', 'b', 'c', 'd', 'e', 'f'].map(foto);

    expect(ids(agregar(llena, foto('g')))).toEqual(['a', 'b', 'c', 'd', 'e', 'f']);
  });

  it('no modifica la lista original', () => {
    const original = [foto('a')];

    agregar(original, foto('b'));

    expect(ids(original)).toEqual(['a']);
  });
});

describe('quitar', () => {
  it('saca la foto indicada y respeta el orden de las demás', () => {
    expect(ids(quitar([foto('a'), foto('b'), foto('c')], 'b'))).toEqual(['a', 'c']);
  });

  it('si la foto no está, deja la lista igual', () => {
    expect(ids(quitar([foto('a')], 'z'))).toEqual(['a']);
  });

  it('al quitar la portada, la siguiente pasa a ser la primera', () => {
    expect(ids(quitar([foto('a'), foto('b')], 'a'))[0]).toBe('b');
  });
});

describe('mover', () => {
  const lista = () => [foto('a'), foto('b'), foto('c')];

  it('adelanta una foto un lugar', () => {
    expect(ids(mover(lista(), 'c', -1))).toEqual(['a', 'c', 'b']);
  });

  it('atrasa una foto un lugar', () => {
    expect(ids(mover(lista(), 'a', 1))).toEqual(['b', 'a', 'c']);
  });

  it('adelantar la segunda foto la convierte en la portada', () => {
    expect(ids(mover(lista(), 'b', -1))[0]).toBe('b');
  });

  it('no cambia nada en los extremos', () => {
    expect(ids(mover(lista(), 'a', -1))).toEqual(['a', 'b', 'c']);
    expect(ids(mover(lista(), 'c', 1))).toEqual(['a', 'b', 'c']);
  });

  it('ignora una foto que no está y no modifica la lista original', () => {
    const original = lista();

    expect(ids(mover(original, 'z', 1))).toEqual(['a', 'b', 'c']);
    mover(original, 'a', 1);
    expect(ids(original)).toEqual(['a', 'b', 'c']);
  });
});
