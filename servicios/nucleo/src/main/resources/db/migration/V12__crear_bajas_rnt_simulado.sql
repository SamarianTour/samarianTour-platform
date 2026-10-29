-- Los números que la demo dio de baja en el RNT simulado (DN-36). Los registros activos están en application-demo.yml;
-- las bajas van aquí para que las dos réplicas las vean. Con el RNT real (nucleo.rnt.fuente=soda) queda vacía.
CREATE TABLE rnt_simulado_bajas (
    numero_rnt  VARCHAR(10)  PRIMARY KEY
);
