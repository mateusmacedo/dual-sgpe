package br.com.dual.sgpe.model.enums;

/**
 * Status de ciclo de vida de uma tarefa. Persistido como TEXT (via {@code name()}).
 */
public enum StatusTarefa {
    /** Tarefa criada, ainda não iniciada. */
    PENDENTE,
    /** Tarefa em execução pelo responsável. */
    EM_ANDAMENTO,
    /** Tarefa finalizada com sucesso. */
    CONCLUIDA,
    /** Tarefa encerrada sem conclusão. */
    CANCELADA
}
