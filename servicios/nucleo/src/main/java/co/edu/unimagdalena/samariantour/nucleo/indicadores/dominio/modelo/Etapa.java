package co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo;

/** Las etapas del embudo de conversión (RF-87), con el estado de la solicitud que marca cada una. */
public enum Etapa {
    ENVIADAS("PENDIENTE"),
    ATENDIDAS("ATENDIDA"),
    CONFIRMADAS("CONFIRMADA"),
    PAGADAS("PAGADA");

    private final String estado;

    Etapa(String estado) {
        this.estado = estado;
    }

    public String estado() {
        return estado;
    }
}
