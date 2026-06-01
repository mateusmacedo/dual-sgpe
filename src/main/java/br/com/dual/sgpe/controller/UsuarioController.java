package br.com.dual.sgpe.controller;

import br.com.dual.sgpe.dao.UsuarioDao;
import br.com.dual.sgpe.exception.ExclusaoBloqueadaException;
import br.com.dual.sgpe.exception.RegistroDuplicadoException;
import br.com.dual.sgpe.exception.ValidacaoException;
import br.com.dual.sgpe.model.entity.Usuario;
import br.com.dual.sgpe.util.ValidationUtils;
import java.util.List;
import java.util.Optional;

/**
 * Coordena as operações de cadastro de usuários: valida os dados, garante as
 * regras de unicidade e integridade e delega a persistência ao {@link UsuarioDao}.
 *
 * <p>Não conhece a camada de apresentação: retorna dados e lança exceções de
 * negócio; cabe à View capturá-las e exibi-las.
 */
public class UsuarioController {

    private final UsuarioDao usuarioDao;

    public UsuarioController(UsuarioDao usuarioDao) {
        this.usuarioDao = usuarioDao;
    }

    /**
     * Valida os campos obrigatórios, normaliza o CPF para apenas dígitos e verifica
     * unicidade de login e CPF antes de persistir o novo usuário.
     *
     * @param usuario dados do usuário a ser criado
     * @return o usuário inserido (com id preenchido pelo DAO)
     * @throws ValidacaoException         se algum campo obrigatório for inválido
     * @throws RegistroDuplicadoException se login ou CPF já estiverem cadastrados
     */
    public Usuario salvar(Usuario usuario) {
        validar(usuario);
        // RN: armazena CPF sem formatação (somente dígitos) para comparação consistente
        usuario.setCpf(ValidationUtils.apenasDigitos(usuario.getCpf()));
        if (usuarioDao.existsByLogin(usuario.getLogin())) {
            throw new RegistroDuplicadoException("Login já cadastrado.");
        }
        if (usuarioDao.existsByCpf(usuario.getCpf())) {
            throw new RegistroDuplicadoException("CPF já cadastrado.");
        }
        usuarioDao.inserir(usuario);
        return usuario;
    }

    /**
     * Valida e persiste as alterações de um usuário existente. Verifica unicidade
     * de login e CPF excluindo o próprio registro da comparação (queries com {@code ExcetoId}).
     *
     * @param usuario usuário com id e novos dados
     * @return o usuário atualizado
     * @throws ValidacaoException         se o usuário for nulo ou não tiver id
     * @throws RegistroDuplicadoException se login ou CPF colidirem com outro registro
     */
    public Usuario atualizar(Usuario usuario) {
        if (usuario == null || usuario.getId() == null) {
            throw new ValidacaoException("Usuário sem id não pode ser atualizado.");
        }
        validar(usuario);
        // RN: normaliza CPF antes de checar duplicidade (evita falso positivo com formatação divergente)
        usuario.setCpf(ValidationUtils.apenasDigitos(usuario.getCpf()));
        // RN: exclui o próprio registro da verificação para permitir atualização sem trocar login/CPF
        if (usuarioDao.existsByLoginExcetoId(usuario.getLogin(), usuario.getId())) {
            throw new RegistroDuplicadoException("Login já cadastrado.");
        }
        if (usuarioDao.existsByCpfExcetoId(usuario.getCpf(), usuario.getId())) {
            throw new RegistroDuplicadoException("CPF já cadastrado.");
        }
        usuarioDao.atualizar(usuario);
        return usuario;
    }

    /**
     * Remove o usuário somente se não houver tarefas ou equipes vinculadas ao seu id.
     *
     * @param id identificador do usuário
     * @throws ExclusaoBloqueadaException se o usuário possuir vínculos ativos
     */
    public void excluir(int id) {
        if (usuarioDao.isReferenciado(id)) {
            throw new ExclusaoBloqueadaException(
                "Usuário não pode ser excluído: está vinculado a tarefas ou equipes.");
        }
        usuarioDao.excluir(id);
    }

    public List<Usuario> listarTodos() {
        return usuarioDao.listarTodos();
    }

    public Optional<Usuario> buscarPorId(int id) {
        return usuarioDao.buscarPorId(id);
    }

    private void validar(Usuario usuario) {
        if (usuario == null) {
            throw new ValidacaoException("Usuário não informado.");
        }
        if (ValidationUtils.isBlank(usuario.getNomeCompleto())) {
            throw new ValidacaoException("Nome completo é obrigatório.");
        }
        if (ValidationUtils.isBlank(usuario.getCpf())) {
            throw new ValidacaoException("CPF é obrigatório.");
        }
        if (!ValidationUtils.isCpfValido(usuario.getCpf())) {
            throw new ValidacaoException("CPF inválido.");
        }
        if (ValidationUtils.isBlank(usuario.getEmail())) {
            throw new ValidacaoException("E-mail é obrigatório.");
        }
        if (!ValidationUtils.isEmailValido(usuario.getEmail())) {
            throw new ValidacaoException("E-mail inválido.");
        }
        if (ValidationUtils.isBlank(usuario.getCargo())) {
            throw new ValidacaoException("Cargo é obrigatório.");
        }
        if (ValidationUtils.isBlank(usuario.getLogin())) {
            throw new ValidacaoException("Login é obrigatório.");
        }
        if (ValidationUtils.isBlank(usuario.getSenha())) {
            throw new ValidacaoException("Senha é obrigatória.");
        }
        if (usuario.getPerfil() == null) {
            throw new ValidacaoException("Perfil é obrigatório.");
        }
    }
}
