package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.mapper;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Coordenada;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Validaciones;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.CoordenadaDto;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Convierte los textos de la API en tipos del dominio. Un valor que no se puede convertir responde
 * DATO_INVALIDO con el campo y los valores o el formato que se esperaban.
 */
final class Conversiones {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private Conversiones() {
    }

    static <E extends Enum<E>> E enumerado(String valor, String campo, Class<E> tipo) {
        return Validaciones.presente(enumeradoOpcional(valor, campo, tipo), campo);
    }

    /** @return null si el valor no viene */
    static <E extends Enum<E>> E enumeradoOpcional(String valor, String campo, Class<E> tipo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, valor.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            String permitidos = Arrays.stream(tipo.getEnumConstants()).map(Enum::name)
                    .collect(Collectors.joining(", "));
            throw new DatoInvalidoException(campo, "valores permitidos: " + permitidos);
        }
    }

    static UUID uuid(String valor, String campo) {
        try {
            return UUID.fromString(Validaciones.texto(valor, campo));
        } catch (IllegalArgumentException e) {
            throw new DatoInvalidoException(campo, "debe ser un identificador UUID");
        }
    }

    /** @return null si la lista no viene, para que el dominio decida si es obligatoria */
    static List<LocalTime> horas(List<String> valores, String campo) {
        if (valores == null) {
            return null;
        }
        return valores.stream().map(valor -> hora(valor, campo)).toList();
    }

    static List<String> horas(List<LocalTime> horas) {
        return horas.stream().map(HORA::format).toList();
    }

    static Coordenada coordenada(CoordenadaDto ubicacion) {
        if (ubicacion == null) {
            return null;
        }
        if (ubicacion.latitud() == null || ubicacion.longitud() == null) {
            throw new DatoInvalidoException("ubicacion", "necesita latitud y longitud");
        }
        return new Coordenada(ubicacion.latitud(), ubicacion.longitud());
    }

    static CoordenadaDto coordenada(Coordenada ubicacion) {
        return ubicacion == null ? null : new CoordenadaDto(ubicacion.latitud(), ubicacion.longitud());
    }

    private static LocalTime hora(String valor, String campo) {
        try {
            return LocalTime.parse(Validaciones.texto(valor, campo), HORA);
        } catch (DateTimeParseException e) {
            throw new DatoInvalidoException(campo, "usa el formato HH:mm, por ejemplo 07:30");
        }
    }
}
