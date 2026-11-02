package co.edu.unimagdalena.samariantour.nucleo.prestadores.api;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Un prestador, para que el administrador le asigne el operador (RF-81).
 *
 * @param razonSocial la del RNT; vacía mientras no se encuentre
 * @param tipo        ALOJAMIENTO, RESTAURANTE, AGENCIA_OPERADORA o GUIA_INDEPENDIENTE
 * @param estadoRnt   ACTIVO, INACTIVO, NO_ENCONTRADO o VERIFICACION_PENDIENTE
 * @param operadorId  el operador que atiende sus solicitudes; vacío si no tiene
 */
public record PrestadorAdministrado(UUID id, String numeroRnt, String razonSocial, String tipo, String estadoRnt,
                                    UUID operadorId, Instant registradoEn) {

    public PrestadorAdministrado {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(numeroRnt, "numeroRnt");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(estadoRnt, "estadoRnt");
    }
}
