package br.com.dual.sgpe.controller;

import br.com.dual.sgpe.dao.ProjetoDao;
import br.com.dual.sgpe.dao.TarefaDao;
import br.com.dual.sgpe.dao.UsuarioDao;
import br.com.dual.sgpe.exception.ValidacaoException;
import br.com.dual.sgpe.model.entity.Projeto;
import br.com.dual.sgpe.model.entity.Tarefa;
import br.com.dual.sgpe.model.entity.Usuario;
import br.com.dual.sgpe.model.enums.PerfilUsuario;
import br.com.dual.sgpe.model.enums.StatusTarefa;
import br.com.dual.sgpe.security.EscopoColaborador;
import br.com.dual.sgpe.util.DateUtils;
import br.com.dual.sgpe.util.ValidationUtils;
import java.util.List;
import java.util.Optional;

/**
 * Orquestra o ciclo de vida das tarefas: valida dados, verifica a existência de
 * projeto e responsável, aplica o status padrão e delega a persistência ao
 * {@link TarefaDao}. Oferece também a transição de status sem revalidação completa.
 */
public class TarefaController {

    private final TarefaDao tarefaDao;
    private final ProjetoDao projetoDao;
    private final UsuarioDao usuarioDao;
    private final EscopoColaborador escopo;

    public TarefaController(TarefaDao tarefaDao, ProjetoDao projetoDao, UsuarioDao usuarioDao,
                            EscopoColaborador escopo) {
        this.tarefaDao = tarefaDao;
        this.projetoDao = projetoDao;
        this.usuarioDao = usuarioDao;
        this.escopo = escopo;
    }

    /**
     * Valida os campos obrigatórios, aplica o status padrão {@code PENDENTE} quando
     * ausente e persiste a nova tarefa. Impede que o solicitante designe a tarefa
     * a um responsável de perfil superior ao seu (hierarquia de perfis).
     *
     * @param tarefa      dados da tarefa a ser criada
     * @param solicitante perfil do usuário que dispara a operação
     * @return a tarefa inserida (com id preenchido pelo DAO)
     * @throws ValidacaoException se título, projeto, responsável ou datas forem inválidos,
     *                            ou se o solicitante não puder gerenciar o perfil do responsável
     */
    public Tarefa salvar(Tarefa tarefa, PerfilUsuario solicitante) {
        validar(tarefa);
        validarHierarquiaResponsavel(tarefa, solicitante);
        aplicarStatusPadrao(tarefa);
        tarefaDao.inserir(tarefa);
        return tarefa;
    }

    /**
     * Salva a tarefa verificando se o solicitante tem acesso ao projeto.
     */
    public Tarefa salvar(Tarefa tarefa, Usuario solicitante) {
        validar(tarefa);
        verificarEscopoProjeto(solicitante, tarefa.getProjetoId());
        validarHierarquiaResponsavel(tarefa, solicitante.getPerfil());
        aplicarStatusPadrao(tarefa);
        tarefaDao.inserir(tarefa);
        return tarefa;
    }

    /**
     * Valida e persiste as alterações em uma tarefa existente, revalidando
     * integridade referencial (projeto e responsável). Impede que o solicitante
     * designe a tarefa a um responsável de perfil superior ao seu (hierarquia de perfis).
     *
     * @param tarefa      tarefa com id e novos dados
     * @param solicitante perfil do usuário que dispara a operação
     * @return a tarefa atualizada
     * @throws ValidacaoException se a tarefa for nula ou não tiver id, ou se o
     *                            solicitante não puder gerenciar o perfil do responsável
     */
    public Tarefa atualizar(Tarefa tarefa, PerfilUsuario solicitante) {
        if (tarefa == null || tarefa.getId() == null) {
            throw new ValidacaoException("Tarefa sem id não pode ser atualizada.");
        }
        validar(tarefa);
        validarHierarquiaResponsavel(tarefa, solicitante);
        aplicarStatusPadrao(tarefa);
        tarefaDao.atualizar(tarefa);
        return tarefa;
    }

    /**
     * Atualiza a tarefa verificando se o solicitante tem acesso ao projeto.
     */
    public Tarefa atualizar(Tarefa tarefa, Usuario solicitante) {
        if (tarefa == null || tarefa.getId() == null) {
            throw new ValidacaoException("Tarefa sem id não pode ser atualizada.");
        }
        validar(tarefa);
        verificarEscopoProjeto(solicitante, tarefa.getProjetoId());
        validarHierarquiaResponsavel(tarefa, solicitante.getPerfil());
        aplicarStatusPadrao(tarefa);
        tarefaDao.atualizar(tarefa);
        return tarefa;
    }

    public void excluir(int id) {
        tarefaDao.excluir(id);
    }

    /**
     * Exclui a tarefa verificando se o solicitante tem acesso ao projeto.
     */
    public void excluir(int id, Usuario solicitante) {
        Tarefa tarefa = tarefaDao.buscarPorId(id)
            .orElseThrow(() -> new ValidacaoException("Tarefa não encontrada."));
        verificarEscopoProjeto(solicitante, tarefa.getProjetoId());
        tarefaDao.excluir(id);
    }

    /**
     * Altera apenas o status de uma tarefa existente, sem revalidar os demais campos.
     * Usado pela View para transições rápidas de estado (ex: PENDENTE → EM_ANDAMENTO).
     *
     * <p>Restringe a operação ao próprio responsável da tarefa: o colaborador só
     * pode atualizar o status das tarefas atribuídas a ele, nunca as de terceiros.
     *
     * @param id          id da tarefa
     * @param novoStatus  novo status a aplicar
     * @param solicitante usuário que dispara a operação (deve ser o responsável)
     * @return a tarefa com o status atualizado
     * @throws ValidacaoException se o status for nulo, a tarefa não for encontrada
     *                            ou o solicitante não for o responsável pela tarefa
     */
    public Tarefa atualizarStatus(int id, StatusTarefa novoStatus, Usuario solicitante) {
        if (novoStatus == null) {
            throw new ValidacaoException("Status é obrigatório.");
        }
        Tarefa tarefa = tarefaDao.buscarPorId(id)
            .orElseThrow(() -> new ValidacaoException("Tarefa não encontrada."));
        // RN: apenas o responsável pela tarefa pode atualizar o seu status
        if (tarefa.getResponsavelId() != solicitante.getId()) {
            throw new ValidacaoException(
                "Apenas o responsável pela tarefa pode atualizar o seu status.");
        }
        tarefa.setStatus(novoStatus);
        tarefaDao.atualizar(tarefa);
        return tarefa;
    }

    public Optional<Tarefa> buscarPorId(int id) {
        return tarefaDao.buscarPorId(id);
    }

    public List<Tarefa> findByProjetoId(int projetoId) {
        return tarefaDao.findByProjetoId(projetoId);
    }

    public List<Tarefa> findByResponsavelId(int responsavelId) {
        return tarefaDao.findByResponsavelId(responsavelId);
    }

    public List<Projeto> listarProjetos() {
        return projetoDao.listarTodos();
    }

    /**
     * Lista todos os usuários, sem filtro de hierarquia. Usado para resolução de
     * nomes de responsáveis em telas de leitura (consulta e detalhamento de projeto),
     * onde exibir o nome não constitui designação de tarefa.
     *
     * @return todos os usuários cadastrados
     */
    public List<Usuario> listarResponsaveis() {
        return usuarioDao.listarTodos();
    }

    /**
     * Lista os usuários que o solicitante pode designar como responsáveis,
     * filtrando aqueles cujo perfil seja superior ao do solicitante (hierarquia).
     *
     * @param solicitante perfil do usuário que dispara a consulta
     * @return usuários cujo perfil o solicitante pode gerenciar
     */
    public List<Usuario> listarResponsaveis(PerfilUsuario solicitante) {
        return listarResponsaveis().stream()
            .filter(usuario -> usuario.getPerfil().isDesignavel())
            .filter(usuario -> solicitante.podeGerenciar(usuario.getPerfil()))
            .toList();
    }

    private void validar(Tarefa tarefa) {
        if (tarefa == null) {
            throw new ValidacaoException("Tarefa não informada.");
        }
        if (ValidationUtils.isBlank(tarefa.getTitulo())) {
            throw new ValidacaoException("Título é obrigatório.");
        }
        // RN: verifica integridade referencial — projeto e responsável devem existir antes de persistir
        if (projetoDao.buscarPorId(tarefa.getProjetoId()).isEmpty()) {
            throw new ValidacaoException("Projeto não encontrado.");
        }
        if (usuarioDao.buscarPorId(tarefa.getResponsavelId()).isEmpty()) {
            throw new ValidacaoException("Responsável não encontrado.");
        }
        if (tarefa.getDataInicio() == null) {
            throw new ValidacaoException("Data de início é obrigatória.");
        }
        if (tarefa.getDataTerminoPrevista() == null) {
            throw new ValidacaoException("Data de término prevista é obrigatória.");
        }
        if (!DateUtils.naoAnterior(tarefa.getDataTerminoPrevista(), tarefa.getDataInicio())) {
            throw new ValidacaoException("Data de término anterior ao início.");
        }
    }

    private void aplicarStatusPadrao(Tarefa tarefa) {
        if (tarefa.getStatus() == null) {
            tarefa.setStatus(StatusTarefa.PENDENTE);
        }
    }

    /**
     * Verifica se o solicitante tem acesso ao projeto da tarefa.
     */
    public boolean podeAcessarProjeto(Usuario solicitante, int projetoId) {
        if (solicitante.getPerfil() == PerfilUsuario.ADMINISTRADOR) {
            return true;
        }
        return escopo.projetoIdsDoColaborador(solicitante.getId()).contains(projetoId);
    }

    private void verificarEscopoProjeto(Usuario solicitante, int projetoId) {
        if (!podeAcessarProjeto(solicitante, projetoId)) {
            throw new ValidacaoException("Sem permissão para alterar tarefa deste projeto.");
        }
    }

    private void validarHierarquiaResponsavel(Tarefa tarefa, PerfilUsuario solicitante) {
        Usuario responsavel = usuarioDao.buscarPorId(tarefa.getResponsavelId())
            .orElseThrow(() -> new ValidacaoException("Responsável não encontrado."));
        if (!responsavel.getPerfil().isDesignavel()) {
            throw new ValidacaoException(
                "Administrador não pode ser designado como responsável de tarefa.");
        }
        if (!solicitante.podeGerenciar(responsavel.getPerfil())) {
            throw new ValidacaoException(
                "Sem permissão para designar tarefa a um responsável de perfil superior.");
        }
    }
}
