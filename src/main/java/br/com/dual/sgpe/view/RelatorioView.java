package br.com.dual.sgpe.view;

import br.com.dual.sgpe.controller.RelatorioController;
import br.com.dual.sgpe.exception.ValidacaoException;
import br.com.dual.sgpe.model.dto.RelatorioProjeto;
import br.com.dual.sgpe.model.entity.Equipe;
import br.com.dual.sgpe.model.entity.Projeto;
import br.com.dual.sgpe.model.entity.Usuario;
import br.com.dual.sgpe.model.enums.PerfilUsuario;
import br.com.dual.sgpe.security.EscopoColaborador;
import br.com.dual.sgpe.util.DateUtils;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

/**
 * Tela de relatório de desempenho de projetos do SGPE.
 *
 * <p>Exibe KPIs de conclusão (percentual em destaque com {@link #FONTE_KPI},
 * contagens de tarefas), dados básicos do projeto e listas de equipes e
 * responsáveis envolvidos. Apresenta comportamento diferenciado por perfil:
 * o colaborador vê apenas os projetos retornados por
 * {@link EscopoColaborador#projetosDoColaborador(int)}, enquanto o gestor
 * enxerga todos os projetos.
 */
public class RelatorioView extends JFrame {

    /** Fonte ampliada usada no label de percentual de conclusão (KPI principal). */
    private static final Font FONTE_KPI = new Font("SansSerif", Font.BOLD, 36);

    private final transient RelatorioController controller;
    private final transient Usuario usuarioSessao;
    /** Provedor de escopo que restringe projetos visíveis ao colaborador logado. */
    private final transient EscopoColaborador escopo;
    /** {@code true} quando o perfil do usuário restringe a visão ao escopo de suas equipes. */
    private final boolean escopoRestrito;

    private final JComboBox<Projeto> comboProjeto = new JComboBox<>();
    private final JLabel labelNome = new JLabel("-");
    private final JLabel labelStatus = new JLabel("-");
    private final JLabel labelInicio = new JLabel("-");
    private final JLabel labelTermino = new JLabel("-");
    private final JLabel labelPercentual = new JLabel("0,0%");
    private final JLabel labelContagens = new JLabel("Total: 0 | Concluídas: 0 | Pendentes: 0");
    private final JLabel labelMensagem = new JLabel(" ");
    private final JTextArea areaEquipes = new JTextArea(4, 30);
    private final JTextArea areaResponsaveis = new JTextArea(4, 30);

    /**
     * Cria a tela de relatório para o usuário em sessão.
     *
     * @param controller   controller que gera o {@link br.com.dual.sgpe.model.dto.RelatorioProjeto}
     * @param usuarioSessao usuário autenticado (determina perfil e escopo visível)
     * @param escopo       provedor de projetos restritos ao colaborador
     */
    public RelatorioView(RelatorioController controller, Usuario usuarioSessao,
                         EscopoColaborador escopo) {
        super("Relatório de Desempenho — SGPE");
        this.controller = controller;
        this.usuarioSessao = usuarioSessao;
        this.escopo = escopo;
        this.escopoRestrito = usuarioSessao.getPerfil() != PerfilUsuario.ADMINISTRADOR;
        carregarComboProjetos();
        configurarJanela();
    }

    /**
     * Popula o combo de projetos de acordo com o perfil do usuário:
     * colaboradores recebem apenas os projetos de seu escopo;
     * gestores recebem a lista completa.
     *
     * <p>O renderer usa pattern matching ({@code instanceof Projeto projeto})
     * para exibir o nome legível no lugar do {@code toString()} padrão.
     */
    private void carregarComboProjetos() {
        DefaultComboBoxModel<Projeto> modelo = new DefaultComboBoxModel<>();
        // Escopo restrito ao colaborador vs. lista global do gestor
        List<Projeto> projetos = escopoRestrito
            ? escopo.projetosDoColaborador(usuarioSessao.getId())
            : controller.listarProjetos();
        for (Projeto p : projetos) {
            modelo.addElement(p);
        }
        comboProjeto.setModel(modelo);
        // Renderer com pattern matching para exibir nome do projeto no combo
        comboProjeto.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Projeto projeto) {
                    setText(projeto.getNome());
                }
                return this;
            }
        });
    }

    private void configurarJanela() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(700, 620);
        setLocationRelativeTo(null);

        JPanel conteudo = new JPanel(new BorderLayout(12, 12));
        conteudo.setBorder(new EmptyBorder(16, 16, 16, 16));
        setContentPane(conteudo);

        conteudo.add(construirTopo(), BorderLayout.NORTH);
        conteudo.add(construirCentro(), BorderLayout.CENTER);

        SwingUtils.aplicarFonte(conteudo, SwingUtils.FONTE_BASE);
        // Sobrescreve a fonte padrão do KPI após aplicar a fonte base para destacar o percentual
        labelPercentual.setFont(FONTE_KPI);
    }

    private JPanel construirTopo() {
        JPanel painel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        painel.add(new JLabel("Projeto:"));
        painel.add(comboProjeto);
        JButton gerar = new JButton("Gerar Relatório");
        gerar.addActionListener(evento -> gerarRelatorio());
        painel.add(gerar);
        return painel;
    }

    /**
     * Constrói o painel central com três seções:
     * <ol>
     *   <li>Dados básicos do projeto (GridLayout 4×2);</li>
     *   <li>Painel de KPI com percentual em destaque ({@link #FONTE_KPI}),
     *       contagem de tarefas e aviso quando não há tarefas cadastradas;</li>
     *   <li>Áreas de texto somente-leitura com equipes e responsáveis.</li>
     * </ol>
     */
    private JPanel construirCentro() {
        JPanel painel = new JPanel(new BorderLayout(8, 8));

        JPanel dadosBasicos = new JPanel(new GridLayout(4, 2, 8, 4));
        dadosBasicos.setBorder(BorderFactory.createTitledBorder("Dados do Projeto"));
        dadosBasicos.add(new JLabel("Nome:")); dadosBasicos.add(labelNome);
        dadosBasicos.add(new JLabel("Status:")); dadosBasicos.add(labelStatus);
        dadosBasicos.add(new JLabel("Início:")); dadosBasicos.add(labelInicio);
        dadosBasicos.add(new JLabel("Término previsto:")); dadosBasicos.add(labelTermino);

        // Painel KPI: percentual centralizado em fonte grande, contagens abaixo, mensagem acima
        JPanel kpi = new JPanel(new BorderLayout());
        kpi.setBorder(BorderFactory.createTitledBorder("Conclusão"));
        labelPercentual.setHorizontalAlignment(SwingConstants.CENTER);
        kpi.add(labelPercentual, BorderLayout.CENTER);
        labelContagens.setHorizontalAlignment(SwingConstants.CENTER);
        kpi.add(labelContagens, BorderLayout.SOUTH);
        labelMensagem.setHorizontalAlignment(SwingConstants.CENTER);
        kpi.add(labelMensagem, BorderLayout.NORTH);

        JPanel topo = new JPanel(new BorderLayout(8, 8));
        topo.add(dadosBasicos, BorderLayout.NORTH);
        topo.add(kpi, BorderLayout.CENTER);

        areaEquipes.setEditable(false);
        areaResponsaveis.setEditable(false);

        JPanel listas = new JPanel(new GridLayout(1, 2, 8, 0));
        JPanel painelEquipes = new JPanel(new BorderLayout());
        painelEquipes.setBorder(BorderFactory.createTitledBorder("Equipes Vinculadas"));
        painelEquipes.add(new JScrollPane(areaEquipes));
        JPanel painelResp = new JPanel(new BorderLayout());
        painelResp.setBorder(BorderFactory.createTitledBorder("Responsáveis Envolvidos"));
        painelResp.add(new JScrollPane(areaResponsaveis));
        listas.add(painelEquipes);
        listas.add(painelResp);

        painel.add(topo, BorderLayout.NORTH);
        painel.add(listas, BorderLayout.CENTER);
        return painel;
    }

    /**
     * Consulta o controller e preenche todos os campos da tela com os dados
     * do {@link RelatorioProjeto} retornado.
     *
     * <p>Quando {@code isSemTarefas()} é verdadeiro, exibe mensagem explicativa
     * no label de aviso do painel KPI para evitar exibição de "0,0%" sem contexto.
     */
    private void gerarRelatorio() {
        Projeto selecionado = (Projeto) comboProjeto.getSelectedItem();
        if (selecionado == null) {
            JOptionPane.showMessageDialog(this, "Selecione um projeto.",
                "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            RelatorioProjeto r = controller.gerar(selecionado.getId());
            Projeto p = r.getProjeto();
            labelNome.setText(p.getNome());
            labelStatus.setText(p.getStatus().name());
            labelInicio.setText(DateUtils.format(p.getDataInicio()));
            labelTermino.setText(DateUtils.format(p.getDataTerminoPrevista()));
            labelPercentual.setText(String.format("%.1f%%", r.getPercentualConclusao()));
            labelContagens.setText(String.format("Total: %d | Concluídas: %d | Pendentes: %d",
                r.getTotalTarefas(), r.getTarefasConcluidas(), r.getTarefasPendentes()));
            // Aviso contextual quando o projeto ainda não possui tarefas
            labelMensagem.setText(r.isSemTarefas() ? "Projeto sem tarefas cadastradas" : " ");

            StringBuilder equipesTxt = new StringBuilder();
            for (Equipe e : r.getEquipes()) {
                equipesTxt.append("• ").append(e.getNome()).append("\n");
            }
            areaEquipes.setText(equipesTxt.length() > 0 ? equipesTxt.toString() : "Nenhuma equipe vinculada");

            StringBuilder respTxt = new StringBuilder();
            for (Usuario u : r.getResponsaveis()) {
                respTxt.append("• ").append(u.getNomeCompleto()).append("\n");
            }
            areaResponsaveis.setText(respTxt.length() > 0 ? respTxt.toString() : "Nenhum responsável");
        } catch (ValidacaoException excecao) {
            SwingUtils.exibirErro(this, excecao.getMessage());
        } catch (RuntimeException excecao) {
            SwingUtils.exibirErro(this, "Erro inesperado. Tente novamente.");
        }
    }

}
