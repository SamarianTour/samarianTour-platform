import { numeroParaEnviar, validarWhatsapp } from './whatsapp';

describe('el WhatsApp de contacto', () => {
  it('es obligatorio', () => {
    expect(validarWhatsapp('')).toBe('requerido');
    expect(validarWhatsapp('   ')).toBe('requerido');
  });

  it('lleva el indicativo del país', () => {
    expect(validarWhatsapp('+573001110202')).toBeNull();
    expect(validarWhatsapp('3001110202')).toBe('invalido');
    expect(validarWhatsapp('573001110202')).toBe('invalido');
    expect(validarWhatsapp('+0573001110202')).toBe('invalido');
    expect(validarWhatsapp('+57300')).toBe('invalido');
  });

  it('se acepta escrito con espacios, guiones y paréntesis', () => {
    expect(validarWhatsapp('+57 300 111-0202')).toBeNull();
    expect(validarWhatsapp('+57 (300) 111 0202')).toBeNull();
  });

  it('se envía sin separadores', () => {
    expect(numeroParaEnviar(' +57 300 111-0202 ')).toBe('+573001110202');
    expect(numeroParaEnviar('+57 (300) 111 0202')).toBe('+573001110202');
  });
});
