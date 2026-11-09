package co.edu.unimagdalena.samariantour.nucleo.bandeja.api;

import java.util.List;

/**
 * Lo que la administración hace con la bandeja de salida (DN-44), aparte de {@link BandejaSalidaApi}, que es para los
 * módulos que encolan.
 */
public interface BandejaAdministracionApi {

    /** Los avisos que el catálogo rechazó, del más reciente al más antiguo. */
    List<AvisoAdministrado> rechazados();

    /**
     * Vuelve a dejar pendiente un aviso rechazado: la publicación lo envía en los siguientes 15 s.
     *
     * @throws AvisoNoReenviableException si no existe o no está rechazado
     */
    AvisoAdministrado reenviar(String idAviso);
}
