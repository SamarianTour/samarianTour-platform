package co.edu.unimagdalena.samariantour.catalogo.opiniones.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Opinion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.OpinionConOferta;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.salida.OfertaCatalogoPort;

import java.util.Map;

/**
 * Agrega a una página de opiniones el nombre de su oferta, con una sola consulta al catálogo para toda la página.
 */
final class NombresDeOfertas {

    private final OfertaCatalogoPort ofertas;

    NombresDeOfertas(OfertaCatalogoPort ofertas) {
        this.ofertas = ofertas;
    }

    Pagina<OpinionConOferta> de(Pagina<Opinion> pagina) {
        Map<String, String> nombres = pagina.contenido().isEmpty() ? Map.of()
                : ofertas.nombres(pagina.contenido().stream().map(Opinion::ofertaId).distinct().toList());
        return pagina.map(opinion -> new OpinionConOferta(opinion, nombres.get(opinion.ofertaId())));
    }
}
