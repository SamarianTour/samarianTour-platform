import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** Contraseña de 8 a 72 caracteres (contrato del núcleo, RNF-06). */
export const CONTRASENA_MINIMA = 8;
export const CONTRASENA_MAXIMA = 72;

/** Los motivos que da la API llegan en minúscula ("debe tener…"): al mostrarlos empiezan con mayúscula. */
export function mayusculaInicial(texto: string): string {
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

/** Un texto con solo espacios no cuenta como escrito. */
export const obligatorio: ValidatorFn = (control) =>
  typeof control.value === 'string' && control.value.trim() !== '' ? null : { required: true };

/**
 * Las reglas de la contraseña: largo, y al menos una letra y un número (DN-28; ver el contrato del núcleo, `contrasena`). El núcleo las vuelve a
 * aplicar; esto solo avisa antes de enviar. Un control vacío no es un error de esta regla: lo cubre `obligatorio`.
 */
export const contrasenaValida: ValidatorFn = (control) => {
  const valor = typeof control.value === 'string' ? control.value : '';
  if (valor === '') {
    return null;
  }
  if (valor.length < CONTRASENA_MINIMA || valor.length > CONTRASENA_MAXIMA) {
    return { largo: true };
  }
  return /\p{L}/u.test(valor) && /\d/.test(valor) ? null : { letraYNumero: true };
};

/** El valor del control debe ser igual al de otro control del mismo formulario, por ejemplo "Repite la contraseña". */
export function igualA(otro: string): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const referencia = control.parent?.get(otro)?.value;
    return control.value === referencia ? null : { coincide: true };
  };
}

/** Quita lo que la gente escribe para separar un número: espacios, guiones y paréntesis. "+57 300 123-4567" → "+573001234567". */
export function normalizarWhatsapp(texto: string): string {
  return texto.replace(/[\s().-]/g, '');
}

/** Formato internacional E.164: "+", el indicativo del país (sin 0) y hasta 15 dígitos en total. */
const WHATSAPP_E164 = /^\+[1-9]\d{7,14}$/;

export function esWhatsappValido(texto: string): boolean {
  return WHATSAPP_E164.test(normalizarWhatsapp(texto));
}

/** Valida el WhatsApp escrito. Un control vacío no es un error de esta regla: lo cubre `obligatorio` cuando se exige. */
export const whatsappValido: ValidatorFn = (control) => {
  const valor = typeof control.value === 'string' ? control.value : '';
  return valor.trim() === '' || esWhatsappValido(valor) ? null : { whatsapp: true };
};
