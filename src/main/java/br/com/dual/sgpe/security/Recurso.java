package br.com.dual.sgpe.security;

/**
 * Recursos (telas/funcionalidades) do sistema sujeitos a controle de acesso
 * por perfil (RBAC). Cada item corresponde a uma área navegável da Tela
 * Principal e carrega o rótulo legível exibido nos botões de navegação.
 */
public enum Recurso {
    USUARIOS("Usuários"),
    PROJETOS("Projetos"),
    TAREFAS("Tarefas"),
    EQUIPES("Equipes"),
    CONSULTA("Consultar Projetos"),
    RELATORIO("Relatório de Desempenho");

    private final String rotulo;

    Recurso(String rotulo) {
        this.rotulo = rotulo;
    }

    /**
     * Rótulo legível do recurso, exibido nos botões da Tela Principal.
     *
     * @return texto de exibição do recurso
     */
    public String getRotulo() {
        return rotulo;
    }
}
