package br.com.dual.sgpe.controller;

import br.com.dual.sgpe.dao.EquipeDao;
import br.com.dual.sgpe.dao.EquipeUsuarioDao;
import br.com.dual.sgpe.dao.ProjetoEquipeDao;
import br.com.dual.sgpe.dao.UsuarioDao;
import br.com.dual.sgpe.security.EscopoColaborador;
import br.com.dual.sgpe.exception.ExclusaoBloqueadaException;
import br.com.dual.sgpe.exception.RegistroDuplicadoException;
import br.com.dual.sgpe.exception.ValidacaoException;
import br.com.dual.sgpe.model.entity.Equipe;
import br.com.dual.sgpe.model.entity.Usuario;
import br.com.dual.sgpe.model.enums.PerfilUsuario;
import java.util.Set;
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
    private final ProjetoEquipeDao projetoEquipeDao;
    private final EscopoColaborador escopo;

    public EquipeController(EquipeDao equipeDao, EquipeUsuarioDao equipeUsuarioDao,
                            UsuarioDao usuarioDao, ProjetoEquipeDao projetoEquipeDao,
                            EscopoColaborador escopo) {
        this.equipeDao = equipeDao;
        this.equipeUsuarioDao = equipeUsuarioDao;
        this.usuarioDao = usuarioDao;
        this.projetoEquipeDao = projetoEquipeDao;
        this.escopo = escopo;
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
     * Atualiza a equipe verificando se o solicitante tem acesso ao seu escopo.
     */
    public Equipe atualizar(Equipe equipe, Usuario solicitante) {
        if (equipe == null || equipe.getId() == null) {
            throw new ValidacaoException("Equipe sem id não pode ser atualizada.");
        }
        verificarEscopo(solicitante, equipe.getId());
        return atualizar(equipe);
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
     * Exclui a equipe verificando se o solicitante tem acesso ao seu escopo.
     */
    public void excluir(int id, Usuario solicitante) {
        verificarEscopo(solicitante, id);
        excluir(id);
    }

    /**
     * Vincula um usuário a uma equipe, verificando a existência do usuário e
     * a ausência de vínculo duplicado antes de persistir. Impede que o solicitante
     * adicione um usuário de perfil superior ao seu (hierarquia de perfis).
     *
     * @param equipeId    id da equipe
     * @param usuarioId   id do usuário a adicionar
     * @param solicitante perfil do usuário que dispara a operação
     * @throws ValidacaoException         se o usuário não for encontrado ou se o
     *                                    solicitante não puder gerenciar seu perfil
     * @throws RegistroDuplicadoException se o usuário já for membro da equipe
     */
    public void adicionarMembro(int equipeId, int usuarioId, PerfilUsuario solicitante) {
        Usuario usuario = usuarioDao.buscarPorId(usuarioId)
            .orElseThrow(() -> new ValidacaoException("Usuário não encontrado."));
        if (!usuario.getPerfil().isDesignavel()) {
            throw new ValidacaoException(
                "Administrador não pode ser adicionado como membro de equipe.");
        }
        if (!solicitante.podeGerenciar(usuario.getPerfil())) {
            throw new ValidacaoException(
                "Sem permissão para adicionar à equipe um usuário de perfil superior.");
        }
        if (equipeUsuarioDao.existeVinculo(equipeId, usuarioId)) {
            throw new RegistroDuplicadoException("Usuário já é membro desta equipe.");
        }
        equipeUsuarioDao.vincular(equipeId, usuarioId);
    }

    /**
     * Adiciona membro verificando escopo do solicitante sobre a equipe.
     */
    public void adicionarMembro(int equipeId, int usuarioId, Usuario solicitante) {
        verificarEscopo(solicitante, equipeId);
        adicionarMembro(equipeId, usuarioId, solicitante.getPerfil());
    }

    public void removerMembro(int equipeId, int usuarioId) {
        equipeUsuarioDao.desvincular(equipeId, usuarioId);
    }

    /**
     * Remove membro verificando escopo do solicitante sobre a equipe.
     */
    public void removerMembro(int equipeId, int usuarioId, Usuario solicitante) {
        verificarEscopo(solicitante, equipeId);
        removerMembro(equipeId, usuarioId);
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

    /**
     * Lista os usuários que o solicitante pode adicionar a uma equipe,
     * filtrando aqueles cujo perfil seja superior ao do solicitante (hierarquia).
     *
     * @param solicitante perfil do usuário que dispara a consulta
     * @return usuários cujo perfil o solicitante pode gerenciar
     */
    public List<Usuario> listarUsuarios(PerfilUsuario solicitante) {
        return usuarioDao.listarTodos().stream()
            .filter(usuario -> usuario.getPerfil().isDesignavel())
            .filter(usuario -> solicitante.podeGerenciar(usuario.getPerfil()))
            .toList();
    }

    public Optional<Equipe> buscarPorId(int id) {
        return equipeDao.buscarPorId(id);
    }

    /**
     * Verifica se o solicitante tem acesso à equipe. Administradores acessam
     * todas; gerentes e colaboradores acessam equipes vinculadas a projetos
     * do seu escopo.
     */
    public boolean equipeNoEscopo(Usuario solicitante, int equipeId) {
        if (solicitante.getPerfil() == PerfilUsuario.ADMINISTRADOR) {
            return true;
        }
        Set<Integer> projetosPermitidos = escopo.projetoIdsDoColaborador(solicitante.getId());
        List<Integer> projetosDaEquipe = projetoEquipeDao.listarProjetoIds(equipeId);
        return projetosDaEquipe.stream().anyMatch(projetosPermitidos::contains);
    }

    private void verificarEscopo(Usuario solicitante, int equipeId) {
        if (!equipeNoEscopo(solicitante, equipeId)) {
            throw new ValidacaoException("Sem permissão para alterar esta equipe.");
        }
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
