package br.com.dual.sgpe.view;

import br.com.dual.sgpe.controller.EquipeController;
import br.com.dual.sgpe.exception.ExclusaoBloqueadaException;
import br.com.dual.sgpe.exception.RegistroDuplicadoException;
import br.com.dual.sgpe.exception.ValidacaoException;
import br.com.dual.sgpe.model.entity.Equipe;
import br.com.dual.sgpe.model.entity.Usuario;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/**
 * Tela principal de cadastro de equipes do SGPE.
 *
 * <p>Permite criar, editar e excluir equipes, além de gerenciar membros via
 * diálogo modal. Delega toda a lógica de negócio ao {@link EquipeController};
 * nunca acessa o DAO diretamente.
 */
public class EquipeView extends JFrame {

    private final transient EquipeController controller;

    private final JTextField campoNome = new JTextField(24);
    private final JTextField campoDescricao = new JTextField(24);

    // Tabela somente-leitura — impede edição direta na célula
    private final DefaultTableModel tableModel = SwingUtils.modeloSomenteLeitura(
        new Object[] {"ID", "Nome", "Descrição"});
    private final JTable tabela = new JTable(tableModel);

    /** ID da equipe atualmente selecionada na tabela; {@code null} indica modo inserção. */
    private Integer equipeSelecionadaId;

    /**
     * Cria e exibe a janela de equipes.
     *
     * @param controller controller que processa as operações CRUD e consultas de membros
     */
    public EquipeView(EquipeController controller) {
        super("Cadastro de Equipes — SGPE");
        this.controller = controller;
        configurarJanela();
        recarregarTabela();
    }

    private void configurarJanela() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 560);
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
     * Monta o painel de formulário usando {@link GridBagLayout} com dois grupos
     * de constraints reutilizados: {@code rotulo} (coluna 0, alinhado à esquerda)
     * e {@code campo} (coluna 1, expansível horizontalmente).
     */
    private JPanel construirFormulario() {
        JPanel painel = new JPanel(new GridBagLayout());

        // Constraints para a coluna de rótulos — fixos à esquerda, sem expansão
        GridBagConstraints rotulo = new GridBagConstraints();
        rotulo.gridx = 0;
        rotulo.anchor = GridBagConstraints.WEST;
        rotulo.insets = new Insets(6, 0, 6, 12);

        // Constraints para a coluna de campos — expandem para preencher a largura disponível
        GridBagConstraints campo = new GridBagConstraints();
        campo.gridx = 1;
        campo.weightx = 1.0;
        campo.fill = GridBagConstraints.HORIZONTAL;
        campo.insets = new Insets(6, 0, 6, 0);

        adicionarLinha(painel, rotulo, campo, 0, "Nome:", campoNome);
        adicionarLinha(painel, rotulo, campo, 1, "Descrição:", campoDescricao);
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
        JButton membros = new JButton("Membros");
        novo.addActionListener(evento -> limparFormulario());
        salvar.addActionListener(evento -> salvar());
        excluir.addActionListener(evento -> excluir());
        membros.addActionListener(evento -> abrirDialogoMembros());
        painel.add(novo);
        painel.add(salvar);
        painel.add(excluir);
        painel.add(membros);
        return painel;
    }

    private void salvar() {
        try {
            Equipe equipe = new Equipe(campoNome.getText().trim(), campoDescricao.getText().trim());
            if (equipeSelecionadaId == null) {
                controller.salvar(equipe);
                SwingUtils.exibirInformacao(this, "Equipe cadastrada com sucesso.");
            } else {
                equipe.setId(equipeSelecionadaId);
                controller.atualizar(equipe);
                SwingUtils.exibirInformacao(this, "Equipe atualizada com sucesso.");
            }
            limparFormulario();
            recarregarTabela();
        } catch (ValidacaoException excecao) {
            SwingUtils.exibirErro(this, excecao.getMessage());
        }
    }

    private void excluir() {
        if (equipeSelecionadaId == null) {
            SwingUtils.exibirErro(this, "Selecione uma equipe na tabela para excluir.");
            return;
        }
        int opcao = JOptionPane.showConfirmDialog(this,
            "Confirma a exclusão da equipe selecionada?", "Excluir", JOptionPane.YES_NO_OPTION);
        if (opcao != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            controller.excluir(equipeSelecionadaId);
            SwingUtils.exibirInformacao(this, "Equipe excluída com sucesso.");
            limparFormulario();
            recarregarTabela();
        } catch (ExclusaoBloqueadaException excecao) {
            SwingUtils.exibirErro(this, excecao.getMessage());
        }
    }

    /**
     * Abre o diálogo modal de gerenciamento de membros da equipe selecionada.
     *
     * <p>O diálogo exibe a lista atual de membros e um combo com os usuários
     * disponíveis para adição (excluídos os já membros). Um {@code Runnable}
     * local {@code recarregar} sincroniza ambas as estruturas após cada operação.
     */
    private void abrirDialogoMembros() {
        if (equipeSelecionadaId == null) {
            SwingUtils.exibirErro(this, "Selecione uma equipe na tabela para gerenciar membros.");
            return;
        }
        String nomeEquipe = campoNome.getText().trim();
        JDialog dialogo = new JDialog(this, "Membros — " + nomeEquipe, true);
        dialogo.setSize(560, 400);
        dialogo.setLocationRelativeTo(this);
        JPanel painelRaiz = new JPanel(new BorderLayout(8, 8));
        painelRaiz.setBorder(new EmptyBorder(12, 12, 12, 12));
        dialogo.setContentPane(painelRaiz);

        // Modelo somente-leitura para a grade de membros
        DefaultTableModel modeloMembros = SwingUtils.modeloSomenteLeitura(
            new Object[] {"ID", "Nome Completo"});
        JTable tabelaMembros = new JTable(modeloMembros);
        tabelaMembros.setRowHeight(24);

        JComboBox<Usuario> comboUsuarios = new JComboBox<>();
        // Renderer com pattern matching (instanceof Usuario) para exibir nomeCompleto no combo
        comboUsuarios.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Usuario usuario) {
                    setText(usuario.getNomeCompleto());
                }
                return this;
            }
        });

        // Recarrega tabela de membros e exclui do combo os usuários já vinculados
        Runnable recarregar = () -> {
            modeloMembros.setRowCount(0);
            List<Usuario> membros = controller.listarMembros(equipeSelecionadaId);
            for (Usuario m : membros) {
                modeloMembros.addRow(new Object[] {m.getId(), m.getNomeCompleto()});
            }
            Set<Integer> membroIds = membros.stream()
                .map(Usuario::getId).collect(Collectors.toSet());
            DefaultComboBoxModel<Usuario> modelo = new DefaultComboBoxModel<>();
            for (Usuario u : controller.listarUsuarios()) {
                if (!membroIds.contains(u.getId())) {
                    modelo.addElement(u);
                }
            }
            comboUsuarios.setModel(modelo);
        };
        recarregar.run();

        JButton adicionar = new JButton("Adicionar");
        adicionar.addActionListener(evento -> {
            Usuario selecionado = (Usuario) comboUsuarios.getSelectedItem();
            if (selecionado == null) {
                SwingUtils.exibirErro(this, "Selecione um usuário para adicionar.");
                return;
            }
            try {
                controller.adicionarMembro(equipeSelecionadaId, selecionado.getId());
                recarregar.run();
            } catch (ValidacaoException | RegistroDuplicadoException ex) {
                SwingUtils.exibirErro(this, ex.getMessage());
            }
        });

        JButton remover = new JButton("Remover");
        remover.addActionListener(evento -> {
            int linha = tabelaMembros.getSelectedRow();
            if (linha < 0) {
                SwingUtils.exibirErro(this, "Selecione um membro na tabela para remover.");
                return;
            }
            int usuarioId = (int) modeloMembros.getValueAt(linha, 0);
            controller.removerMembro(equipeSelecionadaId, usuarioId);
            recarregar.run();
        });

        JButton fechar = new JButton("Fechar");
        fechar.addActionListener(evento -> dialogo.dispose());

        JPanel painelAcoes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        painelAcoes.add(comboUsuarios);
        painelAcoes.add(adicionar);
        painelAcoes.add(remover);
        painelAcoes.add(fechar);

        dialogo.add(new JScrollPane(tabelaMembros), BorderLayout.CENTER);
        dialogo.add(painelAcoes, BorderLayout.SOUTH);

        SwingUtils.aplicarFonte(dialogo.getContentPane(), SwingUtils.FONTE_BASE);
        dialogo.setVisible(true);
    }

    private void carregarSelecionado() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) {
            return;
        }
        int id = (int) tableModel.getValueAt(linha, 0);
        controller.buscarPorId(id).ifPresent(equipe -> {
            equipeSelecionadaId = equipe.getId();
            campoNome.setText(equipe.getNome());
            campoDescricao.setText(equipe.getDescricao());
        });
    }

    private void recarregarTabela() {
        tableModel.setRowCount(0);
        for (Equipe equipe : controller.listarTodos()) {
            tableModel.addRow(new Object[] {
                equipe.getId(),
                equipe.getNome(),
                equipe.getDescricao()
            });
        }
    }

    private void limparFormulario() {
        equipeSelecionadaId = null;
        tabela.clearSelection();
        campoNome.setText("");
        campoDescricao.setText("");
    }
}
