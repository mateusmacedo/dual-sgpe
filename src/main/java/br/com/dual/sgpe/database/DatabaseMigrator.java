package br.com.dual.sgpe.database;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Executa o script {@code db/schema.sql} na inicialização para garantir que as
 * tabelas estejam criadas.
 *
 * <p>O construtor sem argumentos usa a {@link DatabaseConnection} padrão. O
 * construtor com {@link DatabaseConnection} permite injetar uma conexão
 * alternativa (por exemplo, um banco temporário em testes).
 */
public class DatabaseMigrator {

    private final DatabaseConnection databaseConnection;

    public DatabaseMigrator() {
        this(new DatabaseConnection());
    }

    public DatabaseMigrator(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    /**
     * Lê {@code db/schema.sql} do classpath e executa cada instrução DDL
     * separadamente contra o banco de dados.
     *
     * <p>O script usa {@code CREATE TABLE IF NOT EXISTS} em todas as tabelas,
     * tornando este método idempotente: pode ser chamado a cada inicialização
     * sem risco de duplicação ou erro em bancos já migrados.
     *
     * <p>O SQL é dividido por {@code ;} para contornar a limitação do JDBC SQLite,
     * que não suporta múltiplos statements em uma única chamada a
     * {@link Statement#execute}. Tokens vazios (resultado de espaços ao redor
     * do último {@code ;}) são descartados antes da execução.
     *
     * @throws IllegalStateException se o script SQL não for encontrado ou se
     *                               alguma instrução DDL falhar
     */
    public void migrate() {
        String sql = loadSchemaSql();

        try (
            Connection connection = databaseConnection.getConnection();
            Statement statement = connection.createStatement()
        ) {
            // SQLite JDBC não aceita múltiplos statements em um único execute();
            // divide por ";" e executa cada comando individualmente.
            String[] commands = sql.split(";");

            for (String command : commands) {
                String normalizedCommand = command.trim();

                if (!normalizedCommand.isEmpty()) {
                    statement.execute(normalizedCommand);
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Erro ao executar migração inicial do banco.", exception);
        }
    }

    /**
     * Carrega o conteúdo de {@code db/schema.sql} a partir do classpath como
     * uma única {@code String} UTF-8.
     *
     * <p>O arquivo é empacotado em {@code src/main/resources/db/schema.sql} e
     * incluído no JAR/classes durante o build do Maven, garantindo que o
     * classloader consiga localizá-lo em qualquer ambiente de execução.
     *
     * @return conteúdo completo do script DDL
     * @throws IllegalStateException se o recurso não for encontrado no classpath
     *                               ou ocorrer erro de leitura
     */
    private String loadSchemaSql() {
        InputStream inputStream = getClass()
            .getClassLoader()
            .getResourceAsStream("db/schema.sql");

        if (inputStream == null) {
            throw new IllegalStateException("Arquivo db/schema.sql não encontrado.");
        }

        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(inputStream, StandardCharsets.UTF_8)
        )) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception exception) {
            throw new IllegalStateException("Erro ao carregar arquivo db/schema.sql.", exception);
        }
    }
}
