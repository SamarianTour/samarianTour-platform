package co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.entrada.rest.dto;

/** La contraseña actual, que confirma que el titular pide suprimir sus datos (RF-13, DN-45). */
public record SupresionPrestadorRequest(String contrasena) {

    @Override
    public String toString() {
        return "SupresionPrestadorRequest[contrasena=******]";
    }
}
