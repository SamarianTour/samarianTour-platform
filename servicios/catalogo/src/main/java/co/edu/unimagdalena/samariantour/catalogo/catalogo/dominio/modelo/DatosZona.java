package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;

import java.util.List;

/**
 * Lo que escribe el administrador al registrar o editar una zona (HU-ADM-06, RF-84, DC-49). Nombre, descripción y
 * ubicación son obligatorios, como en la historia; las buenas prácticas y las condiciones de visita, no.
 *
 * <p>Estas reglas son de lo que se escribe: {@link Zona} no las exige al leer, porque una zona guardada antes de DC-49
 * puede no tener descripción o ubicación.
 *
 * @param buenasPracticas una por elemento, sin elementos en blanco
 */
public record DatosZona(
        String nombre,
        String descripcion,
        Coordenada ubicacion,
        boolean areaProtegida,
        boolean altaAfluencia,
        List<String> buenasPracticas,
        String condicionesVisita) {

    public DatosZona {
        nombre = Validaciones.texto(nombre, "nombre");
        descripcion = Validaciones.texto(descripcion, "descripcion");
        Validaciones.presente(ubicacion, "ubicacion");
        buenasPracticas = Validaciones.lista(buenasPracticas, "buenasPracticas").stream()
                .map(practica -> {
                    if (practica.isBlank()) {
                        throw new DatoInvalidoException("buenasPracticas", "no admite elementos vacíos");
                    }
                    return practica.strip();
                })
                .toList();
        condicionesVisita = Validaciones.textoOpcional(condicionesVisita);
    }
}
