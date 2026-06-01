package br.com.dual.sgpe.security;

import br.com.dual.sgpe.dao.EquipeUsuarioDao;
import br.com.dual.sgpe.dao.ProjetoDao;
import br.com.dual.sgpe.dao.ProjetoEquipeDao;
import br.com.dual.sgpe.model.entity.Projeto;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolve o escopo de dados visível a um Colaborador: os projetos das equipes
 * das quais ele participa (PRD §11.2). Reusa os DAOs de vínculo
 * (equipe_usuario, projeto_equipe) e o {@link ProjetoDao} para hidratar as
 * entidades.
 *
 * <p>Isolado da camada de view para ser testável sem Swing; reusado por
 * Consulta e Relatório.
 */
public class EscopoColaborador {

    private final EquipeUsuarioDao equipeUsuarioDao;
    private final ProjetoEquipeDao projetoEquipeDao;
    private final ProjetoDao projetoDao;

    public EscopoColaborador(EquipeUsuarioDao equipeUsuarioDao,
                             ProjetoEquipeDao projetoEquipeDao,
                             ProjetoDao projetoDao) {
        this.equipeUsuarioDao = equipeUsuarioDao;
        this.projetoEquipeDao = projetoEquipeDao;
        this.projetoDao = projetoDao;
    }

    /**
     * IDs dos projetos vinculados às equipes do colaborador, sem duplicatas.
     * Útil quando apenas os identificadores bastam (ex.: filtro de consulta).
     */
    public Set<Integer> projetoIdsDoColaborador(int usuarioId) {
        Set<Integer> projetoIds = new LinkedHashSet<>();
        for (int equipeId : equipeUsuarioDao.listarEquipeIdsDoUsuario(usuarioId)) {
            projetoIds.addAll(projetoEquipeDao.listarProjetoIds(equipeId));
        }
        return projetoIds;
    }

    /**
     * Projetos do escopo do colaborador, ordenados por id. Resolve o conjunto
     * de ids permitidos e delega ao {@link ProjetoDao#listarPorIds(java.util.Collection)},
     * que busca todos os projetos numa única consulta com cláusula {@code IN}.
     */
    public List<Projeto> projetosDoColaborador(int usuarioId) {
        Set<Integer> ids = projetoIdsDoColaborador(usuarioId);
        return projetoDao.listarPorIds(ids);
    }
}
