package br.com.dual.sgpe.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.dual.sgpe.database.DatabaseConnection;
import br.com.dual.sgpe.database.DatabaseMigrator;
import br.com.dual.sgpe.model.entity.Usuario;
import br.com.dual.sgpe.model.enums.PerfilUsuario;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UsuarioDaoTest {

    @TempDir
    Path tempDir;

    private DatabaseConnection connection;
    private UsuarioDao dao;

    @BeforeEach
    void setUp() {
        String url = "jdbc:sqlite:" + tempDir.resolve("test.db");
        connection = new DatabaseConnection(url);
        new DatabaseMigrator(connection).migrate();
        dao = new UsuarioDao(connection);
    }

    private Usuario novoUsuario(String login, String cpf) {
        return new Usuario("João Silva", cpf, "joao@exemplo.com", "Desenvolvedor",
            login, "senha123", PerfilUsuario.COLABORADOR);
    }

    @Test
    void inserirPersisteRetornaIdEBuscaPorId() {
        int id = dao.inserir(novoUsuario("joao", "111"));

        assertTrue(id > 0);
        Optional<Usuario> achado = dao.buscarPorId(id);
        assertTrue(achado.isPresent());
        Usuario usuario = achado.get();
        assertEquals("João Silva", usuario.getNomeCompleto());
        assertEquals("joao", usuario.getLogin());
        assertEquals("111", usuario.getCpf());
        assertEquals(PerfilUsuario.COLABORADOR, usuario.getPerfil());
    }

    @Test
    void listarTodosRetornaInseridos() {
        dao.inserir(novoUsuario("ana", "1"));
        dao.inserir(novoUsuario("bia", "2"));

        List<Usuario> todos = dao.listarTodos();

        assertEquals(2, todos.size());
    }

    @Test
    void atualizarAlteraCamposMantemId() {
        int id = dao.inserir(novoUsuario("joao", "111"));
        Usuario usuario = dao.buscarPorId(id).orElseThrow();
        usuario.setNomeCompleto("João Editado");
        usuario.setPerfil(PerfilUsuario.GERENTE);

        dao.atualizar(usuario);

        Usuario depois = dao.buscarPorId(id).orElseThrow();
        assertEquals(id, depois.getId());
        assertEquals("João Editado", depois.getNomeCompleto());
        assertEquals(PerfilUsuario.GERENTE, depois.getPerfil());
    }

    @Test
    void excluirRemoveRegistro() {
        int id = dao.inserir(novoUsuario("joao", "111"));

        dao.excluir(id);

        assertTrue(dao.buscarPorId(id).isEmpty());
    }

    @Test
    void existsByLoginECpf() {
        dao.inserir(novoUsuario("joao", "111"));

        assertTrue(dao.existsByLogin("joao"));
        assertFalse(dao.existsByLogin("maria"));
        assertTrue(dao.existsByCpf("111"));
        assertFalse(dao.existsByCpf("999"));
    }

    @Test
    void existsExcetoIdIgnoraProprioRegistro() {
        int id = dao.inserir(novoUsuario("joao", "111"));

        assertFalse(dao.existsByLoginExcetoId("joao", id));
        assertFalse(dao.existsByCpfExcetoId("111", id));

        int outro = dao.inserir(novoUsuario("maria", "222"));
        assertTrue(dao.existsByLoginExcetoId("joao", outro));
    }

    @Test
    void isReferenciadoDetectaTarefa() throws SQLException {
        int usuarioId = dao.inserir(novoUsuario("joao", "111"));
        int semReferencia = dao.inserir(novoUsuario("maria", "222"));

        try (Connection conexao = connection.getConnection();
             Statement statement = conexao.createStatement()) {
            statement.executeUpdate(
                "INSERT INTO projetos (nome, descricao, data_inicio, data_termino_prevista, status) "
                    + "VALUES ('Projeto', 'Descricao', '2026-01-01', '2026-02-01', 'ABERTO')");
            statement.executeUpdate(
                "INSERT INTO tarefas (titulo, descricao, projeto_id, responsavel_id, data_inicio, data_termino_prevista, status) "
                    + "VALUES ('Tarefa', 'Descricao', 1, " + usuarioId + ", '2026-01-01', '2026-02-01', 'ABERTO')");
        }

        assertTrue(dao.isReferenciado(usuarioId));
        assertFalse(dao.isReferenciado(semReferencia));
    }
}
