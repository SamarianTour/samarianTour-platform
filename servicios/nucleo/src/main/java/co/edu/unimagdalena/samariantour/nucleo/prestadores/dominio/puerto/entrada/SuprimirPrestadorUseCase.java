package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada;

import java.util.UUID;

/** El prestador pide suprimir sus datos personales (RF-13, DN-45). */
public interface SuprimirPrestadorUseCase {

    /**
     * Es inmediata, en una sola transacción: la cuenta queda anonimizada y sin sesiones, el prestador sin WhatsApp ni
     * habilitación, y el catálogo recibe el aviso para retirar sus ofertas y borrar su perfil y sus fotos.
     *
     * @param contrasena la actual, que confirma que el titular lo pide
     * @throws co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.ContrasenaRechazadaException si
     *         no es correcta o la cuenta está bloqueada; el fallo cuenta para el bloqueo de RF-05
     * @throws co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.PrestadorConCompromisosException
     *         si tiene solicitudes pagadas por prestar, pagos en curso o dispersiones por recibir
     */
    void suprimir(UUID prestadorId, String contrasena);
}
