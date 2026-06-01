package br.com.dual.sgpe.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.dual.sgpe.database.DatabaseConnection;
import br.com.dual.sgpe.database.DatabaseMigrator;
import br.com.dual.sgpe.model.entity.Equipe;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EquipeDaoTest {

    @TempDir
    Path tempDir;

    private DatabaseConnection connection;
    private EquipeDao dao;

    @BeforeEach
    void setUp() {
        String url = "jdbc:sqlite:" + tempDir.resolve("test.db");
        connection = new DatabaseConnection(url);
        new DatabaseMigrator(connection).migrate();
        dao = new EquipeDao(connection);
    }

    private Equipe nova(String nome) {
        return new Equipe(nome, "Descrição");
    }

    @Test
    void inserirEBuscarPorIdPreservaCampos() {
        int id = dao.inserir(nova("Equipe A"));

        Equipe encontrada = dao.buscarPorId(id).orElseThrow();
        assertEquals("Equipe A", encontrada.getNome());
        assertEquals("Descrição", encontrada.getDescricao());
    }

    @Test
    void listarTodosRetornaInseridos() {
        dao.inserir(nova("A"));
        dao.inserir(nova("B"));

        assertEquals(2, dao.listarTodos().size());
    }

    @Test
    void atualizarPersisteMudancas() {
        int id = dao.inserir(nova("A"));
        Equipe equipe = dao.buscarPorId(id).orElseThrow();
        equipe.setNome("B");

        dao.atualizar(equipe);

        assertEquals("B", dao.buscarPorId(id).orElseThrow().getNome());
    }

    @Test
    void excluirRemove() {
        int id = dao.inserir(nova("A"));

        dao.excluir(id);

        assertTrue(dao.buscarPorId(id).isEmpty());
    }

    @Test
    void isReferenciadoQuandoVinculadoAProjeto() throws SQLException {
        int equipeId = dao.inserir(nova("Equipe A"));
        try (Connection conexao = connection.getConnection();
             Statement statement = conexao.createStatement()) {
            statement.executeUpdate(
                "INSERT INTO projetos (nome, descricao, data_inicio, data_termino_prevista, status) "
                    + "VALUES ('Projeto', 'Desc', '2026-01-01', '2026-12-31', 'PLANEJADO')");
            statement.executeUpdate(
                "INSERT INTO projeto_equipe (projeto_id, equipe_id) VALUES (1, " + equipeId + ")");
        }

        assertTrue(dao.isReferenciado(equipeId));
    }

    @Test
    void isReferenciadoFalseSemVinculo() {
        int id = dao.inserir(nova("A"));

        assertFalse(dao.isReferenciado(id));
    }
}
