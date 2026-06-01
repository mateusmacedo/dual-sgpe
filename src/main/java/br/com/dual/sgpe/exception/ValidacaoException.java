package br.com.dual.sgpe.exception;

/**
 * Lançada quando dados de entrada falham na validação (campo obrigatório
 * vazio, formato inválido, perfil ausente).
 */
public class ValidacaoException extends RuntimeException {

    public ValidacaoException(String message) {
        super(message);
    }
}
