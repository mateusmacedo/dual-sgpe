package br.com.dual.sgpe.exception;

/**
 * Lançada quando uma operação de persistência (acesso a dados via JDBC) falha,
 * encapsulando a {@link java.sql.SQLException} original como causa. Isola as
 * camadas superiores dos detalhes do JDBC.
 */
public class PersistenciaException extends RuntimeException {

    public PersistenciaException(String message) {
        super(message);
    }

    public PersistenciaException(String message, Throwable cause) {
        super(message, cause);
    }
}
