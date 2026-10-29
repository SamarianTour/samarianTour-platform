package co.edu.unimagdalena.samariantour.nucleo.prestadores;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.ConsultaRnt;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.DatosTipo;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.ModalidadGuianza;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.RegistroRnt;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.TipoPrestador;

import java.time.Instant;
import java.util.List;

/** Datos comunes de las pruebas de prestadores. */
public final class PrestadoresDePrueba {

    public static final Instant AHORA = Instant.parse("2026-10-29T15:00:00Z");

    public static final DatosTipo AGENCIA = new DatosTipo(TipoPrestador.AGENCIA_OPERADORA, 12, List.of(), List.of());
    public static final DatosTipo GUIA = new DatosTipo(TipoPrestador.GUIA_INDEPENDIENTE, null,
            List.of(ModalidadGuianza.CAMINATA), List.of("es", "en"));

    private PrestadoresDePrueba() {
    }

    public static ConsultaRnt agenciaActiva(String numero) {
        return new ConsultaRnt.Encontrado(new RegistroRnt(numero, "MAR Y SELVA EXPEDICIONES S.A.S.", "SANTA MARTA",
                "AGENCIAS DE VIAJES", "AGENCIA DE VIAJES OPERADORAS", false, 12));
    }

    public static ConsultaRnt guiaActivo(String numero) {
        return new ConsultaRnt.Encontrado(new RegistroRnt(numero, "JUAN CARLOS PEREZ ARIAS", "SANTA MARTA",
                "GUIAS DE TURISMO", "GUIA DE TURISMO", true, 1));
    }
}
