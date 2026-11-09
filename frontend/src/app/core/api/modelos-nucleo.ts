import type { components } from './nucleo-api';

/**
 * Nombres cortos para los tipos generados desde contratos/nucleo.openapi.yaml (npm run gen:api). Igual que
 * modelos.ts con el catálogo: aquí no se declara ningún tipo a mano.
 */
type Esquemas = components['schemas'];

export type Rol = Esquemas['Rol'];
export type Cuenta = Esquemas['Cuenta'];
/** Lo que responden POST /cuentas/sesion y /cuentas/sesion/renovacion. */
export type RespuestaSesion = Esquemas['Sesion'];
export type RegistroRequest = Esquemas['RegistroRequest'];
/** La versión vigente de la política de datos y dónde leer su texto. */
export type PoliticaDatos = Esquemas['PoliticaDatos'];
