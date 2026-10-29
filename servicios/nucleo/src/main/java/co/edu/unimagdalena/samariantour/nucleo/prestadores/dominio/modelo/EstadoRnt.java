package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo;

/** El estado del prestador frente al RNT (RF-63, RF-76). Solo uno ACTIVO está habilitado para publicar (RF-77). */
public enum EstadoRnt {
    ACTIVO,
    /** Estuvo activo y ya no aparece en el RNT: su catálogo se retira (RF-77, DN-33). */
    INACTIVO,
    /** Nunca apareció en el RNT con ese número (DN-33). */
    NO_ENCONTRADO,
    /** La fuente del RNT no respondió; se revalida en la siguiente sincronización (QAS-03). */
    VERIFICACION_PENDIENTE
}
