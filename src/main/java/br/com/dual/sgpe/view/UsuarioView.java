package br.com.dual.sgpe.view;

import br.com.dual.sgpe.controller.UsuarioController;
import br.com.dual.sgpe.exception.ExclusaoBloqueadaException;
import br.com.dual.sgpe.exception.RegistroDuplicadoException;
import br.com.dual.sgpe.exception.ValidacaoException;
import br.com.dual.sgpe.model.entity.Usuario;
import br.com.dual.sgpe.model.enums.PerfilUsuario;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/**
 * Tela de cadastro de usuários do SGPE.
 *
 * <p>Captura dados do formulário e encaminha ações ao {@link UsuarioController};
 * nunca acessa o DAO diretamente. Trata as exceções de negócio
 * ({@link br.com.dual.sgpe.exception.ValidacaoException},
 * {@link br.com.dual.sgpe.exception.RegistroDuplicadoException},
 * {@link br.com.dual.sgpe.exception.ExclusaoBloqueadaException})
 * exibindo mensagens via {@link JOptionPane}.
 */
public class UsuarioView extends JFrame {

    private final transient UsuarioController controller;

    private final JTextField campoNome = new JTextField(24);
    private final JTextField campoCpf = new JTextField(24);
    private final JTextField campoEmail = new JTextField(24);
    private final JTextField campoCargo = new JTextField(24);
    private final JTextField campoLogin = new JTextField(24);
    private final JPasswordField campoSenha = new JPasswordField(24);
    private final JComboBox<PerfilUsuario> campoPerfil = new JComboBox<>(PerfilUsuario.values());

    // Tabela somente-leitura — edição somente via formulário
    private final DefaultTableModel tableModel = SwingUtils.modeloSomenteLeitura(
        new Object[] {"ID", "Nome", "CPF", "E-mail", "Cargo", "Login", "Perfil"});
    private final JTable tabela = new JTable(tableModel);

    /** ID do usuário selecionado na tabela; {@code null} indica modo inserção. */
    private Integer usuarioSelecionadoId;

    /**
     * Cria e exibe a janela de cadastro de usuários.
     *
     * @param controller controller que processa as operações CRUD de usuário
     */
    public UsuarioView(UsuarioController controller) {
        super("Cadastro de Usuários — SGPE");
        this.controller = controller;
        configurarJanela();
        recarregarTabela();
    }

    private void configurarJanela() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 560);
        setLocationRelativeTo(null);

        JPanel conteudo = new JPanel(new BorderLayout(12, 12));
        conteudo.setBorder(new EmptyBorder(16, 16, 16, 16));
        setContentPane(conteudo);

        conteudo.add(construirFormulario(), BorderLayout.NORTH);
        conteudo.add(new JScrollPane(tabela), BorderLayout.CENTER);
        conteudo.add(construirBotoes(), BorderLayout.SOUTH);

        // Listener de seleção: getValueIsAdjusting() evita disparo duplo durante drag
        tabela.getSelectionModel().addListSelectionListener(evento -> {
            if (!evento.getValueIsAdjusting()) {
                carregarSelecionado();
            }
        });

        // Propaga a fonte base recursivamente a toda a hierarquia de componentes
        SwingUtils.aplicarFonte(conteudo, SwingUtils.FONTE_BASE);
        SwingUtils.configurarTabela(tabela, SwingUtils.FONTE_BASE);
    }

    /**
     * Monta o formulário de entrada usando {@link GridBagLayout} com dois grupos
     * de constraints reutilizados: rótulos na coluna 0 (fixos) e campos na
     * coluna 1 (expandem horizontalmente com {@code weightx = 1.0}).
     */
    private JPanel construirFormulario() {
        JPanel painel = new JPanel(new GridBagLayout());

        // Constraints para a coluna de rótulos — alinhados à esquerda, sem expansão
        GridBagConstraints rotulo = new GridBagConstraints();
        rotulo.gridx = 0;
        rotulo.anchor = GridBagConstraints.WEST;
        rotulo.insets = new Insets(6, 0, 6, 12);

        // Constraints para a coluna de campos — preenchem a largura restante da janela
        GridBagConstraints campo = new GridBagConstraints();
        campo.gridx = 1;
        campo.weightx = 1.0;
        campo.fill = GridBagConstraints.HORIZONTAL;
        campo.insets = new Insets(6, 0, 6, 0);

        int linha = 0;
        adicionarLinha(painel, rotulo, campo, linha++, "Nome completo:", campoNome);
        adicionarLinha(painel, rotulo, campo, linha++, "CPF:", campoCpf);
        adicionarLinha(painel, rotulo, campo, linha++, "E-mail:", campoEmail);
        adicionarLinha(painel, rotulo, campo, linha++, "Cargo:", campoCargo);
        adicionarLinha(painel, rotulo, campo, linha++, "Login:", campoLogin);
        adicionarLinha(painel, rotulo, campo, linha++, "Senha:", campoSenha);
        adicionarLinha(painel, rotulo, campo, linha, "Perfil:", campoPerfil);
        return painel;
    }

    private void adicionarLinha(JPanel painel, GridBagConstraints rotulo, GridBagConstraints campo,
                                int linha, String texto, JComponent componente) {
        rotulo.gridy = linha;
        campo.gridy = linha;
        painel.add(new JLabel(texto), rotulo);
        painel.add(componente, campo);
    }

    private JPanel construirBotoes() {
        JPanel painel = new JPanel();
        JButton novo = new JButton("Novo");
        JButton salvar = new JButton("Salvar");
        JButton excluir = new JButton("Excluir");
        novo.addActionListener(evento -> limparFormulario());
        salvar.addActionListener(evento -> salvar());
        excluir.addActionListener(evento -> excluir());
        painel.add(novo);
        painel.add(salvar);
        painel.add(excluir);
        return painel;
    }

    private void salvar() {
        try {
            Usuario usuario = lerFormulario();
            if (usuarioSelecionadoId == null) {
                controller.salvar(usuario);
                SwingUtils.exibirInformacao(this, "Usuário cadastrado com sucesso.");
            } else {
                usuario.setId(usuarioSelecionadoId);
                controller.atualizar(usuario);
                SwingUtils.exibirInformacao(this, "Usuário atualizado com sucesso.");
            }
            limparFormulario();
            recarregarTabela();
        } catch (ValidacaoException | RegistroDuplicadoException excecao) {
            SwingUtils.exibirErro(this, excecao.getMessage());
        }
    }

    private void excluir() {
        if (usuarioSelecionadoId == null) {
            SwingUtils.exibirErro(this, "Selecione um usuário na tabela para excluir.");
            return;
        }
        int opcao = JOptionPane.showConfirmDialog(this,
            "Confirma a exclusão do usuário selecionado?", "Excluir", JOptionPane.YES_NO_OPTION);
        if (opcao != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            controller.excluir(usuarioSelecionadoId);
            SwingUtils.exibirInformacao(this, "Usuário excluído com sucesso.");
            limparFormulario();
            recarregarTabela();
        } catch (ExclusaoBloqueadaException excecao) {
            SwingUtils.exibirErro(this, excecao.getMessage());
        }
    }

    /**
     * Lê os valores do formulário e constrói um {@link Usuario}.
     *
     * <p>{@code campoSenha.getPassword()} retorna {@code char[]} para minimizar
     * o tempo que a senha fica em memória como {@code String} imutável.
     */
    private Usuario lerFormulario() {
        return new Usuario(
            campoNome.getText().trim(),
            campoCpf.getText().trim(),
            campoEmail.getText().trim(),
            campoCargo.getText().trim(),
            campoLogin.getText().trim(),
            new String(campoSenha.getPassword()),
            (PerfilUsuario) campoPerfil.getSelectedItem()
        );
    }

    private void carregarSelecionado() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) {
            return;
        }
        int id = (int) tableModel.getValueAt(linha, 0);
        controller.buscarPorId(id).ifPresent(usuario -> {
            usuarioSelecionadoId = usuario.getId();
            campoNome.setText(usuario.getNomeCompleto());
            campoCpf.setText(usuario.getCpf());
            campoEmail.setText(usuario.getEmail());
            campoCargo.setText(usuario.getCargo());
            campoLogin.setText(usuario.getLogin());
            campoSenha.setText(usuario.getSenha());
            campoPerfil.setSelectedItem(usuario.getPerfil());
        });
    }

    private void recarregarTabela() {
        tableModel.setRowCount(0);
        List<Usuario> usuarios = controller.listarTodos();
        for (Usuario usuario : usuarios) {
            tableModel.addRow(new Object[] {
                usuario.getId(),
                usuario.getNomeCompleto(),
                usuario.getCpf(),
                usuario.getEmail(),
                usuario.getCargo(),
                usuario.getLogin(),
                usuario.getPerfil()
            });
        }
    }

    private void limparFormulario() {
        usuarioSelecionadoId = null;
        tabela.clearSelection();
        campoNome.setText("");
        campoCpf.setText("");
        campoEmail.setText("");
        campoCargo.setText("");
        campoLogin.setText("");
        campoSenha.setText("");
        campoPerfil.setSelectedIndex(0);
    }
}
