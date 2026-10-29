package co.edu.unimagdalena.samariantour.nucleo.prestadores.api;

import java.util.Objects;
import java.util.UUID;

/**
 * Lo que los demás módulos saben de un prestador.
 *
 * @param tipo       ALOJAMIENTO, RESTAURANTE, AGENCIA_OPERADORA o GUIA_INDEPENDIENTE
 * @param categoria  GUIA_INDEPENDIENTE, MICRO_PEQUENO, MEDIANO o GRANDE (RF-89)
 * @param habilitado si su RNT está activo: solo así publica y se le aprueban ofertas (RF-77, RF-79)
 * @param operadorId el operador que atiende sus solicitudes (RF-32); vacío si no tiene
 * @param whatsapp   su WhatsApp, solo si lo verificó (RF-57, RF-71); vacío si no
 */
public record PrestadorConsultado(UUID id, String tipo, String categoria, boolean habilitado, UUID operadorId,
                                  String whatsapp) {

    public PrestadorConsultado {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(categoria, "categoria");
    }

    @Override
    public String toString() {
        return "PrestadorConsultado[id=%s, tipo=%s, habilitado=%s]".formatted(id, tipo, habilitado);
    }
}
