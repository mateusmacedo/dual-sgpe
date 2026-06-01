package br.com.dual.sgpe.exception;

/**
 * Lançada quando a exclusão de um usuário é bloqueada por integridade
 * referencial (usuário referenciado em {@code tarefas} ou {@code equipe_usuario}).
 */
public class ExclusaoBloqueadaException extends RuntimeException {

    public ExclusaoBloqueadaException(String message) {
        super(message);
    }
}
