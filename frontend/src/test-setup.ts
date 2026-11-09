import { beforeEach } from 'vitest';
import { establecerIdioma } from './app/core/idioma/idioma';

/**
 * Antes de cada prueba el idioma vuelve a ser el español. Sin esto dependería del navegador del entorno de pruebas
 * (jsdom dice `en-US`) y de lo que haya dejado la prueba anterior, y los textos que comprueban las pruebas cambiarían.
 * Una prueba que necesite el inglés lo pide con `establecerIdioma('en', { recordar: false })`.
 */
beforeEach(() => {
  establecerIdioma('es', { recordar: false });
  document.documentElement.lang = 'es';
});
