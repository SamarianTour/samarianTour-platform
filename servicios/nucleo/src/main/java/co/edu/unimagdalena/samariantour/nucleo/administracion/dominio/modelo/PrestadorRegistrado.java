package co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo;

import java.time.Instant;
import java.util.UUID;

/**
 * Un prestador que ya registró su RNT, como lo dan los prestadores, para asignarle el operador (RF-81).
 *
 * @param razonSocial la del RNT, vacía mientras no se encuentre
 * @param operadorId  el operador que atiende sus solicitudes; vacío si no tiene
 */
public record PrestadorRegistrado(UUID id, String numeroRnt, String razonSocial, String tipo, String estadoRnt,
                                  UUID operadorId, Instant registradoEn) {
}
