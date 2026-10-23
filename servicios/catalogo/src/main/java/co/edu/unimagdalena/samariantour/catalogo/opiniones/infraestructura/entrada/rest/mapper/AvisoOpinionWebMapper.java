package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.mapper;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Validaciones;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.comando.AvisoHabilitacionOpinion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.comando.AvisoModeracionOpinion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.comando.AvisoOpinionCommand;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.comando.AvisoSupresionTurista;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.comando.DecisionModeracion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.dto.AvisoOpinionRequest;

import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Convierte el aviso del núcleo en el comando del caso de uso. Un valor que no se puede convertir responde
 * DATO_INVALIDO con el campo y lo que se esperaba, como en el catálogo.
 */
public final class AvisoOpinionWebMapper {

    private AvisoOpinionWebMapper() {
    }

    public static AvisoOpinionCommand aComando(AvisoOpinionRequest aviso) {
        return switch (enumerado(aviso.tipo(), "tipo", TipoAviso.class)) {
            case HABILITACION_OPINION -> new AvisoHabilitacionOpinion(aviso.idAviso(),
                    uuid(aviso.turistaId(), "turistaId"), aviso.ofertaId());
            case MODERACION_OPINION -> new AvisoModeracionOpinion(aviso.idAviso(), aviso.opinionId(),
                    enumerado(aviso.decision(), "decision", DecisionModeracion.class), aviso.motivo());
            case SUPRESION_TURISTA -> new AvisoSupresionTurista(aviso.idAviso(), uuid(aviso.turistaId(), "turistaId"));
        };
    }

    private static <E extends Enum<E>> E enumerado(String valor, String campo, Class<E> tipo) {
        String texto = Validaciones.texto(valor, campo);
        try {
            return Enum.valueOf(tipo, texto.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            String permitidos = Arrays.stream(tipo.getEnumConstants()).map(Enum::name)
                    .collect(Collectors.joining(", "));
            throw new DatoInvalidoException(campo, "valores permitidos: " + permitidos);
        }
    }

    private static UUID uuid(String valor, String campo) {
        try {
            return UUID.fromString(Validaciones.texto(valor, campo));
        } catch (IllegalArgumentException e) {
            throw new DatoInvalidoException(campo, "debe ser un identificador UUID");
        }
    }

    /** Tipos de aviso que el núcleo envía al módulo de opiniones. */
    private enum TipoAviso {
        HABILITACION_OPINION,
        MODERACION_OPINION,
        SUPRESION_TURISTA
    }
}
