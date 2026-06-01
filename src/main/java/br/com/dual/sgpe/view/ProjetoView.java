package br.com.dual.sgpe.view;

import br.com.dual.sgpe.controller.ProjetoController;
import br.com.dual.sgpe.exception.ExclusaoBloqueadaException;
import br.com.dual.sgpe.exception.RegistroDuplicadoException;
import br.com.dual.sgpe.exception.ValidacaoException;
import br.com.dual.sgpe.model.entity.Equipe;
import br.com.dual.sgpe.model.entity.Projeto;
import br.com.dual.sgpe.model.enums.StatusProjeto;
import br.com.dual.sgpe.util.DateUtils;
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
import javax.swing.JDialog;
import javax.swing.JComponent;
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
 * Tela Swing de cadastro de projetos. Captura dados do formulário e encaminha
 * ações ao {@link ProjetoController}; trata as exceções de negócio exibindo
 * mensagens via {@link JOptionPane}. As datas são digitadas no formato ISO
 * (yyyy-MM-dd) e convertidas por {@link DateUtils}.
 */
public class ProjetoView extends JFrame {

    private static final String FILTRO_TODOS = "Todos";

    private final transient ProjetoController controller;

    private final JTextField campoNome = new JTextField(24);
    private final JTextField campoDescricao = new JTextField(24);
    private final JTextField campoDataInicio = new JTextField(24);
    private final JTextField campoDataTermino = new JTextField(24);
    private final JComboBox<StatusProjeto> campoStatus = new JComboBox<>(StatusProjeto.values());

    private final JComboBox<Object> filtroStatus = new JComboBox<>(modeloFiltro());

    // Tabela somente-leitura: edição inline travada; o preenchimento ocorre
    // exclusivamente pelo formulário acima da tabela.
    private final DefaultTableModel tableModel = SwingUtils.modeloSomenteLeitura(
        new Object[] {"ID", "Nome", "Descrição", "Início", "Término", "Status"});
    private final JTable tabela = new JTable(tableModel);

    private Integer projetoSelecionadoId;

    /**
     * Inicializa a tela com o controller de projetos, monta os componentes Swing
     * e carrega a listagem inicial na tabela.
     *
     * @param controller fornece as operações de CRUD e vínculo de equipes ao projeto
     */
    public ProjetoView(ProjetoController controller) {
        super("Cadastro de Projetos — SGPE");
        this.controller = controller;
        configurarJanela();
        recarregarTabela();
    }

    private static DefaultComboBoxModel<Object> modeloFiltro() {
        DefaultComboBoxModel<Object> modelo = new DefaultComboBoxModel<>();
        modelo.addElement(FILTRO_TODOS);
        for (StatusProjeto status : StatusProjeto.values()) {
            modelo.addElement(status);
        }
        return modelo;
    }

    private void configurarJanela() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(960, 600);
        setLocationRelativeTo(null);

        JPanel conteudo = new JPanel(new BorderLayout(12, 12));
        conteudo.setBorder(new EmptyBorder(16, 16, 16, 16));
        setContentPane(conteudo);

        conteudo.add(construirFormulario(), BorderLayout.NORTH);
        conteudo.add(construirCentro(), BorderLayout.CENTER);
        conteudo.add(construirBotoes(), BorderLayout.SOUTH);

        // ListSelectionListener: getValueIsAdjusting filtra eventos intermediários
        // do arrasto do mouse, garantindo que carregarSelecionado() execute apenas
        // ao final da seleção.
        tabela.getSelectionModel().addListSelectionListener(evento -> {
            if (!evento.getValueIsAdjusting()) {
                carregarSelecionado();
            }
        });

        SwingUtils.aplicarFonte(conteudo, SwingUtils.FONTE_BASE);
        SwingUtils.configurarTabela(tabela, SwingUtils.FONTE_BASE);
    }

    /**
     * Monta o formulário de entrada usando {@link GridBagLayout} com dois GBC
     * reutilizáveis (rótulo e campo), posicionados linha a linha por
     * {@link #adicionarLinha}. {@code weightx=1.0} no GBC de campo faz os
     * campos de texto ocuparem toda a largura disponível.
     */
    private JPanel construirFormulario() {
        JPanel painel = new JPanel(new GridBagLayout());

        // GBC fixo para rótulos: sem expansão, âncora à esquerda, margem direita.
        GridBagConstraints rotulo = new GridBagConstraints();
        rotulo.gridx = 0;
        rotulo.anchor = GridBagConstraints.WEST;
        rotulo.insets = new Insets(6, 0, 6, 12);

        // GBC fixo para campos: expansão horizontal total (weightx=1.0).
        GridBagConstraints campo = new GridBagConstraints();
        campo.gridx = 1;
        campo.weightx = 1.0;
        campo.fill = GridBagConstraints.HORIZONTAL;
        campo.insets = new Insets(6, 0, 6, 0);

        int linha = 0;
        adicionarLinha(painel, rotulo, campo, linha++, "Nome:", campoNome);
        adicionarLinha(painel, rotulo, campo, linha++, "Descrição:", campoDescricao);
        adicionarLinha(painel, rotulo, campo, linha++, "Início (DD-MM-AAAA):", campoDataInicio);
        adicionarLinha(painel, rotulo, campo, linha++, "Término previsto (DD-MM-AAAA):", campoDataTermino);
        adicionarLinha(painel, rotulo, campo, linha, "Status:", campoStatus);
        return painel;
    }

    private void adicionarLinha(JPanel painel, GridBagConstraints rotulo, GridBagConstraints campo,
                                int linha, String texto, JComponent componente) {
        rotulo.gridy = linha;
        campo.gridy = linha;
        painel.add(new JLabel(texto), rotulo);
        painel.add(componente, campo);
    }

    private JPanel construirCentro() {
        JPanel painel = new JPanel(new BorderLayout(0, 8));

        JPanel barraFiltro = new JPanel(new FlowLayout(FlowLayout.LEFT));
        barraFiltro.add(new JLabel("Filtrar por status:"));
        filtroStatus.addActionListener(evento -> recarregarTabela());
        barraFiltro.add(filtroStatus);

        painel.add(barraFiltro, BorderLayout.NORTH);
        painel.add(new JScrollPane(tabela), BorderLayout.CENTER);
        return painel;
    }

    private JPanel construirBotoes() {
        JPanel painel = new JPanel();
        JButton novo = new JButton("Novo");
        JButton salvar = new JButton("Salvar");
        JButton excluir = new JButton("Excluir");
        JButton equipes = new JButton("Gerenciar equipes");
        novo.addActionListener(evento -> limparFormulario());
        salvar.addActionListener(evento -> salvar());
        excluir.addActionListener(evento -> excluir());
        equipes.addActionListener(evento -> abrirDialogoEquipes());
        painel.add(novo);
        painel.add(salvar);
        painel.add(excluir);
        painel.add(equipes);
        return painel;
    }

    private void salvar() {
        try {
            Projeto projeto = lerFormulario();
            if (projetoSelecionadoId == null) {
                controller.salvar(projeto);
                SwingUtils.exibirInformacao(this, "Projeto cadastrado com sucesso.");
            } else {
                projeto.setId(projetoSelecionadoId);
                controller.atualizar(projeto);
                SwingUtils.exibirInformacao(this, "Projeto atualizado com sucesso.");
            }
            limparFormulario();
            filtroStatus.setSelectedItem(FILTRO_TODOS);
            recarregarTabela();
        } catch (ValidacaoException excecao) {
            SwingUtils.exibirErro(this, excecao.getMessage());
        }
    }

    private void excluir() {
        if (projetoSelecionadoId == null) {
            SwingUtils.exibirErro(this, "Selecione um projeto na tabela para excluir.");
            return;
        }
        int opcao = JOptionPane.showConfirmDialog(this,
            "Confirma a exclusão do projeto selecionado?", "Excluir", JOptionPane.YES_NO_OPTION);
        if (opcao != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            controller.excluir(projetoSelecionadoId);
            SwingUtils.exibirInformacao(this, "Projeto excluído com sucesso.");
            limparFormulario();
            recarregarTabela();
        } catch (ExclusaoBloqueadaException excecao) {
            SwingUtils.exibirErro(this, excecao.getMessage());
        }
    }

    private Projeto lerFormulario() {
        return new Projeto(
            campoNome.getText().trim(),
            campoDescricao.getText().trim(),
            DateUtils.parse(campoDataInicio.getText()),
            DateUtils.parse(campoDataTermino.getText()),
            (StatusProjeto) campoStatus.getSelectedItem()
        );
    }

    private void carregarSelecionado() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) {
            return;
        }
        int id = (int) tableModel.getValueAt(linha, 0);
        controller.buscarPorId(id).ifPresent(projeto -> {
            projetoSelecionadoId = projeto.getId();
            campoNome.setText(projeto.getNome());
            campoDescricao.setText(projeto.getDescricao());
            campoDataInicio.setText(DateUtils.format(projeto.getDataInicio()));
            campoDataTermino.setText(DateUtils.format(projeto.getDataTerminoPrevista()));
            campoStatus.setSelectedItem(projeto.getStatus());
        });
    }

    private void recarregarTabela() {
        tableModel.setRowCount(0);
        for (Projeto projeto : listarConformeFiltro()) {
            tableModel.addRow(new Object[] {
                projeto.getId(),
                projeto.getNome(),
                projeto.getDescricao(),
                DateUtils.format(projeto.getDataInicio()),
                DateUtils.format(projeto.getDataTerminoPrevista()),
                projeto.getStatus()
            });
        }
    }

    /**
     * Delega ao controller a listagem conforme o item ativo do combo de filtro.
     * Pattern matching distingue o item sentinela "Todos" (String) do enum
     * {@link StatusProjeto}, evitando cast e simplificando o branch.
     */
    private List<Projeto> listarConformeFiltro() {
        Object selecionado = filtroStatus.getSelectedItem();
        if (selecionado instanceof StatusProjeto status) {
            return controller.listarPorStatus(status);
        }
        return controller.listarTodos();
    }

    private void abrirDialogoEquipes() {
        if (projetoSelecionadoId == null) {
            SwingUtils.exibirErro(this, "Selecione um projeto na tabela para gerenciar equipes.");
            return;
        }
        String nomeProjeto = campoNome.getText().trim();
        JDialog dialogo = new JDialog(this, "Equipes — " + nomeProjeto, true);
        dialogo.setSize(560, 400);
        dialogo.setLocationRelativeTo(this);
        JPanel painelRaiz = new JPanel(new BorderLayout(8, 8));
        painelRaiz.setBorder(new EmptyBorder(12, 12, 12, 12));
        dialogo.setContentPane(painelRaiz);

        DefaultTableModel modeloEquipes = SwingUtils.modeloSomenteLeitura(
            new Object[] {"ID", "Nome"});
        JTable tabelaEquipes = new JTable(modeloEquipes);
        tabelaEquipes.setRowHeight(24);

        // Renderer com pattern matching: exibe apenas o nome da Equipe no combo,
        // ignorando a representação padrão de toString().
        JComboBox<Equipe> comboEquipes = new JComboBox<>();
        comboEquipes.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Equipe equipe) {
                    setText(equipe.getNome());
                }
                return this;
            }
        });

        // Lambda capturado como Runnable: reutilizado pelos botões Adicionar e Remover
        // para ressincronizar tabela e combo sem duplicar lógica. Calcula a diferença
        // entre todas as equipes e as já vinculadas para oferecer apenas as disponíveis.
        Runnable recarregar = () -> {
            modeloEquipes.setRowCount(0);
            List<Equipe> vinculadas = controller.listarEquipesDoProjeto(projetoSelecionadoId);
            for (Equipe e : vinculadas) {
                modeloEquipes.addRow(new Object[] {e.getId(), e.getNome()});
            }
            Set<Integer> vinculadaIds = vinculadas.stream()
                .map(Equipe::getId).collect(Collectors.toSet());
            DefaultComboBoxModel<Equipe> modelo = new DefaultComboBoxModel<>();
            for (Equipe e : controller.listarEquipes()) {
                if (!vinculadaIds.contains(e.getId())) {
                    modelo.addElement(e);
                }
            }
            comboEquipes.setModel(modelo);
        };
        recarregar.run();

        JButton adicionar = new JButton("Adicionar");
        adicionar.addActionListener(evento -> {
            Equipe selecionada = (Equipe) comboEquipes.getSelectedItem();
            if (selecionada == null) {
                SwingUtils.exibirErro(this, "Selecione uma equipe para adicionar.");
                return;
            }
            try {
                controller.vincularEquipe(projetoSelecionadoId, selecionada.getId());
                recarregar.run();
            } catch (ValidacaoException | RegistroDuplicadoException ex) {
                SwingUtils.exibirErro(this, ex.getMessage());
            }
        });

        JButton remover = new JButton("Remover");
        remover.addActionListener(evento -> {
            int linha = tabelaEquipes.getSelectedRow();
            if (linha < 0) {
                SwingUtils.exibirErro(this, "Selecione uma equipe na tabela para remover.");
                return;
            }
            int equipeId = (int) modeloEquipes.getValueAt(linha, 0);
            controller.desvincularEquipe(projetoSelecionadoId, equipeId);
            recarregar.run();
        });

        JButton fechar = new JButton("Fechar");
        fechar.addActionListener(evento -> dialogo.dispose());

        JPanel painelAcoes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        painelAcoes.add(comboEquipes);
        painelAcoes.add(adicionar);
        painelAcoes.add(remover);
        painelAcoes.add(fechar);

        painelRaiz.add(new JScrollPane(tabelaEquipes), BorderLayout.CENTER);
        painelRaiz.add(painelAcoes, BorderLayout.SOUTH);

        SwingUtils.aplicarFonte(dialogo.getContentPane(), SwingUtils.FONTE_BASE);
        dialogo.setVisible(true);
    }

    private void limparFormulario() {
        projetoSelecionadoId = null;
        tabela.clearSelection();
        campoNome.setText("");
        campoDescricao.setText("");
        campoDataInicio.setText("");
        campoDataTermino.setText("");
        campoStatus.setSelectedIndex(0);
    }
}
