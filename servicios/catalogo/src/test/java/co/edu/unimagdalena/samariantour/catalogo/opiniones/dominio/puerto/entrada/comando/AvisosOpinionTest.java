package co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.comando;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion.DatoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AvisosOpinionTest {

    private static final UUID TURISTA = UUID.fromString("70000000-0000-0000-0000-000000000007");

    @Test
    @DisplayName("rechazar una opinión exige motivo; aprobarla no")
    void motivoAlRechazar() {
        assertThatThrownBy(() -> new AvisoModeracionOpinion("a1", "d1", DecisionModeracion.RECHAZAR, " "))
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessage("motivo: es obligatorio al rechazar");
        assertThat(new AvisoModeracionOpinion("a1", "d1", DecisionModeracion.APROBAR, " ").motivo()).isNull();
    }

    @Test
    @DisplayName("todo aviso trae su idAviso y los datos de su tipo")
    void datosObligatorios() {
        assertThatThrownBy(() -> new AvisoHabilitacionOpinion(" ", TURISTA, "b1"))
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessage("idAviso: es obligatorio");
        assertThatThrownBy(() -> new AvisoHabilitacionOpinion("a1", TURISTA, null))
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessage("ofertaId: es obligatorio");
        assertThatThrownBy(() -> new AvisoModeracionOpinion("a1", "", DecisionModeracion.APROBAR, null))
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessage("opinionId: es obligatorio");
        assertThatThrownBy(() -> new AvisoSupresionTurista("a1", null)).isInstanceOf(NullPointerException.class);
    }
}
