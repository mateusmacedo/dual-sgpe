package br.com.dual.sgpe;

import br.com.dual.sgpe.controller.EquipeController;
import br.com.dual.sgpe.controller.LoginController;
import br.com.dual.sgpe.controller.ProjetoController;
import br.com.dual.sgpe.controller.RelatorioController;
import br.com.dual.sgpe.controller.TarefaController;
import br.com.dual.sgpe.controller.UsuarioController;
import br.com.dual.sgpe.dao.EquipeDao;
import br.com.dual.sgpe.dao.EquipeUsuarioDao;
import br.com.dual.sgpe.dao.ProjetoDao;
import br.com.dual.sgpe.dao.ProjetoEquipeDao;
import br.com.dual.sgpe.dao.RelatorioDao;
import br.com.dual.sgpe.dao.TarefaDao;
import br.com.dual.sgpe.dao.UsuarioDao;
import br.com.dual.sgpe.database.DatabaseConnection;
import br.com.dual.sgpe.database.DatabaseMigrator;
import br.com.dual.sgpe.model.entity.Usuario;
import br.com.dual.sgpe.model.enums.PerfilUsuario;
import br.com.dual.sgpe.security.EscopoColaborador;
import br.com.dual.sgpe.view.LoginView;
import javax.swing.SwingUtilities;

/**
 * Ponto de entrada da aplicação. Executa a migração inicial do banco, garante
 * um usuário administrador para o primeiro acesso, monta os DAOs e controllers
 * e abre a tela de login, que conduz à tela principal (hub de navegação).
 */
public class Main {

    private static final String ADMIN_LOGIN = "admin";
    private static final String ADMIN_CPF = "52998224725";

    /**
     * Inicializa e inicia a aplicação SGPE.
     *
     * <p>Sequência de inicialização:
     * <ol>
     *   <li>Executa a migração DDL (idempotente via {@code CREATE TABLE IF NOT EXISTS}).</li>
     *   <li>Instancia uma única {@link DatabaseConnection} compartilhada por todos os DAOs.</li>
     *   <li>Garante a existência do administrador inicial ({@link #semearAdmin}).</li>
     *   <li>Monta os controllers com injeção manual de dependências.</li>
     *   <li>Agenda a abertura da tela de login na Event Dispatch Thread (EDT).</li>
     * </ol>
     *
     * @param args argumentos de linha de comando (não utilizados)
     */
    public static void main(String[] args) {
        new DatabaseMigrator().migrate();

        // Uma única DatabaseConnection é reutilizada por todos os DAOs;
        // cada DAO abre/fecha sua própria conexão por operação via getConnection().
        DatabaseConnection databaseConnection = new DatabaseConnection();
        UsuarioDao usuarioDao = new UsuarioDao(databaseConnection);
        ProjetoDao projetoDao = new ProjetoDao(databaseConnection);
        TarefaDao tarefaDao = new TarefaDao(databaseConnection);
        EquipeDao equipeDao = new EquipeDao(databaseConnection);
        EquipeUsuarioDao equipeUsuarioDao = new EquipeUsuarioDao(databaseConnection);
        ProjetoEquipeDao projetoEquipeDao = new ProjetoEquipeDao(databaseConnection);
        RelatorioDao relatorioDao = new RelatorioDao(databaseConnection);

        semearAdmin(usuarioDao);

        LoginController loginController = new LoginController(usuarioDao);
        UsuarioController usuarioController = new UsuarioController(usuarioDao);
        ProjetoController projetoController =
            new ProjetoController(projetoDao, projetoEquipeDao, equipeDao);
        TarefaController tarefaController =
            new TarefaController(tarefaDao, projetoDao, usuarioDao);
        EquipeController equipeController =
            new EquipeController(equipeDao, equipeUsuarioDao, usuarioDao);
        RelatorioController relatorioController =
            new RelatorioController(projetoDao, relatorioDao, projetoEquipeDao, equipeDao, usuarioDao);
        EscopoColaborador escopoColaborador =
            new EscopoColaborador(equipeUsuarioDao, projetoEquipeDao, projetoDao);

        // Toda criação/manipulação de componentes Swing deve ocorrer na EDT;
        // invokeLater garante essa regra mesmo que main() rode em outra thread.
        SwingUtilities.invokeLater(() -> new LoginView(loginController, usuarioController,
            projetoController, tarefaController, equipeController, relatorioController,
            escopoColaborador).setVisible(true));
    }

    /**
     * Cria um administrador inicial (login {@code admin} / senha {@code admin})
     * quando o banco ainda não tem esse usuário. Idempotente: não insere se o
     * login ou o CPF reservado já existirem, evitando violar as restrições
     * UNIQUE. Garante que a aplicação seja utilizável em um banco novo.
     */
    private static void semearAdmin(UsuarioDao usuarioDao) {
        if (usuarioDao.existsByLogin(ADMIN_LOGIN) || usuarioDao.existsByCpf(ADMIN_CPF)) {
            return;
        }
        usuarioDao.inserir(new Usuario("Administrador do Sistema", ADMIN_CPF,
            "admin@sgpe.local", "Administrador", ADMIN_LOGIN, "admin",
            PerfilUsuario.ADMINISTRADOR));
    }
}
