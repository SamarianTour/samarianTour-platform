package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida;

import java.util.UUID;

/** Lo que los prestadores necesitan de las cuentas (módulo 1): que la cuenta exista y sea de un prestador activo. */
public interface CuentasPort {

    boolean esPrestadorActivo(UUID usuarioId);
}
