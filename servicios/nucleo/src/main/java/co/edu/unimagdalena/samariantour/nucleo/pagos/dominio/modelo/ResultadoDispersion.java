package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo;

/**
 * Lo que respondió la pasarela a una orden de dispersión.
 *
 * @param idPasarela el identificador de la transferencia en la pasarela, si fue exitosa
 * @param causa      por qué falló, si falló
 */
public record ResultadoDispersion(boolean exitosa, String idPasarela, String causa) {

    public static ResultadoDispersion exito(String idPasarela) {
        return new ResultadoDispersion(true, idPasarela, null);
    }

    public static ResultadoDispersion fallo(String causa) {
        return new ResultadoDispersion(false, null, causa);
    }
}
