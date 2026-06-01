package br.com.dual.sgpe.view;

import br.com.dual.sgpe.controller.EquipeController;
import br.com.dual.sgpe.controller.ProjetoController;
import br.com.dual.sgpe.controller.RelatorioController;
import br.com.dual.sgpe.controller.TarefaController;
import br.com.dual.sgpe.controller.UsuarioController;
import br.com.dual.sgpe.model.entity.Usuario;
import br.com.dual.sgpe.model.enums.PerfilUsuario;
import br.com.dual.sgpe.security.EscopoColaborador;
import br.com.dual.sgpe.security.PermissoesPerfil;
import br.com.dual.sgpe.security.Recurso;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

/**
 * Tela principal (hub de navegação) exibida após a autenticação. Apresenta o
 * usuário autenticado e, conforme o perfil (RBAC — {@link PermissoesPerfil}),
 * os botões dos recursos permitidos. Cada botão abre a tela correspondente,
 * passando o contexto do usuário (perfil/escopo) quando há filtro ou modo
 * leitura. O botão "Sair" delega o logout ao callback recebido.
 */
public class MainView extends JFrame {

    private final Usuario usuario;
    private final Runnable aoSair;
    private final UsuarioController usuarioController;
    private final ProjetoController projetoController;
    private final TarefaController tarefaController;
    private final EquipeController equipeController;
    private final RelatorioController relatorioController;
    private final EscopoColaborador escopoColaborador;

    /**
     * Cria o hub de navegação para o usuário autenticado.
     *
     * @param usuario            usuário autenticado; perfil determina o conjunto de recursos visíveis
     * @param aoSair             callback executado ao clicar em "Sair" — tipicamente {@code LoginView::voltarParaLogin}
     * @param usuarioController  repassado à {@link UsuarioView} quando o recurso é aberto
     * @param projetoController  repassado às telas de projeto e consulta
     * @param tarefaController   repassado à {@link TarefaView}
     * @param equipeController   repassado à {@link EquipeView}
     * @param relatorioController repassado à {@link RelatorioView}
     * @param escopoColaborador  repassado às telas que filtram por escopo do colaborador
     */
    public MainView(Usuario usuario, Runnable aoSair,
                    UsuarioController usuarioController,
                    ProjetoController projetoController,
                    TarefaController tarefaController,
                    EquipeController equipeController,
                    RelatorioController relatorioController,
                    EscopoColaborador escopoColaborador) {
        super("SGPE — Início");
        this.usuario = usuario;
        this.aoSair = aoSair;
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
        setSize(440, 360);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JLabel boasVindas = new JLabel(
            "Bem-vindo, " + usuario.getNomeCompleto() + " (" + usuario.getPerfil() + ")",
            SwingConstants.CENTER);
        boasVindas.setFont(boasVindas.getFont().deriveFont(Font.BOLD, 16f));
        boasVindas.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        add(boasVindas, BorderLayout.NORTH);

        add(criarPainelRecursos(), BorderLayout.CENTER);

        JButton sair = new JButton("Sair");
        sair.addActionListener(evento -> {
            dispose();
            aoSair.run();
        });
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.CENTER));
        rodape.add(sair);
        add(rodape, BorderLayout.SOUTH);
    }

    /**
     * Gera dinamicamente um botão por recurso permitido ao perfil do usuário.
     * {@link PermissoesPerfil#recursosDe} retorna um conjunto ordenado; a ordem
     * dos botões reflete diretamente a ordem de declaração no enum {@link Recurso}.
     */
    private JPanel criarPainelRecursos() {
        Set<Recurso> recursos = PermissoesPerfil.recursosDe(usuario.getPerfil());
        JPanel painel = new JPanel(new GridLayout(0, 1, 8, 8));
        painel.setBorder(BorderFactory.createEmptyBorder(8, 60, 8, 60));
        for (Recurso recurso : recursos) {
            JButton botao = new JButton(recurso.getRotulo());
            botao.addActionListener(evento -> abrir(recurso));
            painel.add(botao);
        }
        return painel;
    }

    /**
     * Instancia e exibe a tela correspondente ao {@link Recurso} solicitado.
     * A revalidação de permissão (mesmo que o botão só seja renderizado quando
     * permitido) aplica defesa em profundidade contra acesso programático direto.
     * As telas de recurso usam {@link WindowConstants#DISPOSE_ON_CLOSE} para não
     * encerrar o processo ao fechá-las — somente a MainView usa {@code EXIT_ON_CLOSE}.
     */
    private void abrir(Recurso recurso) {
        // Defesa em profundidade: além de só renderizar botões permitidos, revalida o acesso.
        if (!PermissoesPerfil.podeAcessar(usuario.getPerfil(), recurso)) {
            return;
        }
        boolean colaborador = usuario.getPerfil() == PerfilUsuario.COLABORADOR;
        JFrame janela = switch (recurso) {
            case USUARIOS -> new UsuarioView(usuarioController);
            case PROJETOS -> new ProjetoView(projetoController);
            case TAREFAS -> new TarefaView(tarefaController, usuario, colaborador);
            case EQUIPES -> new EquipeView(equipeController);
            case CONSULTA -> new ConsultaProjetoView(projetoController, usuario, escopoColaborador);
            case RELATORIO -> new RelatorioView(relatorioController, usuario, escopoColaborador);
        };
        // Fechar uma tela de recurso volta ao hub; apenas a MainView encerra o app.
        janela.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        janela.setVisible(true);
    }
}
