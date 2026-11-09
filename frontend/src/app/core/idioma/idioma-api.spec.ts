import { establecerIdioma } from './idioma';
import { conIdioma, parametrosConIdioma } from './idioma-api';

describe('idioma en las peticiones de recomendación', () => {
  it('conIdioma agrega el idioma vigente al cuerpo de la consulta', () => {
    expect(conIdioma({ texto: 'buceo' })).toEqual({ texto: 'buceo', idioma: 'es' });

    establecerIdioma('en', { recordar: false });

    expect(conIdioma({ texto: 'diving' })).toEqual({ texto: 'diving', idioma: 'en' });
  });

  it('conIdioma respeta un idioma que la consulta ya traiga', () => {
    establecerIdioma('en', { recordar: false });

    expect(conIdioma({ texto: 'buceo', idioma: 'es' }).idioma).toBe('es');
  });

  it('conIdioma no modifica la consulta original', () => {
    const consulta = { texto: 'buceo' };

    conIdioma(consulta);

    expect(consulta).toEqual({ texto: 'buceo' });
  });

  it('parametrosConIdioma entrega ?idioma= para las alertas y las alternativas', () => {
    expect(parametrosConIdioma()).toEqual({ idioma: 'es' });

    establecerIdioma('en', { recordar: false });

    expect(parametrosConIdioma({ zonaId: 'a01' })).toEqual({ zonaId: 'a01', idioma: 'en' });
  });
});
