package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo;

import java.util.List;

/**
 * Lugar o atractivo turístico registrado por el Administrador (RF-84). En el prototipo es solo de lectura.
 *
 * @param areaProtegida marca de área protegida o ecosistema sensible; RF-84 las une en una sola marca (RF-25)
 * @param altaAfluencia marca de zona congestionada (RF-29)
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
}
