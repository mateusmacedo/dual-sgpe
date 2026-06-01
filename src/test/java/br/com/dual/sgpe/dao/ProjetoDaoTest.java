package br.com.dual.sgpe.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.dual.sgpe.database.DatabaseConnection;
import br.com.dual.sgpe.database.DatabaseMigrator;
import br.com.dual.sgpe.model.entity.Projeto;
import br.com.dual.sgpe.model.enums.StatusProjeto;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjetoDaoTest {

    @TempDir
    Path tempDir;

    private DatabaseConnection connection;
    private ProjetoDao dao;

    @BeforeEach
    void setUp() {
        String url = "jdbc:sqlite:" + tempDir.resolve("test.db");
        connection = new DatabaseConnection(url);
        new DatabaseMigrator(connection).migrate();
        dao = new ProjetoDao(connection);
    }

    private Projeto novo(String nome, StatusProjeto status) {
        return new Projeto(nome, "Descrição", LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 30), status);
    }

    @Test
    void inserirEBuscarPorIdPreservaCampos() {
        int id = dao.inserir(novo("Projeto A", StatusProjeto.PLANEJADO));

        Projeto encontrado = dao.buscarPorId(id).orElseThrow();
        assertEquals("Projeto A", encontrado.getNome());
        assertEquals(LocalDate.of(2026, 6, 1), encontrado.getDataInicio());
        assertEquals(LocalDate.of(2026, 6, 30), encontrado.getDataTerminoPrevista());
        assertEquals(StatusProjeto.PLANEJADO, encontrado.getStatus());
    }

    @Test
    void listarTodosRetornaInseridos() {
        dao.inserir(novo("A", StatusProjeto.PLANEJADO));
        dao.inserir(novo("B", StatusProjeto.EM_ANDAMENTO));

        assertEquals(2, dao.listarTodos().size());
    }

    @Test
    void atualizarPersisteMudancas() {
        int id = dao.inserir(novo("A", StatusProjeto.PLANEJADO));
        Projeto projeto = dao.buscarPorId(id).orElseThrow();
        projeto.setStatus(StatusProjeto.CONCLUIDO);

        dao.atualizar(projeto);

        assertEquals(StatusProjeto.CONCLUIDO, dao.buscarPorId(id).orElseThrow().getStatus());
    }

    @Test
    void excluirRemove() {
        int id = dao.inserir(novo("A", StatusProjeto.PLANEJADO));

        dao.excluir(id);

        assertTrue(dao.buscarPorId(id).isEmpty());
    }

    @Test
    void findByStatusFiltra() {
        dao.inserir(novo("A", StatusProjeto.PLANEJADO));
        dao.inserir(novo("B", StatusProjeto.CONCLUIDO));

        assertEquals(1, dao.findByStatus(StatusProjeto.PLANEJADO).size());
        assertEquals("A", dao.findByStatus(StatusProjeto.PLANEJADO).get(0).getNome());
    }

    @Test
    void isReferenciadoQuandoVinculadoAEquipe() throws SQLException {
        int id = dao.inserir(novo("A", StatusProjeto.PLANEJADO));
        try (Connection conexao = connection.getConnection();
             Statement statement = conexao.createStatement()) {
            statement.executeUpdate("INSERT INTO equipes (nome, descricao) VALUES ('Equipe', 'Desc')");
            statement.executeUpdate(
                "INSERT INTO projeto_equipe (projeto_id, equipe_id) VALUES (" + id + ", 1)");
        }

        assertTrue(dao.isReferenciado(id));
    }

    @Test
    void isReferenciadoFalseSemVinculo() {
        int id = dao.inserir(novo("A", StatusProjeto.PLANEJADO));

        assertFalse(dao.isReferenciado(id));
    }
}
