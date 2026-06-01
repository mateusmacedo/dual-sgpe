package br.com.dual.sgpe.model.enums;

/**
 * Status de um projeto (RN004). Persistido como TEXT (via {@code name()}).
 */
public enum StatusProjeto {
    /** Projeto criado, ainda não iniciado. */
    PLANEJADO,
    /** Projeto em execução ativa. */
    EM_ANDAMENTO,
    /** Todas as entregas foram finalizadas com sucesso. */
    CONCLUIDO,
    /** Projeto encerrado antes da conclusão. */
    CANCELADO
}
