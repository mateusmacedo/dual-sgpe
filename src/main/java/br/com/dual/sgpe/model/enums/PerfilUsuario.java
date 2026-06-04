package br.com.dual.sgpe.model.enums;

/**
 * Perfis de acesso de um usuário do sistema (RN001 — perfil obrigatório).
 *
 * <p>Cada perfil possui um {@link #getNivel() nível} hierárquico explícito que
 * define quem pode gerenciar quem: {@code ADMINISTRADOR (3) > GERENTE (2) >
 * COLABORADOR (1)}. Um perfil só pode gerenciar perfis de nível igual ou
 * inferior ao seu (ver {@link #podeGerenciar(PerfilUsuario)}).
 */
public enum PerfilUsuario {
    /** Acesso total ao sistema, incluindo gerenciamento de usuários. */
    ADMINISTRADOR(3),
    /** Gerencia projetos e equipes; não administra usuários. */
    GERENTE(2),
    /** Executa tarefas; acesso restrito à visualização e atualização de suas próprias tarefas. */
    COLABORADOR(1);

    private final int nivel;

    PerfilUsuario(int nivel) {
        this.nivel = nivel;
    }

    /**
     * Nível hierárquico do perfil. Valores maiores indicam maior autoridade.
     *
     * @return o nível do perfil (ADMINISTRADOR = 3, GERENTE = 2, COLABORADOR = 1)
     */
    public int getNivel() {
        return nivel;
    }

    /**
     * Indica se este perfil pode gerenciar o perfil alvo, ou seja, se possui
     * nível igual ou superior ao dele. Usado para impedir que um usuário designe
     * tarefas ou adicione a uma equipe outro usuário de nível superior.
     *
     * @param alvo perfil a ser gerenciado
     * @return {@code true} se este perfil tem nível maior ou igual ao do alvo
     */
    public boolean podeGerenciar(PerfilUsuario alvo) {
        return this.nivel >= alvo.nivel;
    }

    /**
     * Indica se um usuário com este perfil pode ser designado como responsável
     * de tarefa ou membro de equipe. Administradores não são designáveis —
     * seu papel é gerenciar, não executar.
     *
     * @return {@code true} se o perfil permite designação
     */
    public boolean isDesignavel() {
        return this != ADMINISTRADOR;
    }
}
