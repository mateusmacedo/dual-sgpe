package br.com.dual.sgpe.dao;

import br.com.dual.sgpe.database.DatabaseConnection;
import br.com.dual.sgpe.exception.PersistenciaException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO responsável pela tabela de junção {@code projeto_equipe}, que representa
 * o relacionamento N:N entre projetos e equipes. Opera exclusivamente sobre o
 * par {@code (projeto_id, equipe_id)}, sem chave primária própria.
 */
public class ProjetoEquipeDao {

    private final DatabaseConnection databaseConnection;

    public ProjetoEquipeDao(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    /**
     * Cria o vínculo entre projeto e equipe inserindo uma linha na tabela de
     * junção {@code projeto_equipe}.
     *
     * @param projetoId id do projeto
     * @param equipeId  id da equipe a vincular
     * @throws PersistenciaException em falha de SQL (ex.: violação de unicidade)
     */
    public void vincular(int projetoId, int equipeId) {
        String sql = "INSERT INTO projeto_equipe (projeto_id, equipe_id) VALUES (?, ?)";
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, projetoId);
            statement.setInt(2, equipeId);
            statement.executeUpdate();
        } catch (SQLException excecao) {
            throw new PersistenciaException("Erro ao vincular equipe ao projeto.", excecao);
        }
    }

    /**
     * Remove o vínculo entre projeto e equipe de {@code projeto_equipe}.
     *
     * @param projetoId id do projeto
     * @param equipeId  id da equipe a desvincular
     * @throws PersistenciaException em falha de SQL
     */
    public void desvincular(int projetoId, int equipeId) {
        String sql = "DELETE FROM projeto_equipe WHERE projeto_id = ? AND equipe_id = ?";
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, projetoId);
            statement.setInt(2, equipeId);
            statement.executeUpdate();
        } catch (SQLException excecao) {
            throw new PersistenciaException("Erro ao desvincular equipe do projeto.", excecao);
        }
    }

    /**
     * Verifica se já existe vínculo entre o projeto e a equipe na tabela
     * {@code projeto_equipe}.
     *
     * @param projetoId id do projeto
     * @param equipeId  id da equipe
     * @return {@code true} se o par estiver registrado na tabela de junção
     * @throws PersistenciaException em falha de SQL
     */
    public boolean existeVinculo(int projetoId, int equipeId) {
        String sql = "SELECT COUNT(*) FROM projeto_equipe WHERE projeto_id = ? AND equipe_id = ?";
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, projetoId);
            statement.setInt(2, equipeId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        } catch (SQLException excecao) {
            throw new PersistenciaException("Erro ao verificar vínculo.", excecao);
        }
    }

    /**
     * Retorna os ids de todas as equipes vinculadas ao projeto.
     *
     * @param projetoId id do projeto
     * @return lista (possivelmente vazia) de ids de equipes, ordenada por id
     * @throws PersistenciaException em falha de SQL
     */
    public List<Integer> listarEquipeIds(int projetoId) {
        String sql = "SELECT equipe_id FROM projeto_equipe WHERE projeto_id = ? ORDER BY equipe_id";
        return listarIds(sql, projetoId);
    }

    /**
     * Retorna os ids de todos os projetos aos quais a equipe está vinculada.
     *
     * @param equipeId id da equipe
     * @return lista (possivelmente vazia) de ids de projetos, ordenada por id
     * @throws PersistenciaException em falha de SQL
     */
    public List<Integer> listarProjetoIds(int equipeId) {
        String sql = "SELECT projeto_id FROM projeto_equipe WHERE equipe_id = ? ORDER BY projeto_id";
        return listarIds(sql, equipeId);
    }

    private List<Integer> listarIds(String sql, int parametro) {
        List<Integer> ids = new ArrayList<>();
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, parametro);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(resultSet.getInt(1));
                }
            }
        } catch (SQLException excecao) {
            throw new PersistenciaException("Erro ao listar vínculos.", excecao);
        }
        return ids;
    }
}
