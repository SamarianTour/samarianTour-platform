package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.CriteriosBusqueda;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.OfertasDePrestador;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.ContarOfertasVisiblesUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.OfertaRepositoryPort;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cuenta las ofertas visibles de cada prestador recorriendo la búsqueda pública por páginas, así la visibilidad es
 * exactamente la misma que ve el turista (DC-51). Lo pide el núcleo una vez al día; no necesita una consulta propia.
 */
public class ContarOfertasVisiblesService implements ContarOfertasVisiblesUseCase {

    private final OfertaRepositoryPort ofertas;

    public ContarOfertasVisiblesService(OfertaRepositoryPort ofertas) {
        this.ofertas = ofertas;
    }

    @Override
    public List<OfertasDePrestador> porPrestador() {
        Map<UUID, Long> conteo = new HashMap<>();
        Pagina<Oferta> pagina;
        int numero = 0;
        do {
            pagina = ofertas.buscarVisibles(new CriteriosBusqueda(null, null, null, null, null, null, null, false,
                    new Paginacion(numero++, Paginacion.TAMANO_MAXIMO)));
            pagina.contenido().forEach(oferta -> conteo.merge(oferta.prestadorId(), 1L, Long::sum));
        } while (numero < pagina.totalPaginas());
        return conteo.entrySet().stream()
                .map(entrada -> new OfertasDePrestador(entrada.getKey(), entrada.getValue()))
                .sorted(Comparator.comparingLong(OfertasDePrestador::ofertas).reversed()
                        .thenComparing(OfertasDePrestador::prestadorId))
                .toList();
    }
}
