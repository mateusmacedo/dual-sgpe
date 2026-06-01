package br.com.dual.sgpe.view;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * Utilitários compartilhados pelas telas Swing do SGPE. Centraliza a fonte base,
 * a aplicação recursiva de fonte, os diálogos de informação e erro e a montagem
 * de tabelas somente-leitura, eliminando a duplicação dessas rotinas entre as
 * várias views.
 */
public final class SwingUtils {

    /** Fonte base aplicada a toda a hierarquia de componentes das telas. */
    public static final Font FONTE_BASE = new Font("SansSerif", Font.PLAIN, 15);

    private SwingUtils() {
    }

    /**
     * Aplica {@code fonte} recursivamente a {@code componente} e a toda a sua
     * hierarquia de filhos. O instanceof com pattern variable (Java 16+) elimina
     * o cast manual para {@link Container}, encerrando a recursão nos componentes
     * folha naturalmente.
     *
     * @param componente raiz da subárvore de componentes
     * @param fonte      fonte a propagar
     */
    static void aplicarFonte(Component componente, Font fonte) {
        componente.setFont(fonte);
        if (componente instanceof Container container) {
            for (Component filho : container.getComponents()) {
                aplicarFonte(filho, fonte);
            }
        }
    }

    /**
     * Exibe um diálogo informativo padrão.
     *
     * @param owner    componente pai do diálogo (pode ser {@code null})
     * @param mensagem texto a exibir
     */
    static void exibirInformacao(Component owner, String mensagem) {
        JOptionPane.showMessageDialog(owner, mensagem, "Informação", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Exibe um diálogo de erro padrão.
     *
     * @param owner    componente pai do diálogo (pode ser {@code null})
     * @param mensagem texto a exibir
     */
    static void exibirErro(Component owner, String mensagem) {
        JOptionPane.showMessageDialog(owner, mensagem, "Erro", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Cria um {@link DefaultTableModel} somente-leitura: {@code isCellEditable}
     * sempre retorna {@code false}, impedindo a edição inline das células. O
     * preenchimento ocorre exclusivamente pela lógica da view.
     *
     * @param colunas cabeçalhos das colunas
     * @return modelo de tabela não editável
     */
    static DefaultTableModel modeloSomenteLeitura(Object[] colunas) {
        return new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    /**
     * Configura a aparência padrão de uma tabela: altura de linha e cabeçalho em
     * negrito derivado de {@code fonte}.
     *
     * @param tabela tabela a configurar
     * @param fonte  fonte base usada para derivar o cabeçalho em negrito
     */
    static void configurarTabela(JTable tabela, Font fonte) {
        tabela.setRowHeight(26);
        tabela.getTableHeader().setFont(fonte.deriveFont(Font.BOLD));
    }
}
