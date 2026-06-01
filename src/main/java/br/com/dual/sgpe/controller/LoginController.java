package br.com.dual.sgpe.controller;

import br.com.dual.sgpe.config.AppConfig;
import br.com.dual.sgpe.dao.UsuarioDao;
import br.com.dual.sgpe.exception.AutenticacaoException;
import br.com.dual.sgpe.model.entity.Usuario;

/**
 * Orquestra a autenticação de usuários. Valida as credenciais contra a base e,
 * em caso de sucesso, registra o usuário autenticado na sessão ({@link AppConfig}).
 *
 * <p>Independente de Swing — pode ser testado isoladamente.
 */
public class LoginController {

    // Mensagem genérica idêntica para login inexistente e senha incorreta — evita enumeração de usuários
    private static final String CREDENCIAIS_INVALIDAS = "Credenciais inválidas";

    private final UsuarioDao usuarioDao;

    public LoginController(UsuarioDao usuarioDao) {
        this.usuarioDao = usuarioDao;
    }

    /**
     * Autentica pelo login e senha. Em qualquer falha (login inexistente ou
     * senha incorreta) lança {@link AutenticacaoException} com a mesma mensagem
     * genérica, evitando enumeração de usuários. A comparação de senha é
     * case-sensitive (segurança básica, RNF007).
     *
     * @return o usuário autenticado, também registrado em {@link AppConfig}
     */
    public Usuario autenticar(String login, String senha) {
        Usuario usuario = usuarioDao.buscarPorLogin(login)
            .orElseThrow(() -> new AutenticacaoException(CREDENCIAIS_INVALIDAS));
        // Comparação case-sensitive (RNF007); senha armazenada em texto simples nesta versão
        if (!usuario.getSenha().equals(senha)) {
            throw new AutenticacaoException(CREDENCIAIS_INVALIDAS);
        }
        AppConfig.getInstance().setUsuarioAutenticado(usuario);
        return usuario;
    }
}
