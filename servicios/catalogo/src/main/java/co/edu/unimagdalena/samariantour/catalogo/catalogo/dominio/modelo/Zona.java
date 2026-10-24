package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo;

import java.util.List;

/**
 * Lugar o atractivo turístico registrado por el Administrador (RF-84). El administrador la registra, la edita y la
 * inactiva, pero nunca la elimina (DC-49).
 *
 * @param areaProtegida marca de área protegida o ecosistema sensible; RF-84 las une en una sola marca (RF-25)
 * @param altaAfluencia marca de zona congestionada (RF-29)
 * @param activa        una zona inactiva no admite ofertas nuevas ni ediciones (DC-12), pero las que ya tiene siguen
 *                      visibles
 */
public record Zona(
        String id,
        String nombre,
        String descripcion,
        Coordenada ubicacion,
        boolean areaProtegida,
        boolean altaAfluencia,
        boolean activa,
        List<String> buenasPracticas,
        String condicionesVisita) {

    public Zona {
        id = Validaciones.texto(id, "id");
        nombre = Validaciones.texto(nombre, "nombre");
        descripcion = Validaciones.textoOpcional(descripcion);
        buenasPracticas = Validaciones.lista(buenasPracticas, "buenasPracticas");
        condicionesVisita = Validaciones.textoOpcional(condicionesVisita);
    }

    /** La zona que registra el administrador: nace activa (DC-49). */
    public static Zona registrar(String id, DatosZona datos) {
        return con(id, datos, true);
    }

    /**
     * Reemplaza todos los datos de la zona. Con {@code activa} en false la inactiva, y en true la reactiva (DC-49).
     */
    public Zona editar(DatosZona datos, boolean activa) {
        return con(id, datos, activa);
    }

    /**
     * El nombre para compararlo con el de otras zonas: sin tildes, en minúsculas y con un solo espacio entre palabras.
     * Dos zonas no pueden tener la misma clave (DC-49): "Minca" y "MINCÁ" son la misma.
     */
    public String claveNombre() {
        return TextoBusqueda.normalizar(nombre).replaceAll("\\s+", " ");
    }

    private static Zona con(String id, DatosZona datos, boolean activa) {
        return new Zona(id, datos.nombre(), datos.descripcion(), datos.ubicacion(), datos.areaProtegida(),
                datos.altaAfluencia(), activa, datos.buenasPracticas(), datos.condicionesVisita());
    }
}
