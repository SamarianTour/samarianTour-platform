package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.DatosZona;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Zona;

import java.util.List;

/**
 * El administrador gestiona y marca las zonas del destino (HU-ADM-06, RF-84, DC-49). Las marcas y las buenas prácticas
 * se aplican de inmediato en el catálogo público, porque la ficha y la recomendación leen la zona al responder.
 */
public interface GestionarZonasUseCase {

    /** Activas e inactivas, ordenadas por nombre. */
    List<Zona> listarTodas();

    /**
     * Registra una zona nueva, activa.
     *
     * @throws co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.ZonaDuplicadaException si ya hay una
     *         zona con ese nombre
     */
    Zona registrar(DatosZona datos);

    /**
     * Reemplaza los datos de la zona. Con {@code activa} en false la inactiva: sus ofertas siguen visibles, pero no se
     * publican ni se editan ofertas en ella (DC-12). Con true la reactiva.
     *
     * @throws co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.ZonaNoEncontradaException si no existe
     * @throws co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.ZonaDuplicadaException si otra zona ya
     *         tiene ese nombre
     */
    Zona editar(String zonaId, DatosZona datos, boolean activa);
}
