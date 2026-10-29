package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo;

import java.util.Objects;

/**
 * El resultado de buscar un número en el RNT (RF-62, RF-76, DN-33): encontrado (y por eso activo), no encontrado, o
 * la fuente no respondió en 3 s. Que la fuente no responda no es un error para el prestador: queda en Verificación
 * pendiente (QAS-03).
 */
public sealed interface ConsultaRnt {

    record Encontrado(RegistroRnt registro) implements ConsultaRnt {

        public Encontrado {
            Objects.requireNonNull(registro, "registro");
        }
    }

    record NoEncontrado() implements ConsultaRnt {
    }

    record NoDisponible(String motivo) implements ConsultaRnt {
    }
}
