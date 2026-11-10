import { esWhatsappValido, normalizarWhatsapp } from '../../cuentas/validaciones';

export type MotivoWhatsapp = 'requerido' | 'invalido';

/**
 * El WhatsApp de contacto del prestador debe ir con el indicativo del país, en formato internacional (E.164). Se acepta escrito
 * con espacios, guiones o paréntesis, como en el registro del turista.
 */
export function validarWhatsapp(texto: string): MotivoWhatsapp | null {
  if (texto.trim() === '') {
    return 'requerido';
  }
  return esWhatsappValido(texto) ? null : 'invalido';
}

/** El número como lo recibe el núcleo: sin espacios, guiones ni paréntesis. "+57 300 111-0202" → "+573001110202". */
export function numeroParaEnviar(texto: string): string {
  return normalizarWhatsapp(texto);
}
