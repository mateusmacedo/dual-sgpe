package br.com.dual.sgpe.model.enums;

/**
 * Perfis de acesso de um usuário do sistema (RN001 — perfil obrigatório).
 */
public enum PerfilUsuario {
    /** Acesso total ao sistema, incluindo gerenciamento de usuários. */
    ADMINISTRADOR,
    /** Gerencia projetos e equipes; não administra usuários. */
    GERENTE,
    /** Executa tarefas; acesso restrito à visualização e atualização de suas próprias tarefas. */
    COLABORADOR
}
