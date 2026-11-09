package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada;

import java.util.UUID;

/** Cuántas dispersiones le faltan por recibir al prestador, para la supresión de sus datos (RF-13, DN-45). */
public interface ConsultarDispersionesPorRecibirUseCase {

    /** @return las pendientes y las fallidas, que todavía pueden enviarse cuando corrija su cuenta de cobro */
    long porRecibir(UUID prestadorId);
}
