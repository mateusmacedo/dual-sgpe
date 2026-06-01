package br.com.dual.sgpe.controller;

import br.com.dual.sgpe.dao.EquipeDao;
import br.com.dual.sgpe.dao.ProjetoDao;
import br.com.dual.sgpe.dao.ProjetoEquipeDao;
import br.com.dual.sgpe.dao.RelatorioDao;
import br.com.dual.sgpe.dao.UsuarioDao;
import br.com.dual.sgpe.exception.ValidacaoException;
import br.com.dual.sgpe.model.dto.RelatorioProjeto;
import br.com.dual.sgpe.model.entity.Equipe;
import br.com.dual.sgpe.model.entity.Projeto;
import br.com.dual.sgpe.model.entity.Usuario;
import java.util.List;

/**
 * Orquestra a geração do relatório de desempenho de um projeto: agrega dados de
 * tarefas (contagens e percentual de conclusão), equipes vinculadas e responsáveis
 * distintos, compondo o {@link RelatorioProjeto} para exibição na View.
 */
public class RelatorioController {

    private final ProjetoDao projetoDao;
    private final RelatorioDao relatorioDao;
    private final ProjetoEquipeDao projetoEquipeDao;
    private final EquipeDao equipeDao;
    private final UsuarioDao usuarioDao;

    public RelatorioController(ProjetoDao projetoDao, RelatorioDao relatorioDao,
                               ProjetoEquipeDao projetoEquipeDao, EquipeDao equipeDao,
                               UsuarioDao usuarioDao) {
        this.projetoDao = projetoDao;
        this.relatorioDao = relatorioDao;
        this.projetoEquipeDao = projetoEquipeDao;
        this.equipeDao = equipeDao;
        this.usuarioDao = usuarioDao;
    }

    /**
     * Gera o relatório de desempenho de um projeto. Contabiliza tarefas totais,
     * concluídas e pendentes via {@link RelatorioDao}, calcula o percentual de
     * conclusão (0,0 quando não há tarefas), e resolve as equipes e responsáveis
     * distintos associados ao projeto.
     *
     * @param projetoId id do projeto a analisar
     * @return DTO com métricas consolidadas, equipes e lista de responsáveis
     * @throws ValidacaoException se o projeto não for encontrado
     */
    public RelatorioProjeto gerar(int projetoId) {
        Projeto projeto = projetoDao.buscarPorId(projetoId)
            .orElseThrow(() -> new ValidacaoException("Projeto não encontrado."));

        int total = relatorioDao.contarTarefasPorProjeto(projetoId);
        int concluidas = relatorioDao.contarConcluidasPorProjeto(projetoId);
        int pendentes = relatorioDao.contarPendentesPorProjeto(projetoId);
        // RN: evita divisão por zero; projetos sem tarefas têm 0% de conclusão
        double percentual = total > 0 ? (concluidas * 100.0 / total) : 0.0;

        List<Equipe> equipes = equipeDao.listarPorProjeto(projetoId);
        List<Usuario> responsaveis = usuarioDao.listarResponsaveisPorProjeto(projetoId);

        return new RelatorioProjeto(projeto, total, concluidas, pendentes,
            percentual, equipes, responsaveis);
    }

    public List<Projeto> listarProjetos() {
        return projetoDao.listarTodos();
    }
}
