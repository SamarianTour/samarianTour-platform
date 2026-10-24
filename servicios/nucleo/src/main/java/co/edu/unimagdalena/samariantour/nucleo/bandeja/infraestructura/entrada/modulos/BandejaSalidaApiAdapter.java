package co.edu.unimagdalena.samariantour.nucleo.bandeja.infraestructura.entrada.modulos;

import co.edu.unimagdalena.samariantour.nucleo.bandeja.api.BandejaSalidaApi;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.api.DestinoAviso;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.modelo.Destino;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.puerto.entrada.EncolarAvisoUseCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * Atiende a los demás módulos del núcleo por {@code bandeja.api}, como un controlador REST pero dentro del proceso:
 * convierte el aviso a JSON y llama al caso de uso.
 */
@Component
public class BandejaSalidaApiAdapter implements BandejaSalidaApi {

    private final EncolarAvisoUseCase encolar;
    private final ObjectMapper json;

    public BandejaSalidaApiAdapter(EncolarAvisoUseCase encolar, ObjectMapper json) {
        this.encolar = encolar;
        this.json = json;
    }

    @Override
    public boolean encolar(String idAviso, DestinoAviso destino, Object cuerpo) {
        try {
            return encolar.encolar(idAviso, Destino.valueOf(destino.name()), json.writeValueAsString(cuerpo));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("El aviso " + idAviso + " no se puede convertir a JSON", e);
        }
    }
}
