package co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.entrada.rest.dto;

import java.util.List;

/**
 * El registro del prestador (RF-62, RF-68, RF-69).
 *
 * @param tipo            ALOJAMIENTO, RESTAURANTE, AGENCIA_OPERADORA o GUIA_INDEPENDIENTE
 * @param numeroEmpleados vacío para el guía independiente
 * @param modalidades     solo el guía: CAMINATA, BICICLETA, KAYAK u OTRA
 * @param idiomas         solo el guía: códigos de dos letras ("es", "en")
 */
public record RegistroPrestadorRequest(String numeroRnt, String tipo, Integer numeroEmpleados,
                                       List<String> modalidades, List<String> idiomas) {
}
