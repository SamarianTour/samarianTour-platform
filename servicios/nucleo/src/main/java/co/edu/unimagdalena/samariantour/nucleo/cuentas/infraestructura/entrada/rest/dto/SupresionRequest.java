package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.entrada.rest.dto;

/** La contraseña actual, que confirma que el titular pide suprimir sus datos (RF-13, DN-31). */
public record SupresionRequest(String contrasena) {

    @Override
    public String toString() {
        return "SupresionRequest[contrasena=******]";
    }
}
