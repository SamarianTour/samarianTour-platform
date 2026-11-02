package co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo;

import java.util.List;

/** El embudo de un tipo de oferta (RF-88). */
public record EmbudoTipo(String tipoOferta, List<EtapaEmbudo> etapas) {
}
