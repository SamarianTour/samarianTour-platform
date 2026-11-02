package co.edu.unimagdalena.samariantour.nucleo.indicadores.infraestructura.salida.catalogo;

import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.excepcion.CatalogoNoDisponibleException;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.OfertasElegibles;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.salida.OfertaElegibleConsultaPort;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Las ofertas visibles de cada prestador, de {@code GET /internal/ofertas/por-prestador} del catálogo (DC-51), con el
 * cliente de siempre: espera de 3 s y el token de servicio del núcleo (DN-38).
 */
@Component
public class CatalogoRestClientAdapter implements OfertaElegibleConsultaPort {

    private final RestClient catalogo;

    public CatalogoRestClientAdapter(RestClient clienteCatalogo) {
        this.catalogo = clienteCatalogo;
    }

    @Override
    public List<OfertasElegibles> porPrestador() {
        try {
            OfertasJson[] respuesta = catalogo.get().uri("/internal/ofertas/por-prestador").retrieve()
                    .body(OfertasJson[].class);
            return respuesta == null ? List.of() : Arrays.stream(respuesta)
                    .map(ofertas -> new OfertasElegibles(ofertas.prestadorId(), ofertas.ofertas()))
                    .toList();
        } catch (RestClientResponseException e) {
            throw new CatalogoNoDisponibleException("respondió " + e.getStatusCode().value());
        } catch (RestClientException e) {
            throw new CatalogoNoDisponibleException("no respondió");
        }
    }

    record OfertasJson(UUID prestadorId, long ofertas) {
    }
}
