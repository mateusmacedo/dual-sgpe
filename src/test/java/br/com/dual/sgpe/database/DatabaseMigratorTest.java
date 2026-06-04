package br.com.dual.sgpe.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatabaseMigratorTest {

    @TempDir
    Path tempDir;

    private DatabaseConnection connection;
    private DatabaseMigrator migrator;

    @BeforeEach
    void setUp() {
        connection = new DatabaseConnection("jdbc:sqlite:" + tempDir.resolve("test.db"));
        migrator = new DatabaseMigrator(connection);
    }

    @Test
    void migrateExecutaSchemaRealEhIdempotente() throws SQLException {
        migrator.migrate();
        // Roda novamente: CREATE TABLE/INDEX IF NOT EXISTS torna a migração idempotente.
        migrator.migrate();

        try (Connection conexao = connection.getConnection();
             Statement statement = conexao.createStatement();
             ResultSet resultSet = statement.executeQuery(
                 "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'usuarios'")) {
            assertTrue(resultSet.next());
        }
    }

    @Test
    void dividirIgnoraPontoEVirgulaEmString() {
        List<String> statements = migrator.dividirStatements(
            "INSERT INTO t (v) VALUES ('a;b');\nINSERT INTO t (v) VALUES ('c')");

        assertEquals(2, statements.size());
        assertEquals("INSERT INTO t (v) VALUES ('a;b')", statements.get(0));
        assertEquals("INSERT INTO t (v) VALUES ('c')", statements.get(1));
    }

    @Test
    void dividirTrataAspasDuplicadasComoEscape() {
        List<String> statements = migrator.dividirStatements(
            "INSERT INTO t (v) VALUES ('O''Brien; Jr');\nSELECT 1");

        assertEquals(2, statements.size());
        assertEquals("INSERT INTO t (v) VALUES ('O''Brien; Jr')", statements.get(0));
        assertEquals("SELECT 1", statements.get(1));
    }

    @Test
    void dividirIgnoraComentariosDeLinhaEBloco() {
        List<String> statements = migrator.dividirStatements(
            "-- comentario; com ponto e virgula\n"
                + "CREATE TABLE a (id INTEGER);\n"
                + "/* bloco; tambem com ponto e virgula */\n"
                + "CREATE TABLE b (id INTEGER);");

        assertEquals(2, statements.size());
        assertTrue(statements.get(0).startsWith("CREATE TABLE a"));
        assertTrue(statements.get(1).startsWith("CREATE TABLE b"));
    }

    @Test
    void dividirDescartaFragmentosVaziosESoComentario() {
        List<String> statements = migrator.dividirStatements(
            "CREATE TABLE a (id INTEGER);; \n  ;\n-- apenas comentario\n");

        assertEquals(1, statements.size());
        assertEquals("CREATE TABLE a (id INTEGER)", statements.get(0));
    }
}
