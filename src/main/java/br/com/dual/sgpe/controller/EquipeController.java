package br.com.dual.sgpe.controller;

import br.com.dual.sgpe.dao.EquipeDao;
import br.com.dual.sgpe.dao.EquipeUsuarioDao;
import br.com.dual.sgpe.dao.UsuarioDao;
import br.com.dual.sgpe.exception.ExclusaoBloqueadaException;
import br.com.dual.sgpe.exception.RegistroDuplicadoException;
import br.com.dual.sgpe.exception.ValidacaoException;
import br.com.dual.sgpe.model.entity.Equipe;
import br.com.dual.sgpe.model.entity.Usuario;
import br.com.dual.sgpe.util.ValidationUtils;
import java.util.List;
import java.util.Optional;

/**
 * Orquestra as operações de equipes: valida dados, gerencia o vínculo
 * n:n entre equipe e usuários via {@link EquipeUsuarioDao} e garante a
 * integridade referencial antes de excluir, delegando a persistência ao
 * {@link EquipeDao}.
 */
public class EquipeController {

    private final EquipeDao equipeDao;
    private final EquipeUsuarioDao equipeUsuarioDao;
    private final UsuarioDao usuarioDao;

    public EquipeController(EquipeDao equipeDao, EquipeUsuarioDao equipeUsuarioDao,
                            UsuarioDao usuarioDao) {
        this.equipeDao = equipeDao;
        this.equipeUsuarioDao = equipeUsuarioDao;
        this.usuarioDao = usuarioDao;
    }

    /**
     * Valida os campos obrigatórios e persiste uma nova equipe.
     *
     * @param equipe dados da equipe a ser criada
     * @return a equipe inserida (com id preenchido pelo DAO)
     * @throws ValidacaoException se o nome estiver em branco
     */
    public Equipe salvar(Equipe equipe) {
        validar(equipe);
        equipeDao.inserir(equipe);
        return equipe;
    }

    /**
     * Valida e persiste as alterações em uma equipe existente.
     *
     * @param equipe equipe com id e novos dados
     * @return a equipe atualizada
     * @throws ValidacaoException se a equipe for nula ou não tiver id
     */
    public Equipe atualizar(Equipe equipe) {
        if (equipe == null || equipe.getId() == null) {
            throw new ValidacaoException("Equipe sem id não pode ser atualizada.");
        }
        validar(equipe);
        equipeDao.atualizar(equipe);
        return equipe;
    }

    /**
     * Remove a equipe somente se não houver projetos vinculados. Remove primeiro
     * todos os membros da equipe (tabela intermediária) antes de excluir o registro.
     *
     * @param id identificador da equipe
     * @throws ExclusaoBloqueadaException se existirem projetos vinculados à equipe
     */
    public void excluir(int id) {
        if (equipeDao.isReferenciado(id)) {
            throw new ExclusaoBloqueadaException(
                "Equipe não pode ser excluída: há projetos vinculados.");
        }
        equipeUsuarioDao.removerTodosDaEquipe(id);
        equipeDao.excluir(id);
    }

    /**
     * Vincula um usuário a uma equipe, verificando a existência do usuário e
     * a ausência de vínculo duplicado antes de persistir.
     *
     * @param equipeId  id da equipe
     * @param usuarioId id do usuário a adicionar
     * @throws ValidacaoException         se o usuário não for encontrado
     * @throws RegistroDuplicadoException se o usuário já for membro da equipe
     */
    public void adicionarMembro(int equipeId, int usuarioId) {
        if (usuarioDao.buscarPorId(usuarioId).isEmpty()) {
            throw new ValidacaoException("Usuário não encontrado.");
        }
        if (equipeUsuarioDao.existeVinculo(equipeId, usuarioId)) {
            throw new RegistroDuplicadoException("Usuário já é membro desta equipe.");
        }
        equipeUsuarioDao.vincular(equipeId, usuarioId);
    }

    public void removerMembro(int equipeId, int usuarioId) {
        equipeUsuarioDao.desvincular(equipeId, usuarioId);
    }

    /**
     * Retorna os usuários membros da equipe, resolvendo os ids via {@link UsuarioDao}.
     *
     * @param equipeId id da equipe
     * @return lista de usuários membros; vazia se a equipe não tiver membros
     */
    public List<Usuario> listarMembros(int equipeId) {
        return usuarioDao.listarPorEquipe(equipeId);
    }

    public List<Equipe> listarTodos() {
        return equipeDao.listarTodos();
    }

    public List<Usuario> listarUsuarios() {
        return usuarioDao.listarTodos();
    }

    public Optional<Equipe> buscarPorId(int id) {
        return equipeDao.buscarPorId(id);
    }

    private void validar(Equipe equipe) {
        if (equipe == null) {
            throw new ValidacaoException("Equipe não informada.");
        }
        if (ValidationUtils.isBlank(equipe.getNome())) {
            throw new ValidacaoException("Nome é obrigatório.");
        }
    }
}
