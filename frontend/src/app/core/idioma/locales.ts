import { registerLocaleData } from '@angular/common';
import localeEsCo from '@angular/common/locales/es-CO';

/**
 * Los datos regionales de Colombia, que Angular no trae de fábrica (el inglés de EE. UU. sí). Se registran al importar
 * este archivo: lo hacen la aplicación y los pipes `fecha` y `precio`, que son los que formatean con ellos.
 */
registerLocaleData(localeEsCo);
