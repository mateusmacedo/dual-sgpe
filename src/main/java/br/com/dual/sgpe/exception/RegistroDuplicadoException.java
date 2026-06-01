package br.com.dual.sgpe.exception;

/**
 * Lançada quando uma operação violaria a unicidade de {@code login} (RN002)
 * ou {@code cpf} (RN003).
 */
public class RegistroDuplicadoException extends RuntimeException {

    public RegistroDuplicadoException(String message) {
        super(message);
    }
}
