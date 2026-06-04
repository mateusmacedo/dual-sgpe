package br.com.dual.sgpe.controller;

import br.com.dual.sgpe.dao.EquipeDao;
import br.com.dual.sgpe.dao.ProjetoDao;
import br.com.dual.sgpe.dao.ProjetoEquipeDao;
import br.com.dual.sgpe.exception.ExclusaoBloqueadaException;
import br.com.dual.sgpe.exception.RegistroDuplicadoException;
import br.com.dual.sgpe.exception.ValidacaoException;
import br.com.dual.sgpe.model.entity.Equipe;
import br.com.dual.sgpe.model.entity.Projeto;
import br.com.dual.sgpe.model.entity.Usuario;
import br.com.dual.sgpe.model.filter.ProjetoFiltro;
import br.com.dual.sgpe.model.enums.PerfilUsuario;
import br.com.dual.sgpe.model.enums.StatusProjeto;
import br.com.dual.sgpe.security.EscopoColaborador;
import br.com.dual.sgpe.util.DateUtils;
import br.com.dual.sgpe.util.ValidationUtils;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Coordena o cadastro de projetos: valida os dados, aplica o status padrão e
 * garante a integridade na exclusão, delegando a persistência ao
 * {@link ProjetoDao}. Não conhece a camada de apresentação.
 */
public class ProjetoController {

    private final ProjetoDao projetoDao;
    private final ProjetoEquipeDao projetoEquipeDao;
    private final EquipeDao equipeDao;
    private final EscopoColaborador escopo;

    public ProjetoController(ProjetoDao projetoDao, ProjetoEquipeDao projetoEquipeDao,
                             EquipeDao equipeDao, EscopoColaborador escopo) {
        this.projetoDao = projetoDao;
        this.projetoEquipeDao = projetoEquipeDao;
        this.equipeDao = equipeDao;
        this.escopo = escopo;
    }

    /**
     * Valida os campos obrigatórios, aplica o status padrão {@code PLANEJADO} quando
     * ausente e persiste o novo projeto.
     *
     * @param projeto dados do projeto a ser criado
     * @return o projeto inserido (com id preenchido pelo DAO)
     * @throws ValidacaoException se nome, datas forem inválidos ou término anterior ao início
     */
    public Projeto salvar(Projeto projeto) {
        validar(projeto);
        aplicarStatusPadrao(projeto);
        projetoDao.inserir(projeto);
        return projeto;
    }

    /**
     * Valida e persiste as alterações em um projeto existente, reaplicando o
     * status padrão caso o campo tenha sido removido.
     *
     * @param projeto projeto com id e novos dados
     * @return o projeto atualizado
     * @throws ValidacaoException se o projeto for nulo ou não tiver id
     */
    public Projeto atualizar(Projeto projeto) {
        if (projeto == null || projeto.getId() == null) {
            throw new ValidacaoException("Projeto sem id não pode ser atualizado.");
        }
        validar(projeto);
        aplicarStatusPadrao(projeto);
        projetoDao.atualizar(projeto);
        return projeto;
    }

    /**
     * Atualiza o projeto verificando se o solicitante tem acesso ao seu escopo.
     */
    public Projeto atualizar(Projeto projeto, Usuario solicitante) {
        if (projeto == null || projeto.getId() == null) {
            throw new ValidacaoException("Projeto sem id não pode ser atualizado.");
        }
        verificarEscopo(solicitante, projeto.getId());
        return atualizar(projeto);
    }

    /**
     * Remove o projeto somente se não houver tarefas ou equipes vinculadas.
     *
     * @param id identificador do projeto
     * @throws ExclusaoBloqueadaException se existirem dependências no banco
     */
    public void excluir(int id) {
        if (projetoDao.isReferenciado(id)) {
            throw new ExclusaoBloqueadaException(
                "Projeto não pode ser excluído: há tarefas ou equipes vinculadas.");
        }
        projetoDao.excluir(id);
    }

    /**
     * Exclui o projeto verificando se o solicitante tem acesso ao seu escopo.
     */
    public void excluir(int id, Usuario solicitante) {
        verificarEscopo(solicitante, id);
        excluir(id);
    }

    public List<Projeto> listarTodos() {
        return projetoDao.listarTodos();
    }

    public List<Projeto> listarPorStatus(StatusProjeto status) {
        if (status == null) {
            return projetoDao.listarTodos();
        }
        return projetoDao.findByStatus(status);
    }

    public Optional<Projeto> buscarPorId(int id) {
        return projetoDao.buscarPorId(id);
    }

    /**
     * Consulta projetos aplicando os critérios do filtro (nome, status, intervalo de datas).
     * Delega a construção da query dinâmica ao {@link ProjetoDao}.
     *
     * @param filtro critérios de busca; campos nulos são ignorados
     * @return lista de projetos que satisfazem o filtro
     */
    public List<Projeto> consultar(ProjetoFiltro filtro) {
        return projetoDao.consultar(filtro);
    }

    /**
     * Consulta projetos aplicando o filtro e, quando o solicitante não é
     * {@link PerfilUsuario#ADMINISTRADOR}, restringe o resultado ao escopo de
     * projetos das equipes do solicitante. Apenas o administrador tem visão
     * irrestrita.
     *
     * @param filtro      critérios de busca; campos nulos são ignorados
     * @param solicitante usuário autenticado que dispara a consulta
     * @return lista de projetos visíveis ao solicitante conforme seu perfil
     */
    public List<Projeto> consultar(ProjetoFiltro filtro, Usuario solicitante) {
        List<Projeto> resultado = consultar(filtro);
        if (solicitante.getPerfil() != PerfilUsuario.ADMINISTRADOR) {
            Set<Integer> permitidos = escopo.projetoIdsDoColaborador(solicitante.getId());
            resultado = resultado.stream()
                .filter(projeto -> permitidos.contains(projeto.getId()))
                .toList();
        }
        return resultado;
    }

    /**
     * Vincula uma equipe a um projeto após validar a existência de ambos e a
     * ausência de vínculo duplicado.
     *
     * @param projetoId id do projeto
     * @param equipeId  id da equipe
     * @throws ValidacaoException         se o projeto ou a equipe não existirem
     * @throws RegistroDuplicadoException se o vínculo já existir
     */
    public void vincularEquipe(int projetoId, int equipeId) {
        if (projetoDao.buscarPorId(projetoId).isEmpty()) {
            throw new ValidacaoException("Projeto não encontrado.");
        }
        if (equipeDao.buscarPorId(equipeId).isEmpty()) {
            throw new ValidacaoException("Equipe não encontrada.");
        }
        if (projetoEquipeDao.existeVinculo(projetoId, equipeId)) {
            throw new RegistroDuplicadoException("Equipe já vinculada a este projeto.");
        }
        projetoEquipeDao.vincular(projetoId, equipeId);
    }

    public void vincularEquipe(int projetoId, int equipeId, Usuario solicitante) {
        verificarEscopo(solicitante, projetoId);
        vincularEquipe(projetoId, equipeId);
    }

    public void desvincularEquipe(int projetoId, int equipeId) {
        projetoEquipeDao.desvincular(projetoId, equipeId);
    }

    public void desvincularEquipe(int projetoId, int equipeId, Usuario solicitante) {
        verificarEscopo(solicitante, projetoId);
        desvincularEquipe(projetoId, equipeId);
    }

    public List<Equipe> listarEquipesDoProjeto(int projetoId) {
        return equipeDao.listarPorProjeto(projetoId);
    }

    public List<Projeto> listarProjetosDaEquipe(int equipeId) {
        return projetoDao.listarPorEquipe(equipeId);
    }

    public List<Equipe> listarEquipes() {
        return equipeDao.listarTodos();
    }

    /**
     * Verifica se o solicitante tem acesso ao projeto. Administradores acessam
     * todos; gerentes e colaboradores acessam apenas projetos das suas equipes.
     *
     * @param solicitante usuário que dispara a operação
     * @param projetoId   id do projeto a verificar
     * @return {@code true} se o solicitante pode acessar o projeto
     */
    public boolean podeAcessarProjeto(Usuario solicitante, int projetoId) {
        if (solicitante.getPerfil() == PerfilUsuario.ADMINISTRADOR) {
            return true;
        }
        return escopo.projetoIdsDoColaborador(solicitante.getId()).contains(projetoId);
    }

    private void verificarEscopo(Usuario solicitante, int projetoId) {
        if (!podeAcessarProjeto(solicitante, projetoId)) {
            throw new ValidacaoException("Sem permissão para alterar este projeto.");
        }
    }

    private void validar(Projeto projeto) {
        if (projeto == null) {
            throw new ValidacaoException("Projeto não informado.");
        }
        if (ValidationUtils.isBlank(projeto.getNome())) {
            throw new ValidacaoException("Nome é obrigatório.");
        }
        if (projeto.getDataInicio() == null) {
            throw new ValidacaoException("Data de início é obrigatória.");
        }
        if (projeto.getDataTerminoPrevista() == null) {
            throw new ValidacaoException("Data de término prevista é obrigatória.");
        }
        // RN: término previsto deve ser igual ou posterior ao início (aceita mesmo dia)
        if (!DateUtils.naoAnterior(projeto.getDataTerminoPrevista(), projeto.getDataInicio())) {
            throw new ValidacaoException("Data de término anterior ao início.");
        }
    }

    private void aplicarStatusPadrao(Projeto projeto) {
        if (projeto.getStatus() == null) {
            projeto.setStatus(StatusProjeto.PLANEJADO);
        }
    }
}
