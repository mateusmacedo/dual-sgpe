package br.com.dual.sgpe.view;

import br.com.dual.sgpe.config.AppConfig;
import br.com.dual.sgpe.controller.EquipeController;
import br.com.dual.sgpe.controller.LoginController;
import br.com.dual.sgpe.controller.ProjetoController;
import br.com.dual.sgpe.controller.RelatorioController;
import br.com.dual.sgpe.controller.TarefaController;
import br.com.dual.sgpe.controller.UsuarioController;
import br.com.dual.sgpe.exception.AutenticacaoException;
import br.com.dual.sgpe.model.entity.Usuario;
import br.com.dual.sgpe.security.EscopoColaborador;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

/**
 * Tela de login. Coleta login e senha, delega a autenticação ao
 * {@link LoginController} e, no sucesso, abre a {@link MainView}. A senha é
 * lida via {@link JPasswordField} e nunca exibida.
 */
public class LoginView extends JFrame {

    private final transient LoginController controller;
    private final transient UsuarioController usuarioController;
    private final transient ProjetoController projetoController;
    private final transient TarefaController tarefaController;
    private final transient EquipeController equipeController;
    private final transient RelatorioController relatorioController;
    private final transient EscopoColaborador escopoColaborador;
    private final JTextField loginField = new JTextField(18);
    private final JPasswordField senhaField = new JPasswordField(18);

    /**
     * Inicializa a tela recebendo todos os controllers necessários para construir a
     * {@link MainView} após autenticação bem-sucedida. Os controllers são armazenados
     * e repassados como dependências; nenhuma lógica de negócio é executada no construtor.
     *
     * @param controller          responsável pela autenticação (hash de senha e lookup de usuário)
     * @param usuarioController   CRUD de usuários — repassado à MainView
     * @param projetoController   CRUD de projetos — repassado à MainView
     * @param tarefaController    CRUD de tarefas — repassado à MainView
     * @param equipeController    CRUD de equipes — repassado à MainView
     * @param relatorioController geração de relatórios — repassado à MainView
     * @param escopoColaborador   mapeamento de projetos/tarefas visíveis ao colaborador
     */
    public LoginView(LoginController controller,
                     UsuarioController usuarioController,
                     ProjetoController projetoController,
                     TarefaController tarefaController,
                     EquipeController equipeController,
                     RelatorioController relatorioController,
                     EscopoColaborador escopoColaborador) {
        super("SGPE — Login");
        this.controller = controller;
        this.usuarioController = usuarioController;
        this.projetoController = projetoController;
        this.tarefaController = tarefaController;
        this.equipeController = equipeController;
        this.relatorioController = relatorioController;
        this.escopoColaborador = escopoColaborador;
        configurarJanela();
    }

    private void configurarJanela() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(360, 200);
        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.LINE_START;

        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("Login:"), gbc);
        gbc.gridx = 1;
        add(loginField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        add(new JLabel("Senha:"), gbc);
        gbc.gridx = 1;
        add(senhaField, gbc);

        JButton entrar = new JButton("Entrar");
        entrar.addActionListener(evento -> autenticar());
        senhaField.addActionListener(evento -> autenticar());

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.LINE_END;
        add(entrar, gbc);
    }

    private void autenticar() {
        String login = loginField.getText();
        char[] senhaChars = senhaField.getPassword();
        String senha = new String(senhaChars);
        try {
            Usuario usuario = controller.autenticar(login, senha);
            senhaField.setText("");
            abrirPrincipal(usuario);
        } catch (AutenticacaoException excecao) {
            JOptionPane.showMessageDialog(this, excecao.getMessage(),
                "Falha na autenticação", JOptionPane.ERROR_MESSAGE);
            senhaField.setText("");
            senhaField.requestFocusInWindow();
        } catch (RuntimeException excecao) {
            JOptionPane.showMessageDialog(this,
                "Erro inesperado ao autenticar. Tente novamente.",
                "Erro", JOptionPane.ERROR_MESSAGE);
            senhaField.setText("");
            senhaField.requestFocusInWindow();
        } finally {
            // Zera o array de chars da senha na memória antes do GC, reduzindo
            // a janela de exposição em caso de heap dump.
            Arrays.fill(senhaChars, '\0');
        }
    }

    private void abrirPrincipal(Usuario usuario) {
        setVisible(false);
        new MainView(usuario, this::voltarParaLogin, usuarioController, projetoController,
            tarefaController, equipeController, relatorioController, escopoColaborador)
            .setVisible(true);
    }

    /**
     * Callback de logout: limpa o singleton {@link AppConfig} (conexão/estado global),
     * reseta os campos e reexibe a tela de login sem recriar a instância.
     */
    private void voltarParaLogin() {
        AppConfig.getInstance().limpar();
        loginField.setText("");
        senhaField.setText("");
        setVisible(true);
        loginField.requestFocusInWindow();
    }
}
