package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo;

/** Estado de una cuenta. Solo una cuenta activa inicia sesión. */
public enum EstadoCuenta {
    ACTIVA,
    /** Una cuenta interna que el administrador dio de baja (RF-81). */
    DESACTIVADA,
    /** Sus datos personales se anonimizaron a petición del titular (RF-13); no vuelve a estar activa. */
    SUPRIMIDA
}
