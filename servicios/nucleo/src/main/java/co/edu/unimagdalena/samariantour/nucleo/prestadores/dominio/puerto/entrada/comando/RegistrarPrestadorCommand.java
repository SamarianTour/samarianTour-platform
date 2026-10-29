package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada.comando;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.DatosTipo;

import java.util.UUID;

/** @param prestadorId el de la identidad: el de su cuenta */
public record RegistrarPrestadorCommand(UUID prestadorId, String numeroRnt, DatosTipo datosTipo) {
}
