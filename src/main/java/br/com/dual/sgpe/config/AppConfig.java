package br.com.dual.sgpe.config;

import br.com.dual.sgpe.model.entity.Usuario;

/**
 * Configuração de sessão da aplicação. Singleton que mantém o usuário
 * autenticado disponível ao restante da aplicação durante a execução
 * (app desktop monousuário, em execução local).
 */
public final class AppConfig {

    private static final AppConfig INSTANCE = new AppConfig();

    private Usuario usuarioAutenticado;

    private AppConfig() {
    }

    /**
     * Retorna a instância única de {@code AppConfig}.
     * Inicializada eagerly no carregamento da classe; segura para acesso
     * concorrente sem sincronização adicional.
     *
     * @return instância Singleton de configuração da sessão
     */
    public static AppConfig getInstance() {
        return INSTANCE;
    }

    public Usuario getUsuarioAutenticado() {
        return usuarioAutenticado;
    }

    public void setUsuarioAutenticado(Usuario usuarioAutenticado) {
        this.usuarioAutenticado = usuarioAutenticado;
    }

    /**
     * Encerra a sessão do usuário atual, limpando o estado autenticado.
     * Deve ser chamado no logout para evitar que dados de sessão persistam
     * após o retorno à tela de login.
     */
    public void limpar() {
        this.usuarioAutenticado = null;
    }
}
