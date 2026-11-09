import { FormControl, FormGroup } from '@angular/forms';
import {
  contrasenaValida,
  esWhatsappValido,
  igualA,
  normalizarWhatsapp,
  obligatorio,
  whatsappValido,
} from './validaciones';

const control = (valor: unknown) => new FormControl(valor);

describe('obligatorio', () => {
  it.each(['', '   ', null])('rechaza %j', (valor) => {
    expect(obligatorio(control(valor))).toEqual({ required: true });
  });

  it('acepta un texto con contenido', () => {
    expect(obligatorio(control(' Laura '))).toBeNull();
  });
});

describe('contrasenaValida', () => {
  it.each(['Playa2026', 'abcdefg1', '12345678a', 'Ñandú2026', 'a'.repeat(71) + '1'])(
    'acepta %s',
    (clave) => {
      expect(contrasenaValida(control(clave))).toBeNull();
    },
  );

  it.each(['Playa20', 'a1', '1234567'])('rechaza por corta: %s', (clave) => {
    expect(contrasenaValida(control(clave))).toEqual({ largo: true });
  });

  it('rechaza por larga a partir de 73 caracteres', () => {
    expect(contrasenaValida(control('a1'.repeat(37)))).toEqual({ largo: true });
    expect(contrasenaValida(control('a'.repeat(71) + '1'))).toBeNull();
  });

  it('rechaza una contraseña sin números', () => {
    expect(contrasenaValida(control('SoloLetras'))).toEqual({ letraYNumero: true });
  });

  it('rechaza una contraseña sin letras', () => {
    expect(contrasenaValida(control('12345678'))).toEqual({ letraYNumero: true });
    expect(contrasenaValida(control('1234 5678 !'))).toEqual({ letraYNumero: true });
  });

  it('un valor vacío no es un error de esta regla', () => {
    expect(contrasenaValida(control(''))).toBeNull();
    expect(contrasenaValida(control(null))).toBeNull();
  });
});

describe('igualA', () => {
  function formulario(contrasena: string, repetir: string) {
    const grupo = new FormGroup({
      contrasena: new FormControl(contrasena),
      repetir: new FormControl(repetir, igualA('contrasena')),
    });
    // El control se evalúa al crearlo, cuando todavía no tiene formulario padre: se evalúa de nuevo ya dentro.
    grupo.controls.repetir.updateValueAndValidity();
    return grupo;
  }

  it('acepta cuando los dos valores son iguales', () => {
    expect(formulario('Playa2026', 'Playa2026').controls.repetir.errors).toBeNull();
  });

  it('rechaza cuando son distintos, incluso por una letra en mayúscula', () => {
    expect(formulario('Playa2026', 'playa2026').controls.repetir.errors).toEqual({
      coincide: true,
    });
  });

  it('se vuelve a evaluar al corregir la contraseña', () => {
    const grupo = formulario('Playa2026', 'Playa2027');
    expect(grupo.controls.repetir.invalid).toBe(true);

    grupo.controls.contrasena.setValue('Playa2027');
    grupo.controls.repetir.updateValueAndValidity();

    expect(grupo.controls.repetir.valid).toBe(true);
  });

  it('sin formulario padre no encuentra con qué comparar', () => {
    expect(igualA('otro')(control('x'))).toEqual({ coincide: true });
  });
});

describe('WhatsApp', () => {
  it.each([
    ['+57 300 123 4567', '+573001234567'],
    ['+57-300-123-4567', '+573001234567'],
    ['+57 (300) 123.4567', '+573001234567'],
    ['  +14155552671 ', '+14155552671'],
  ])('normaliza %j a %j', (escrito, esperado) => {
    expect(normalizarWhatsapp(escrito.trim())).toBe(esperado);
  });

  it.each(['+573001234567', '+57 300 123 4567', '+14155552671', '+442071838750'])(
    'acepta el número %s',
    (numero) => {
      expect(esWhatsappValido(numero)).toBe(true);
      expect(whatsappValido(control(numero))).toBeNull();
    },
  );

  it.each([
    ['sin el signo +', '573001234567'],
    ['solo el número local', '3001234567'],
    ['con letras', '+57300abc4567'],
    ['demasiado corto', '+5730012'],
    ['demasiado largo', '+5730012345678901'],
    ['indicativo que empieza en 0', '+0573001234567'],
    ['con un + en medio', '+57300+1234567'],
  ])('rechaza un número %s: %s', (_motivo, numero) => {
    expect(esWhatsappValido(numero)).toBe(false);
    expect(whatsappValido(control(numero))).toEqual({ whatsapp: true });
  });

  it('un campo vacío no es un error de formato', () => {
    expect(whatsappValido(control(''))).toBeNull();
    expect(whatsappValido(control('   '))).toBeNull();
  });
});
