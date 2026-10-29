package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo;

import java.time.Instant;

/**
 * Lo que recibe el cliente al iniciar o renovar la sesión (RF-04, DN-29): el token de acceso y el de renovación, que
 * solo se muestra esta vez.
 */
public record SesionIniciada(Usuario usuario, TokenAcceso acceso, TokenOpaco renovacion,
                             Instant renovacionExpiraEn) {
}
