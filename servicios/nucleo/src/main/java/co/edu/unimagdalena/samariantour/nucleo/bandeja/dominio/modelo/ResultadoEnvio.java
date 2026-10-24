package co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.modelo;

/** Lo que pasó al enviar un aviso al catálogo (DN-06). */
public sealed interface ResultadoEnvio {

    /** El catálogo lo recibió: lo aplicó, ya lo tenía o no le correspondía (los tres responden 200). */
    record Recibido() implements ResultadoEnvio {
    }

    /** El catálogo lo rechazó con un 4xx que no se arregla reintentando. */
    record Rechazado(String motivo) implements ResultadoEnvio {
    }

    /** No llegó o el catálogo no pudo aplicarlo todavía: caído, tiempo agotado, 5xx o 409 de concurrencia. */
    record Fallido(String motivo) implements ResultadoEnvio {
    }
}
