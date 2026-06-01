package br.com.dual.sgpe.dao;

import br.com.dual.sgpe.database.DatabaseConnection;
import br.com.dual.sgpe.exception.PersistenciaException;
import br.com.dual.sgpe.model.enums.StatusTarefa;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO de leitura agregada para geração do relatório de desempenho de projetos.
 * Executa apenas consultas de contagem e listagem sobre a tabela {@code tarefas};
 * não realiza operações de escrita.
 */
public class RelatorioDao {

    private final DatabaseConnection databaseConnection;

    public RelatorioDao(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    /**
     * Conta o total de tarefas do projeto, independentemente do status.
     *
     * @param projetoId id do projeto
     * @return número total de tarefas
     * @throws PersistenciaException em falha de SQL
     */
    public int contarTarefasPorProjeto(int projetoId) {
        return contar("SELECT COUNT(*) FROM tarefas WHERE projeto_id = ?", projetoId);
    }

    /**
     * Conta tarefas com status {@link StatusTarefa#CONCLUIDA} no projeto.
     * Usado para calcular o percentual de conclusão no relatório de desempenho.
     *
     * @param projetoId id do projeto
     * @return número de tarefas concluídas
     * @throws PersistenciaException em falha de SQL
     */
    public int contarConcluidasPorProjeto(int projetoId) {
        return contar("SELECT COUNT(*) FROM tarefas WHERE projeto_id = ? AND status = ?",
            projetoId, StatusTarefa.CONCLUIDA.name());
    }

    /**
     * Conta tarefas ainda não concluídas ({@link StatusTarefa#PENDENTE} ou
     * {@link StatusTarefa#EM_ANDAMENTO}) no projeto. Usa cláusula {@code IN}
     * com dois parâmetros de status em vez de um único {@code != CONCLUIDA}
     * para ser explícito quanto aos estados considerados ativos.
     *
     * @param projetoId id do projeto
     * @return número de tarefas pendentes ou em andamento
     * @throws PersistenciaException em falha de SQL
     */
    public int contarPendentesPorProjeto(int projetoId) {
        return contar("SELECT COUNT(*) FROM tarefas WHERE projeto_id = ? AND status IN (?, ?)",
            projetoId, StatusTarefa.PENDENTE.name(), StatusTarefa.EM_ANDAMENTO.name());
    }

    /**
     * Retorna ids únicos dos responsáveis por tarefas do projeto. O {@code DISTINCT}
     * garante que cada responsável apareça uma única vez, mesmo com múltiplas tarefas.
     *
     * @param projetoId id do projeto
     * @return lista de ids de responsáveis distintos, ordenada por id
     * @throws PersistenciaException em falha de SQL
     */
    public List<Integer> listarResponsavelIds(int projetoId) {
        // DISTINCT elimina duplicatas quando um usuário é responsável por mais de uma tarefa
        String sql = "SELECT DISTINCT responsavel_id FROM tarefas WHERE projeto_id = ? ORDER BY responsavel_id";
        List<Integer> ids = new ArrayList<>();
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, projetoId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(resultSet.getInt(1));
                }
            }
        } catch (SQLException excecao) {
            throw new PersistenciaException("Erro ao listar responsáveis do projeto.", excecao);
        }
        return ids;
    }

    /**
     * Executa uma contagem parametrizada e retorna o valor da primeira coluna.
     * Os parâmetros são vinculados na ordem informada via {@code setObject}.
     *
     * @param sql    consulta de contagem com placeholders {@code ?}
     * @param params valores a vincular aos placeholders, na ordem
     * @return o resultado da contagem, ou 0 se não houver linhas
     * @throws PersistenciaException em falha de SQL
     */
    private int contar(String sql, Object... params) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        } catch (SQLException excecao) {
            throw new PersistenciaException("Erro ao contar tarefas.", excecao);
        }
    }
}
