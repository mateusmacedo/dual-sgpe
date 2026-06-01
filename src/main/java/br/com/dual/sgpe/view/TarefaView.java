package br.com.dual.sgpe.view;

import br.com.dual.sgpe.controller.TarefaController;
import br.com.dual.sgpe.exception.ValidacaoException;
import br.com.dual.sgpe.model.entity.Projeto;
import br.com.dual.sgpe.model.entity.Tarefa;
import br.com.dual.sgpe.model.entity.Usuario;
import br.com.dual.sgpe.model.enums.StatusTarefa;
import br.com.dual.sgpe.util.DateUtils;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
 * Tela de cadastro e visualização de tarefas do SGPE.
 *
 * <p>Opera em dois modos mutuamente exclusivos definidos no construtor:
 * <ul>
 *   <li><b>Modo edição</b> (gestor): botões Salvar e Excluir habilitados;
 *       tabela filtrada por projeto selecionado no combo de filtro.</li>
 *   <li><b>Modo leitura</b> (colaborador): botões Salvar e Excluir desabilitados;
 *       tabela carregada via {@code findByResponsavelId} mostrando apenas
 *       as tarefas atribuídas ao usuário em sessão.</li>
 * </ul>
 * Toda lógica de negócio é delegada ao {@link TarefaController}.
 */
public class TarefaView extends JFrame {

    private final transient TarefaController controller;
    private final transient Usuario usuarioSessao;
    /**
     * Quando {@code true}, a tela opera em modo leitura:
     * botões de persistência desabilitados e tabela filtrada pelo responsável.
     */
    private final boolean modoLeitura;

    private final JTextField campoTitulo = new JTextField(24);
    private final JTextField campoDescricao = new JTextField(24);
    private final JComboBox<Projeto> comboProjeto = new JComboBox<>();
    private final JComboBox<Usuario> comboResponsavel = new JComboBox<>();
    private final JTextField campoDataInicio = new JTextField(24);
    private final JTextField campoDataTermino = new JTextField(24);
    private final JComboBox<StatusTarefa> campoStatus = new JComboBox<>(StatusTarefa.values());

    private final JComboBox<Projeto> filtroProjeto = new JComboBox<>();

    // Tabela somente-leitura — edição apenas via formulário
    private final DefaultTableModel tableModel = SwingUtils.modeloSomenteLeitura(
        new Object[] {"ID", "Título", "Responsável", "Início", "Término", "Status"});
    private final JTable tabela = new JTable(tableModel);

    /** ID da tarefa selecionada na tabela; {@code null} indica modo inserção. */
    private Integer tarefaSelecionadaId;

    /**
     * Cria a tela de tarefas para o usuário em sessão.
     *
     * @param controller    controller que processa as operações CRUD e consultas
     * @param usuarioSessao usuário autenticado (usado no modo leitura para filtrar responsável)
     * @param modoLeitura   {@code true} para colaborador (somente leitura);
     *                      {@code false} para gestor (edição completa)
     */
    public TarefaView(TarefaController controller, Usuario usuarioSessao, boolean modoLeitura) {
        super("Cadastro de Tarefas — SGPE");
        this.controller = controller;
        this.usuarioSessao = usuarioSessao;
        this.modoLeitura = modoLeitura;
        configurarRenderers();
        carregarCombos();
        configurarJanela();
        recarregarTabela();
    }

    /**
     * Configura os renderers customizados dos combos de projeto e responsável.
     *
     * <p>O {@code rendererProjeto} é instanciado uma única vez e reutilizado em
     * {@code comboProjeto} e {@code filtroProjeto} — ambos exibem o nome do
     * projeto usando pattern matching ({@code instanceof Projeto projeto}).
     * O renderer de responsável usa o mesmo padrão para exibir o nome completo.
     */
    private void configurarRenderers() {
        // Renderer compartilhado entre comboProjeto e filtroProjeto
        DefaultListCellRenderer rendererProjeto = new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Projeto projeto) {
                    setText(projeto.getNome());
                }
                return this;
            }
        };
        comboProjeto.setRenderer(rendererProjeto);
        filtroProjeto.setRenderer(rendererProjeto);

        // Renderer do responsável: exibe nomeCompleto via pattern matching
        comboResponsavel.setRenderer(new DefaultListCellRenderer() {
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
    }

    private void carregarCombos() {
        List<Projeto> projetos = controller.listarProjetos();
        DefaultComboBoxModel<Projeto> modeloProjeto = new DefaultComboBoxModel<>();
        DefaultComboBoxModel<Projeto> modeloFiltro = new DefaultComboBoxModel<>();
        for (Projeto projeto : projetos) {
            modeloProjeto.addElement(projeto);
            modeloFiltro.addElement(projeto);
        }
        comboProjeto.setModel(modeloProjeto);
        filtroProjeto.setModel(modeloFiltro);

        DefaultComboBoxModel<Usuario> modeloResponsavel = new DefaultComboBoxModel<>();
        for (Usuario usuario : controller.listarResponsaveis()) {
            modeloResponsavel.addElement(usuario);
        }
        comboResponsavel.setModel(modeloResponsavel);
    }

    private void configurarJanela() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 640);
        setLocationRelativeTo(null);

        JPanel conteudo = new JPanel(new BorderLayout(12, 12));
        conteudo.setBorder(new EmptyBorder(16, 16, 16, 16));
        setContentPane(conteudo);

        conteudo.add(construirFormulario(), BorderLayout.NORTH);
        conteudo.add(construirCentro(), BorderLayout.CENTER);
        conteudo.add(construirBotoes(), BorderLayout.SOUTH);

        // Listener de seleção: getValueIsAdjusting() evita disparo duplo durante arraste
        tabela.getSelectionModel().addListSelectionListener(evento -> {
            if (!evento.getValueIsAdjusting()) {
                carregarSelecionado();
            }
        });

        SwingUtils.aplicarFonte(conteudo, SwingUtils.FONTE_BASE);
        SwingUtils.configurarTabela(tabela, SwingUtils.FONTE_BASE);
    }

    /**
     * Monta o formulário de entrada via {@link GridBagLayout} com dois grupos
     * de constraints reutilizados por linha: rótulos na coluna 0 e campos na
     * coluna 1 (expansível horizontalmente).
     */
    private JPanel construirFormulario() {
        JPanel painel = new JPanel(new GridBagLayout());

        // Constraints para rótulos: coluna fixa, sem expansão
        GridBagConstraints rotulo = new GridBagConstraints();
        rotulo.gridx = 0;
        rotulo.anchor = GridBagConstraints.WEST;
        rotulo.insets = new Insets(6, 0, 6, 12);

        // Constraints para campos: expande horizontalmente para preencher a janela
        GridBagConstraints campo = new GridBagConstraints();
        campo.gridx = 1;
        campo.weightx = 1.0;
        campo.fill = GridBagConstraints.HORIZONTAL;
        campo.insets = new Insets(6, 0, 6, 0);

        int linha = 0;
        adicionarLinha(painel, rotulo, campo, linha++, "Título:", campoTitulo);
        adicionarLinha(painel, rotulo, campo, linha++, "Descrição:", campoDescricao);
        adicionarLinha(painel, rotulo, campo, linha++, "Projeto:", comboProjeto);
        adicionarLinha(painel, rotulo, campo, linha++, "Responsável:", comboResponsavel);
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

    /**
     * Monta o painel central com barra de filtro e tabela de tarefas.
     *
     * <p>Em modo leitura, {@code filtroProjeto} é desabilitado pois a tabela
     * já é filtrada automaticamente por responsável em {@link #recarregarTabela()}.
     */
    private JPanel construirCentro() {
        JPanel painel = new JPanel(new BorderLayout(0, 8));

        JPanel barraFiltro = new JPanel(new FlowLayout(FlowLayout.LEFT));
        barraFiltro.add(new JLabel("Filtrar por projeto:"));
        filtroProjeto.addActionListener(evento -> recarregarTabela());
        // Em modo leitura o filtro é irrelevante — a tabela já filtra por responsável
        filtroProjeto.setEnabled(!modoLeitura);
        barraFiltro.add(filtroProjeto);

        painel.add(barraFiltro, BorderLayout.NORTH);
        painel.add(new JScrollPane(tabela), BorderLayout.CENTER);
        return painel;
    }

    /**
     * Cria a barra de botões de ação.
     *
     * <p>Em modo leitura (colaborador), Salvar e Excluir são desabilitados;
     * o botão Novo permanece ativo para limpar a seleção visual do formulário.
     */
    private JPanel construirBotoes() {
        JPanel painel = new JPanel();
        JButton novo = new JButton("Novo");
        JButton salvar = new JButton("Salvar");
        JButton excluir = new JButton("Excluir");
        novo.addActionListener(evento -> limparFormulario());
        salvar.addActionListener(evento -> salvar());
        excluir.addActionListener(evento -> excluir());
        // Modo leitura: colaborador não pode persistir nem remover tarefas
        salvar.setEnabled(!modoLeitura);
        excluir.setEnabled(!modoLeitura);
        painel.add(novo);
        painel.add(salvar);
        painel.add(excluir);
        return painel;
    }

    private void salvar() {
        try {
            Tarefa tarefa = lerFormulario();
            if (tarefaSelecionadaId == null) {
                controller.salvar(tarefa);
                SwingUtils.exibirInformacao(this, "Tarefa cadastrada com sucesso.");
            } else {
                tarefa.setId(tarefaSelecionadaId);
                controller.atualizar(tarefa);
                SwingUtils.exibirInformacao(this, "Tarefa atualizada com sucesso.");
            }
            limparFormulario();
            recarregarTabela();
        } catch (ValidacaoException excecao) {
            SwingUtils.exibirErro(this, excecao.getMessage());
        }
    }

    private void excluir() {
        if (tarefaSelecionadaId == null) {
            SwingUtils.exibirErro(this, "Selecione uma tarefa na tabela para excluir.");
            return;
        }
        int opcao = JOptionPane.showConfirmDialog(this,
            "Confirma a exclusão da tarefa selecionada?", "Excluir", JOptionPane.YES_NO_OPTION);
        if (opcao != JOptionPane.YES_OPTION) {
            return;
        }
        controller.excluir(tarefaSelecionadaId);
        SwingUtils.exibirInformacao(this, "Tarefa excluída com sucesso.");
        limparFormulario();
        recarregarTabela();
    }

    private Tarefa lerFormulario() {
        Projeto projeto = (Projeto) comboProjeto.getSelectedItem();
        if (projeto == null) {
            throw new ValidacaoException("Selecione um projeto.");
        }
        Usuario responsavel = (Usuario) comboResponsavel.getSelectedItem();
        if (responsavel == null) {
            throw new ValidacaoException("Selecione um responsável.");
        }
        return new Tarefa(
            campoTitulo.getText().trim(),
            campoDescricao.getText().trim(),
            projeto.getId(),
            responsavel.getId(),
            DateUtils.parse(campoDataInicio.getText()),
            DateUtils.parse(campoDataTermino.getText()),
            (StatusTarefa) campoStatus.getSelectedItem()
        );
    }

    private void carregarSelecionado() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) {
            return;
        }
        int id = (int) tableModel.getValueAt(linha, 0);
        controller.buscarPorId(id).ifPresent(tarefa -> {
            tarefaSelecionadaId = tarefa.getId();
            campoTitulo.setText(tarefa.getTitulo());
            campoDescricao.setText(tarefa.getDescricao());
            selecionarPorId(comboProjeto, tarefa.getProjetoId(), Projeto::getId);
            selecionarPorId(comboResponsavel, tarefa.getResponsavelId(), Usuario::getId);
            campoDataInicio.setText(DateUtils.format(tarefa.getDataInicio()));
            campoDataTermino.setText(DateUtils.format(tarefa.getDataTerminoPrevista()));
            campoStatus.setSelectedItem(tarefa.getStatus());
        });
    }

    /**
     * Seleciona no combo o item cujo id, extraído por {@code idExtractor},
     * coincide com {@code id}. Unifica a seleção por id de projeto e de
     * responsável num único helper genérico.
     *
     * @param combo       combo a ajustar
     * @param id          id procurado
     * @param idExtractor função que extrai o id de um item do combo
     * @param <T>         tipo dos itens do combo
     */
    private <T> void selecionarPorId(JComboBox<T> combo, int id, ToIntFunction<T> idExtractor) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (idExtractor.applyAsInt(combo.getItemAt(i)) == id) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    /**
     * Recarrega a tabela de tarefas de acordo com o modo ativo:
     * <ul>
     *   <li>Modo leitura: busca somente as tarefas em que o usuário em sessão
     *       é responsável ({@code findByResponsavelId}).</li>
     *   <li>Modo edição: busca pelo projeto selecionado no filtro
     *       ({@code findByProjetoId}).</li>
     * </ul>
     */
    private void recarregarTabela() {
        tableModel.setRowCount(0);
        List<Tarefa> tarefas;
        if (modoLeitura) {
            // Colaborador: exibe apenas suas próprias tarefas, independente do projeto
            tarefas = controller.findByResponsavelId(usuarioSessao.getId());
        } else {
            Projeto projetoFiltro = (Projeto) filtroProjeto.getSelectedItem();
            if (projetoFiltro == null) {
                return;
            }
            tarefas = controller.findByProjetoId(projetoFiltro.getId());
        }
        // Indexa os responsáveis por id antes do loop para resolver o nome em O(1)
        // por linha, evitando uma varredura do combo a cada tarefa.
        Map<Integer, String> nomesResponsaveis = mapearNomesResponsaveis();
        for (Tarefa tarefa : tarefas) {
            tableModel.addRow(new Object[] {
                tarefa.getId(),
                tarefa.getTitulo(),
                nomesResponsaveis.getOrDefault(tarefa.getResponsavelId(), "?"),
                DateUtils.format(tarefa.getDataInicio()),
                DateUtils.format(tarefa.getDataTerminoPrevista()),
                tarefa.getStatus()
            });
        }
    }

    private Map<Integer, String> mapearNomesResponsaveis() {
        Map<Integer, String> nomes = new HashMap<>();
        for (int i = 0; i < comboResponsavel.getItemCount(); i++) {
            Usuario usuario = comboResponsavel.getItemAt(i);
            nomes.put(usuario.getId(), usuario.getNomeCompleto());
        }
        return nomes;
    }

    private void limparFormulario() {
        tarefaSelecionadaId = null;
        tabela.clearSelection();
        campoTitulo.setText("");
        campoDescricao.setText("");
        if (comboProjeto.getItemCount() > 0) {
            comboProjeto.setSelectedIndex(0);
        }
        if (comboResponsavel.getItemCount() > 0) {
            comboResponsavel.setSelectedIndex(0);
        }
        campoDataInicio.setText("");
        campoDataTermino.setText("");
        campoStatus.setSelectedIndex(0);
    }

}
