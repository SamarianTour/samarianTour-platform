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
/** Lo que se reemplaza al actualizar los datos personales: el nombre y, solo el turista, su WhatsApp. */
export type DatosPersonalesRequest = Esquemas['DatosPersonalesRequest'];
/** La versión vigente de la política de datos y dónde leer su texto. */
export type PoliticaDatos = Esquemas['PoliticaDatos'];

/** Los nueve estados de una solicitud de disponibilidad (SRS 3.1.3, DN-09). */
export type EstadoSolicitud = Esquemas['EstadoSolicitud'];
/** Lo que el turista envía para pedir disponibilidad (RF-30): los campos cambian con el tipo de oferta. */
export type EnviarSolicitudRequest = Esquemas['EnviarSolicitudRequest'];
/** Una solicitud del turista con su historial y su plazo vigente (RF-33). */
export type SolicitudTurista = Esquemas['SolicitudTurista'];
export type PaginaSolicitudesTurista = Esquemas['PaginaSolicitudesTurista'];
/** La oferta como se copió al crear la solicitud (DC-35). */
export type OfertaSolicitada = Esquemas['OfertaSolicitada'];
/** Lo que registró el operador y el turista ve antes de confirmar (RF-42). */
export type CondicionesSolicitud = Esquemas['Condiciones'];
/** Un cambio de estado en el historial de la solicitud. */
export type TransicionSolicitud = Esquemas['Transicion'];
/** Lo que el turista decide sobre las condiciones de una solicitud atendida (RF-43). */
export type DecisionCondiciones = Esquemas['RespuestaCondicionesRequest']['decision'];
/** Una solicitud como aparece en la lista: sin historial ni condiciones (esas llegan al abrirla). */
export type SolicitudResumen = Esquemas['SolicitudResumen'];
