package br.com.dual.sgpe.controller;

import br.com.dual.sgpe.dao.ProjetoDao;
import br.com.dual.sgpe.dao.TarefaDao;
import br.com.dual.sgpe.dao.UsuarioDao;
import br.com.dual.sgpe.exception.ValidacaoException;
import br.com.dual.sgpe.model.entity.Projeto;
import br.com.dual.sgpe.model.entity.Tarefa;
import br.com.dual.sgpe.model.entity.Usuario;
import br.com.dual.sgpe.model.enums.StatusTarefa;
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

    public TarefaController(TarefaDao tarefaDao, ProjetoDao projetoDao, UsuarioDao usuarioDao) {
        this.tarefaDao = tarefaDao;
        this.projetoDao = projetoDao;
        this.usuarioDao = usuarioDao;
    }

    /**
     * Valida os campos obrigatórios, aplica o status padrão {@code PENDENTE} quando
     * ausente e persiste a nova tarefa.
     *
     * @param tarefa dados da tarefa a ser criada
     * @return a tarefa inserida (com id preenchido pelo DAO)
     * @throws ValidacaoException se título, projeto, responsável ou datas forem inválidos
     */
    public Tarefa salvar(Tarefa tarefa) {
        validar(tarefa);
        aplicarStatusPadrao(tarefa);
        tarefaDao.inserir(tarefa);
        return tarefa;
    }

    /**
     * Valida e persiste as alterações em uma tarefa existente, revalidando
     * integridade referencial (projeto e responsável).
     *
     * @param tarefa tarefa com id e novos dados
     * @return a tarefa atualizada
     * @throws ValidacaoException se a tarefa for nula ou não tiver id
     */
    public Tarefa atualizar(Tarefa tarefa) {
        if (tarefa == null || tarefa.getId() == null) {
            throw new ValidacaoException("Tarefa sem id não pode ser atualizada.");
        }
        validar(tarefa);
        aplicarStatusPadrao(tarefa);
        tarefaDao.atualizar(tarefa);
        return tarefa;
    }

    public void excluir(int id) {
        tarefaDao.excluir(id);
    }

    /**
     * Altera apenas o status de uma tarefa existente, sem revalidar os demais campos.
     * Usado pela View para transições rápidas de estado (ex: PENDENTE → EM_ANDAMENTO).
     *
     * @param id         id da tarefa
     * @param novoStatus novo status a aplicar
     * @return a tarefa com o status atualizado
     * @throws ValidacaoException se o status for nulo ou a tarefa não for encontrada
     */
    public Tarefa atualizarStatus(int id, StatusTarefa novoStatus) {
        if (novoStatus == null) {
            throw new ValidacaoException("Status é obrigatório.");
        }
        Tarefa tarefa = tarefaDao.buscarPorId(id)
            .orElseThrow(() -> new ValidacaoException("Tarefa não encontrada."));
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

    public List<Usuario> listarResponsaveis() {
        return usuarioDao.listarTodos();
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
}
