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

/** Una solicitud en la cola o el historial del operador (RF-35, RF-36): `proximaAVencer` marca las de 6 h o menos. */
export type SolicitudCola = Esquemas['SolicitudCola'];
export type PaginaColaOperador = Esquemas['PaginaColaOperador'];
/** Una solicitud para verificarla con el prestador (RF-37): trae su WhatsApp y su historial, no datos del turista. */
export type SolicitudOperador = Esquemas['SolicitudOperador'];
/** Lo que el operador registra al atender: hay cupo con sus condiciones, o no hay (RF-37 a RF-40). */
export type AtencionRequest = Esquemas['AtencionRequest'];
/** Un aviso del operador en la plataforma (RF-34): cada solicitud nueva de su cola deja uno. */
export type AvisoOperador = Esquemas['Aviso'];
export type PaginaAvisos = Esquemas['PaginaAvisos'];

/** El intento de pago en curso y cómo abrirlo en la pasarela (RF-46 a RF-49). */
export type SesionCobro = Esquemas['SesionCobro'];
/** Cómo abrir el pago: la dirección del checkout. El backend nunca ve datos de tarjeta ni de cuenta (RF-47). */
export type CheckoutPago = Esquemas['Checkout'];
/** Un intento de pago de una solicitud, con su estado y el id de la transacción en la pasarela (RF-52). */
export type IntentoPago = Esquemas['IntentoPago'];
export type EstadoIntento = Esquemas['EstadoIntento'];

/** Un prestador como lo ve el administrador para asignarle su operador (RF-81). */
export type PrestadorAdmin = Esquemas['PrestadorAdmin'];
/** Un prestador que la recomendación muestra más de lo que le corresponde (RET-02). */
export type AlertaSesgo = Esquemas['AlertaSesgo'];

/** Cómo está un prestador frente al RNT (RF-76 a RF-78). */
export type EstadoRnt = Esquemas['PrestadorAdmin']['estadoRnt'];
/** Lo que hizo una sincronización con el RNT: cuántos se revisaron, retiraron y restituyeron. */
export type SincronizacionRnt = Esquemas['SincronizacionRnt'];
/** Una cuenta de administrador, operador de disponibilidad o gestor del destino (RF-81). */
export type CuentaInterna = Esquemas['CuentaInterna'];

/** Aprobar o rechazar lo que está en moderación (RF-79, RF-80). */
export type DecisionModerar = Esquemas['Decision'];
/** La decisión sobre una versión de oferta: lleva el número de la versión que el administrador revisó. */
export type DecisionOfertaRequest = Esquemas['DecisionOfertaRequest'];
export type DecisionOpinionRequest = Esquemas['DecisionOpinionRequest'];
/** La decisión guardada por el núcleo; el catálogo la aplica en los siguientes segundos por la bandeja de salida. */
export type DecisionModeracion = Esquemas['DecisionModeracion'];

/** Lo que escribe el administrador para crear la cuenta de un administrador, un operador de disponibilidad o un gestor (RF-81). */
export type CuentaInternaRequest = Esquemas['CuentaInternaRequest'];
