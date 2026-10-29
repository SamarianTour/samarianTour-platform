package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.DatoInvalidoException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

/**
 * El tipo del prestador y sus campos condicionales (RF-68, RF-69): los empleados para los demás tipos; las modalidades
 * de guianza y los idiomas solo para el guía independiente. Las reglas son [SUPUESTO] (DN-33): el guía declara al
 * menos una modalidad y un idioma; los demás, de 1 a 100.000 empleados.
 *
 * @param numeroEmpleados vacío para el guía independiente
 * @param idiomas         códigos de dos letras en minúscula, como en el catálogo ("es", "en")
 */
public record DatosTipo(TipoPrestador tipo, Integer numeroEmpleados, List<ModalidadGuianza> modalidades,
                        List<String> idiomas) {

    public static final int MAXIMO_EMPLEADOS = 100_000;
    private static final Pattern IDIOMA = Pattern.compile("^[a-z]{2}$");

    public DatosTipo {
        if (tipo == null) {
            throw new DatoInvalidoException("tipo", "es obligatorio: ALOJAMIENTO, RESTAURANTE, AGENCIA_OPERADORA o "
                    + "GUIA_INDEPENDIENTE");
        }
        modalidades = sinRepetir(modalidades);
        idiomas = sinRepetir(idiomas);
        if (tipo == TipoPrestador.GUIA_INDEPENDIENTE) {
            if (numeroEmpleados != null) {
                throw new DatoInvalidoException("numeroEmpleados", "no aplica al guía independiente");
            }
            if (modalidades.isEmpty()) {
                throw new DatoInvalidoException("modalidades", "el guía independiente declara al menos una (RF-69)");
            }
            if (idiomas.isEmpty() || idiomas.stream().anyMatch(idioma -> !IDIOMA.matcher(idioma).matches())) {
                throw new DatoInvalidoException("idiomas", "al menos uno, con códigos de dos letras en minúscula, "
                        + "como \"es\" o \"en\" (RF-69)");
            }
        } else {
            if (numeroEmpleados == null || numeroEmpleados < 1 || numeroEmpleados > MAXIMO_EMPLEADOS) {
                throw new DatoInvalidoException("numeroEmpleados", "debe estar entre 1 y %d"
                        .formatted(MAXIMO_EMPLEADOS));
            }
            if (!modalidades.isEmpty() || !idiomas.isEmpty()) {
                throw new DatoInvalidoException("modalidades", "las modalidades y los idiomas son solo del guía "
                        + "independiente");
            }
        }
    }

    private static <T> List<T> sinRepetir(List<T> valores) {
        return valores == null ? List.of() : List.copyOf(new LinkedHashSet<>(valores));
    }
}
