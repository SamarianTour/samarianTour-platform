package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada;

/**
 * El webhook de la pasarela (RF-50, RT-07, DN-21): verifica la firma y guarda el evento para procesarlo después, así
 * la respuesta a la pasarela sale de inmediato.
 */
public interface RecibirEventoPasarelaUseCase {

    /**
     * @return false si ya se había recibido el mismo evento, o si no es de una transacción: no se guarda
     * @throws co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion.EventoNoAutenticoException si la firma
     *         no es válida; no se guarda nada
     */
    boolean recibir(String cuerpo, String firmaCabecera);
}
