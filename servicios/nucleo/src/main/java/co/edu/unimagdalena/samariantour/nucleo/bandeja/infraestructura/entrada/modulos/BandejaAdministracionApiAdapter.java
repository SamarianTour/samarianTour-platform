package co.edu.unimagdalena.samariantour.nucleo.bandeja.infraestructura.entrada.modulos;

import co.edu.unimagdalena.samariantour.nucleo.bandeja.api.AvisoAdministrado;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.api.AvisoNoReenviableException;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.api.BandejaAdministracionApi;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.modelo.AvisoSaliente;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.puerto.entrada.AdministrarBandejaUseCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

/** La entrada de la administración a la bandeja (DN-44): traduce a los casos de uso y a los tipos del {@code api}. */
@Component
public class BandejaAdministracionApiAdapter implements BandejaAdministracionApi {

    private final AdministrarBandejaUseCase bandeja;
    private final ObjectMapper json;

    public BandejaAdministracionApiAdapter(AdministrarBandejaUseCase bandeja, ObjectMapper json) {
        this.bandeja = bandeja;
        this.json = json;
    }

    @Override
    public List<AvisoAdministrado> rechazados() {
        return bandeja.rechazados().stream().map(this::aApi).toList();
    }

    @Override
    public AvisoAdministrado reenviar(String idAviso) {
        try {
            return aApi(bandeja.reenviar(idAviso));
        } catch (co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.excepcion.AvisoNoReenviableException e) {
            throw new AvisoNoReenviableException(e.codigo(), e.getMessage());
        }
    }

    private AvisoAdministrado aApi(AvisoSaliente aviso) {
        return new AvisoAdministrado(aviso.idAviso(), aviso.destino().name(), tipo(aviso.cuerpo()),
                aviso.estado().name(), aviso.intentos(), aviso.ultimoError(), aviso.creadoEn());
    }

    /** El {@code tipo} del cuerpo, que es el aviso en JSON tal como lo espera el catálogo. */
    private String tipo(String cuerpo) {
        try {
            JsonNode tipo = json.readTree(cuerpo).get("tipo");
            return tipo == null || tipo.isNull() ? null : tipo.asText();
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
