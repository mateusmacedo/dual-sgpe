package br.com.dual.sgpe.database;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
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
     * <p>O SQL é dividido em statements por {@link #dividirStatements} para
     * contornar a limitação do JDBC SQLite, que não suporta múltiplos statements
     * em uma única chamada a {@link Statement#execute}. A divisão respeita
     * {@code ;} dentro de strings e ignora comentários; fragmentos vazios são
     * descartados antes da execução.
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
            // divide respeitando strings e comentários e executa um a um.
            for (String command : dividirStatements(sql)) {
                statement.execute(command);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Erro ao executar migração inicial do banco.", exception);
        }
    }

    /**
     * Divide um script SQL em statements individuais, separando por {@code ;}.
     *
     * <p>Ao contrário de um {@code split(";")} ingênuo, ignora {@code ;} contido
     * em strings e identificadores entre aspas, trata aspas duplicadas como
     * escape literal e descarta comentários de linha e de bloco. Fragmentos
     * vazios (inclusive os que continham apenas comentários) não são retornados,
     * evitando chamadas a {@link Statement#execute} sem SQL executável.
     *
     * <p>Visibilidade de pacote para permitir testes unitários do divisor.
     *
     * @param sql script SQL completo
     * @return lista de statements não vazios, sem comentários e com espaços ao redor removidos
     */
    List<String> dividirStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        int i = 0;
        int tamanho = sql.length();

        while (i < tamanho) {
            char caractere = sql.charAt(i);

            // Comentário de linha: descarta do hífen duplo até o fim da linha.
            if (caractere == '-' && i + 1 < tamanho && sql.charAt(i + 1) == '-') {
                i += 2;
                while (i < tamanho && sql.charAt(i) != '\n') {
                    i++;
                }
                continue;
            }

            // Comentário de bloco estilo C: descarta da abertura até o fechamento.
            if (caractere == '/' && i + 1 < tamanho && sql.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < tamanho && !(sql.charAt(i) == '*' && sql.charAt(i + 1) == '/')) {
                    i++;
                }
                i += 2;
                continue;
            }

            // String ou identificador entre aspas: copia literalmente até fechar,
            // tratando aspas duplicadas como escape (não encerram o literal).
            if (caractere == '\'' || caractere == '"') {
                char aspas = caractere;
                atual.append(caractere);
                i++;
                while (i < tamanho) {
                    char interno = sql.charAt(i);
                    atual.append(interno);
                    i++;
                    if (interno == aspas) {
                        if (i < tamanho && sql.charAt(i) == aspas) {
                            atual.append(aspas);
                            i++;
                        } else {
                            break;
                        }
                    }
                }
                continue;
            }

            // Separador de statement, fora de strings e comentários.
            if (caractere == ';') {
                adicionarSeNaoVazio(statements, atual);
                atual.setLength(0);
                i++;
                continue;
            }

            atual.append(caractere);
            i++;
        }

        adicionarSeNaoVazio(statements, atual);
        return statements;
    }

    private void adicionarSeNaoVazio(List<String> statements, StringBuilder fragmento) {
        String statement = fragmento.toString().trim();
        if (!statement.isEmpty()) {
            statements.add(statement);
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
