package br.com.dual.sgpe.model.dto;

import br.com.dual.sgpe.model.entity.Equipe;
import br.com.dual.sgpe.model.entity.Projeto;
import br.com.dual.sgpe.model.entity.Usuario;
import java.util.List;

/**
 * DTO de leitura com os KPIs de desempenho de um projeto.
 *
 * <p>Objeto imutável montado pela camada DAO e consumido diretamente pela
 * tela de relatório. Não é persistido — agrega dados calculados na consulta.
 */
public class RelatorioProjeto {

    private final Projeto projeto;
    /** Contagem total de tarefas associadas ao projeto. */
    private final int totalTarefas;
    /** Contagem de tarefas com status {@code CONCLUIDA}. */
    private final int tarefasConcluidas;
    /** Contagem de tarefas com status {@code PENDENTE}. */
    private final int tarefasPendentes;
    /**
     * Percentual de conclusão do projeto: {@code (tarefasConcluidas / totalTarefas) * 100}.
     * Vale {@code 0.0} quando não há tarefas.
     */
    private final double percentualConclusao;
    /** Equipes distintas com ao menos uma tarefa no projeto. */
    private final List<Equipe> equipes;
    /** Usuários distintos responsáveis por ao menos uma tarefa no projeto. */
    private final List<Usuario> responsaveis;

    public RelatorioProjeto(Projeto projeto, int totalTarefas, int tarefasConcluidas,
                            int tarefasPendentes, double percentualConclusao,
                            List<Equipe> equipes, List<Usuario> responsaveis) {
        this.projeto = projeto;
        this.totalTarefas = totalTarefas;
        this.tarefasConcluidas = tarefasConcluidas;
        this.tarefasPendentes = tarefasPendentes;
        this.percentualConclusao = percentualConclusao;
        this.equipes = equipes;
        this.responsaveis = responsaveis;
    }

    public Projeto getProjeto() {
        return projeto;
    }

    public int getTotalTarefas() {
        return totalTarefas;
    }

    public int getTarefasConcluidas() {
        return tarefasConcluidas;
    }

    public int getTarefasPendentes() {
        return tarefasPendentes;
    }

    public double getPercentualConclusao() {
        return percentualConclusao;
    }

    public List<Equipe> getEquipes() {
        return equipes;
    }

    public List<Usuario> getResponsaveis() {
        return responsaveis;
    }

    /**
     * Retorna {@code true} se o projeto não possui nenhuma tarefa cadastrada,
     * indicando que os demais KPIs devem ser interpretados como indefinidos.
     *
     * @return {@code true} quando {@code totalTarefas == 0}
     */
    public boolean isSemTarefas() {
        return totalTarefas == 0;
    }
}
