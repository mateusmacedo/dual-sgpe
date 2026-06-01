package br.com.dual.sgpe.dao;

import br.com.dual.sgpe.database.DatabaseConnection;
import br.com.dual.sgpe.exception.PersistenciaException;
import br.com.dual.sgpe.model.entity.Tarefa;
import br.com.dual.sgpe.model.enums.StatusTarefa;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO de persistência para a tabela {@code tarefas}. Fornece CRUD completo e
 * consultas por projeto e responsável. Datas são armazenadas como TEXT ISO-8601
 * e o status como TEXT via {@code name()} do enum {@link StatusTarefa}.
 */
public class TarefaDao {

    private static final String COLUNAS =
        "id, titulo, descricao, projeto_id, responsavel_id, data_inicio, data_termino_prevista, status";

    private final DatabaseConnection databaseConnection;

    public TarefaDao(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    /**
     * Persiste uma nova tarefa e atualiza o campo {@code id} da entidade.
     *
     * @param tarefa entidade a inserir (id ignorado; será preenchido após inserção)
     * @return id gerado pelo banco de dados
     * @throws PersistenciaException em falha de SQL ou se o id não for retornado
     */
    public int inserir(Tarefa tarefa) {
        String sql = "INSERT INTO tarefas (titulo, descricao, projeto_id, responsavel_id, "
            + "data_inicio, data_termino_prevista, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = databaseConnection.getConnection();
             // RETURN_GENERATED_KEYS: recupera o id autoincrement gerado pelo SQLite
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preencher(statement, tarefa);
            statement.executeUpdate();
            try (ResultSet chaves = statement.getGeneratedKeys()) {
                if (chaves.next()) {
                    int id = chaves.getInt(1);
                    tarefa.setId(id);
                    return id;
                }
                throw new PersistenciaException("Falha ao obter o id gerado da tarefa.");
            }
        } catch (SQLException excecao) {
            throw new PersistenciaException("Erro ao inserir tarefa.", excecao);
        }
    }

    /**
     * Atualiza todos os campos da tarefa identificada por {@code tarefa.getId()}.
     *
     * @param tarefa entidade com os novos valores e id existente
     * @throws PersistenciaException em falha de SQL
     */
    public void atualizar(Tarefa tarefa) {
        String sql = "UPDATE tarefas SET titulo = ?, descricao = ?, projeto_id = ?, responsavel_id = ?, "
            + "data_inicio = ?, data_termino_prevista = ?, status = ? WHERE id = ?";
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            preencher(statement, tarefa);
            statement.setInt(8, tarefa.getId());
            statement.executeUpdate();
        } catch (SQLException excecao) {
            throw new PersistenciaException("Erro ao atualizar tarefa.", excecao);
        }
    }

    /**
     * Remove a tarefa pelo id.
     *
     * @param id identificador da tarefa a remover
     * @throws PersistenciaException em falha de SQL
     */
    public void excluir(int id) {
        String sql = "DELETE FROM tarefas WHERE id = ?";
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException excecao) {
            throw new PersistenciaException("Erro ao excluir tarefa.", excecao);
        }
    }

    /**
     * Busca uma tarefa pelo id.
     *
     * @param id identificador da tarefa
     * @return {@link Optional} contendo a entidade, ou vazio se não encontrada
     * @throws PersistenciaException em falha de SQL
     */
    public Optional<Tarefa> buscarPorId(int id) {
        String sql = "SELECT " + COLUNAS + " FROM tarefas WHERE id = ?";
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapear(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException excecao) {
            throw new PersistenciaException("Erro ao buscar tarefa por id.", excecao);
        }
    }

    /**
     * Lista todas as tarefas do projeto, ordenadas por id.
     *
     * @param projetoId id do projeto
     * @return lista (possivelmente vazia) de tarefas do projeto
     * @throws PersistenciaException em falha de SQL
     */
    public List<Tarefa> findByProjetoId(int projetoId) {
        String sql = "SELECT " + COLUNAS + " FROM tarefas WHERE projeto_id = ? ORDER BY id";
        return listarPorParametro(sql, projetoId);
    }

    /**
     * Lista todas as tarefas atribuídas ao responsável, ordenadas por id.
     *
     * @param responsavelId id do usuário responsável
     * @return lista (possivelmente vazia) de tarefas do responsável
     * @throws PersistenciaException em falha de SQL
     */
    public List<Tarefa> findByResponsavelId(int responsavelId) {
        String sql = "SELECT " + COLUNAS + " FROM tarefas WHERE responsavel_id = ? ORDER BY id";
        return listarPorParametro(sql, responsavelId);
    }

    private List<Tarefa> listarPorParametro(String sql, int parametro) {
        List<Tarefa> tarefas = new ArrayList<>();
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, parametro);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    tarefas.add(mapear(resultSet));
                }
            }
        } catch (SQLException excecao) {
            throw new PersistenciaException("Erro ao listar tarefas.", excecao);
        }
        return tarefas;
    }

    private void preencher(PreparedStatement statement, Tarefa tarefa) throws SQLException {
        statement.setString(1, tarefa.getTitulo());
        statement.setString(2, tarefa.getDescricao());
        statement.setInt(3, tarefa.getProjetoId());
        statement.setInt(4, tarefa.getResponsavelId());
        statement.setString(5, tarefa.getDataInicio() != null ? tarefa.getDataInicio().toString() : null);
        statement.setString(6, tarefa.getDataTerminoPrevista() != null
            ? tarefa.getDataTerminoPrevista().toString() : null);
        statement.setString(7, tarefa.getStatus() != null
            ? tarefa.getStatus().name() : StatusTarefa.PENDENTE.name());
    }

    private Tarefa mapear(ResultSet resultSet) throws SQLException {
        return new Tarefa(
            resultSet.getInt("id"),
            resultSet.getString("titulo"),
            resultSet.getString("descricao"),
            resultSet.getInt("projeto_id"),
            resultSet.getInt("responsavel_id"),
            LocalDate.parse(resultSet.getString("data_inicio")),
            LocalDate.parse(resultSet.getString("data_termino_prevista")),
            StatusTarefa.valueOf(resultSet.getString("status"))
        );
    }
}
