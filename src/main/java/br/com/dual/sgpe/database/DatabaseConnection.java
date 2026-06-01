package br.com.dual.sgpe.database;

import java.sql.Connection;
import java.sql.SQLException;
import org.sqlite.SQLiteConfig;

/**
 * Centraliza a criação de conexões com o banco SQLite e habilita chaves estrangeiras.
 *
 * <p>O construtor sem argumentos usa o banco padrão da aplicação
 * ({@code jdbc:sqlite:data/sgpe.db}). O construtor com URL permite apontar para
 * um banco alternativo (por exemplo, um arquivo temporário em testes).
 */
public class DatabaseConnection {

    private static final String DEFAULT_DATABASE_URL = "jdbc:sqlite:data/sgpe.db";

    private final String databaseUrl;

    public DatabaseConnection() {
        this(DEFAULT_DATABASE_URL);
    }

    public DatabaseConnection(String databaseUrl) {
        this.databaseUrl = databaseUrl;
    }

    /**
     * Abre e retorna uma nova conexão JDBC com o banco SQLite configurado.
     *
     * <p>A conexão é aberta com {@link SQLiteConfig#enforceForeignKeys(boolean)}
     * habilitado, porque o SQLite desabilita a verificação de chaves estrangeiras
     * por padrão; sem isso, violações de FK passariam silenciosamente. O chamador
     * é responsável por fechar a conexão (use try-with-resources).
     *
     * @return conexão ativa com suporte a chaves estrangeiras habilitado
     * @throws IllegalStateException se a conexão JDBC falhar
     */
    public Connection getConnection() {
        try {
            SQLiteConfig config = new SQLiteConfig();
            config.enforceForeignKeys(true);
            return config.createConnection(databaseUrl);
        } catch (SQLException exception) {
            throw new IllegalStateException("Erro ao conectar ao banco SQLite.", exception);
        }
    }
}
