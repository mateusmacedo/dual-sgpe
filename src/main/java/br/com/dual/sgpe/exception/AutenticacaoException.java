package br.com.dual.sgpe.exception;

/**
 * Lançada quando a autenticação falha. A mensagem é sempre genérica para não
 * revelar se o login não existe ou se a senha está incorreta (anti-enumeração).
 */
public class AutenticacaoException extends RuntimeException {

    public AutenticacaoException(String message) {
        super(message);
    }
}
