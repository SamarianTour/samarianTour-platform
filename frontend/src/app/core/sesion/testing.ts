import { TipoPrestador } from '../api/modelos';
import { RespuestaSesion, Rol } from '../api/modelos-nucleo';

/** Ayudas solo para las pruebas: arman tokens y respuestas de sesión con la forma que entrega el núcleo. */

function base64url(texto: string): string {
  let binario = '';
  new TextEncoder().encode(texto).forEach((byte) => (binario += String.fromCharCode(byte)));
  return btoa(binario).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

/** Un JWT con las declaraciones dadas. La firma es falsa: el frontend nunca la valida. */
export function crearToken(claims: object): string {
  return `${base64url('{"alg":"RS256","typ":"JWT"}')}.${base64url(JSON.stringify(claims))}.firma`;
}

interface Opciones {
  rol?: Rol;
  tipoPrestador?: TipoPrestador;
  /** Cambia los tokens para distinguir una renovación de la anterior. */
  numero?: number;
  ahora?: number;
}

const MINUTO = 60_000;
const DIA = 24 * 60 * MINUTO;

export function respuestaDeSesion({
  rol = 'TURISTA',
  tipoPrestador,
  numero = 1,
  ahora = Date.now(),
}: Opciones = {}): RespuestaSesion {
  const id = '70000000-0000-0000-0000-000000000007';
  return {
    tokenAcceso: crearToken({
      sub: id,
      rol,
      sid: `sesion-${numero}`,
      ...(tipoPrestador && { tipoPrestador }),
    }),
    tipoToken: 'Bearer',
    accesoExpiraEn: new Date(ahora + 15 * MINUTO).toISOString(),
    tokenRenovacion: `renovacion-${numero}`,
    renovacionExpiraEn: new Date(ahora + 7 * DIA).toISOString(),
    cuenta: {
      id,
      rol,
      nombre: 'Laura',
      correo: 'laura@demo.samariantour.co',
      whatsapp: '+573105550007',
      whatsappVerificado: true,
    },
  };
}
