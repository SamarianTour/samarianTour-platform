/**
 * Adaptadores de salida en MongoDB, activos con el perfil {@code mongo}: usan {@code MongoTemplate} con los
 * documentos de {@code document} y los mappers documento <-> dominio de {@code mapper}. Siguen el modelo documental
 * (DC-16, DC-30) y el bloqueo optimista del ADR-05.
 */
package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia;
