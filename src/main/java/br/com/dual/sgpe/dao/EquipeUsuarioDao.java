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
 * DAO responsável pela tabela de junção {@code equipe_usuario}, que representa
 * o relacionamento N:N entre equipes e usuários. Todas as operações manipulam
 * exclusivamente o par {@code (equipe_id, usuario_id)}, sem chave primária própria.
 */
public class EquipeUsuarioDao {

    private final DatabaseConnection databaseConnection;

    public EquipeUsuarioDao(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    /**
     * Cria o vínculo entre equipe e usuário inserindo uma linha na tabela de
     * junção {@code equipe_usuario}.
     *
     * @param equipeId  id da equipe
     * @param usuarioId id do usuário a vincular
     * @throws PersistenciaException em falha de SQL (ex.: violação de unicidade)
     */
    public void vincular(int equipeId, int usuarioId) {
        String sql = "INSERT INTO equipe_usuario (equipe_id, usuario_id) VALUES (?, ?)";
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, equipeId);
            statement.setInt(2, usuarioId);
            statement.executeUpdate();
        } catch (SQLException excecao) {
            throw new PersistenciaException("Erro ao vincular usuário à equipe.", excecao);
        }
    }

    /**
     * Remove o vínculo entre equipe e usuário da tabela {@code equipe_usuario}.
     *
     * @param equipeId  id da equipe
     * @param usuarioId id do usuário a desvincular
     * @throws PersistenciaException em falha de SQL
     */
    public void desvincular(int equipeId, int usuarioId) {
        String sql = "DELETE FROM equipe_usuario WHERE equipe_id = ? AND usuario_id = ?";
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, equipeId);
            statement.setInt(2, usuarioId);
            statement.executeUpdate();
        } catch (SQLException excecao) {
            throw new PersistenciaException("Erro ao desvincular usuário da equipe.", excecao);
        }
    }

    /**
     * Verifica se já existe vínculo entre a equipe e o usuário na tabela
     * {@code equipe_usuario}.
     *
     * @param equipeId  id da equipe
     * @param usuarioId id do usuário
     * @return {@code true} se o par estiver presente na tabela de junção
     * @throws PersistenciaException em falha de SQL
     */
    public boolean existeVinculo(int equipeId, int usuarioId) {
        String sql = "SELECT COUNT(*) FROM equipe_usuario WHERE equipe_id = ? AND usuario_id = ?";
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, equipeId);
            statement.setInt(2, usuarioId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        } catch (SQLException excecao) {
            throw new PersistenciaException("Erro ao verificar vínculo.", excecao);
        }
    }

    /**
     * Retorna os ids de todos os usuários vinculados à equipe.
     *
     * @param equipeId id da equipe
     * @return lista (possivelmente vazia) de ids de usuários, ordenada por id
     * @throws PersistenciaException em falha de SQL
     */
    public List<Integer> listarUsuarioIds(int equipeId) {
        String sql = "SELECT usuario_id FROM equipe_usuario WHERE equipe_id = ? ORDER BY usuario_id";
        List<Integer> ids = new ArrayList<>();
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, equipeId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(resultSet.getInt("usuario_id"));
                }
            }
        } catch (SQLException excecao) {
            throw new PersistenciaException("Erro ao listar usuários da equipe.", excecao);
        }
        return ids;
    }

    /**
     * Retorna os ids de todas as equipes às quais o usuário pertence.
     *
     * @param usuarioId id do usuário
     * @return lista (possivelmente vazia) de ids de equipes, ordenada por id
     * @throws PersistenciaException em falha de SQL
     */
    public List<Integer> listarEquipeIdsDoUsuario(int usuarioId) {
        String sql = "SELECT equipe_id FROM equipe_usuario WHERE usuario_id = ? ORDER BY equipe_id";
        List<Integer> ids = new ArrayList<>();
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, usuarioId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(resultSet.getInt("equipe_id"));
                }
            }
        } catch (SQLException excecao) {
            throw new PersistenciaException("Erro ao listar equipes do usuário.", excecao);
        }
        return ids;
    }

    /**
     * Remove todos os vínculos da equipe em {@code equipe_usuario}. Utilizado
     * antes de excluir a equipe para limpar a tabela de junção.
     *
     * @param equipeId id da equipe cujos vínculos serão removidos
     * @throws PersistenciaException em falha de SQL
     */
    public void removerTodosDaEquipe(int equipeId) {
        String sql = "DELETE FROM equipe_usuario WHERE equipe_id = ?";
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, equipeId);
            statement.executeUpdate();
        } catch (SQLException excecao) {
            throw new PersistenciaException("Erro ao remover todos os vínculos da equipe.", excecao);
        }
    }
}
