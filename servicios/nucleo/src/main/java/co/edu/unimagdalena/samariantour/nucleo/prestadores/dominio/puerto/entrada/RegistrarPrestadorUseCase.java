package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.CuentaNoEncontradaException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.PrestadorNoRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.PrestadorYaRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.RntYaRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.TipoNoCoincideConRntException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.Prestador;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada.comando.RegistrarPrestadorCommand;

import java.util.UUID;

/**
 * El prestador con cuenta registra su RNT y su tipo, y el núcleo lo valida contra el RNT (RF-62, RF-63, RF-68, RF-70,
 * RF-72). Si queda activo, se avisa al catálogo que puede publicar (DC-02).
 */
public interface RegistrarPrestadorUseCase {

    /**
     * @throws CuentaNoEncontradaException    si no hay una cuenta de prestador activa con ese identificador
     * @throws PrestadorYaRegistradoException si ya registró su RNT
     * @throws RntYaRegistradoException       si otro prestador ya tiene ese RNT
     * @throws TipoNoCoincideConRntException  si el tipo no corresponde a la categoría del RNT
     * @throws DatoInvalidoException          si un dato no cumple las reglas
     */
    Prestador registrar(RegistrarPrestadorCommand comando);

    /**
     * Cambia el número de RNT y lo vuelve a validar (RF-62, RF-63).
     *
     * @throws PrestadorNoRegistradoException si todavía no registró uno
     * @throws RntYaRegistradoException       si otro prestador ya tiene el nuevo
     * @throws TipoNoCoincideConRntException  si su tipo no corresponde a la categoría del nuevo RNT
     */
    Prestador cambiarRnt(UUID prestadorId, String numeroRnt);
}
