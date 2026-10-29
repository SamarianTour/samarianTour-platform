package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion;

import java.util.UUID;

/** El prestador ya registró su RNT: para cambiarlo usa el cambio de RNT (RF-62). */
public class PrestadorYaRegistradoException extends ExcepcionDominio {

    public PrestadorYaRegistradoException(UUID prestadorId) {
        super("PRESTADOR_YA_REGISTRADO", "El prestador %s ya registró su RNT; para cambiarlo usa PUT /prestador/rnt"
                .formatted(prestadorId));
    }
}
