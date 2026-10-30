package co.edu.unimagdalena.samariantour.seguridadjwt;

/**
 * Los nombres de los servicios en el token de servicio (DN-38): van en {@code sub} (quién llama) y en {@code aud} (a
 * quién), y en la autoridad {@code SERVICIO_<nombre>} de las reglas de {@code /internal/**}.
 */
public final class ServiciosInternos {

    public static final String NUCLEO = "nucleo";
    public static final String CATALOGO = "catalogo";
    public static final String RECOMENDACION = "recomendacion";

    private ServiciosInternos() {
    }
}
