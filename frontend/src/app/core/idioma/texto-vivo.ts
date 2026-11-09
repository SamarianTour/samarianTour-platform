import type { Parametros } from './traducir';
import type { ClaveTexto } from './textos';
import { traducir } from './traducir';

/**
 * Un texto que se arma al mostrarlo, no al guardarlo. Las pantallas guardan en sus `signal` de mensajes (errores, avisos
 * de campo) un `TextoVivo` y la plantilla lo llama (`{{ mensaje() }}`): como se lee el idioma en ese momento, un mensaje que
 * ya está en pantalla cambia de idioma junto con la interfaz.
 */
export type TextoVivo = () => string;

/** El texto de una clave, para mostrarlo en el idioma vigente cada vez que se lea. */
export function textoVivo(clave: ClaveTexto, parametros?: Parametros): TextoVivo {
  return () => traducir(clave, parametros);
}
