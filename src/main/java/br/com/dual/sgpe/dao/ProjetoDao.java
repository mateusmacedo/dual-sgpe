package br.com.dual.sgpe.dao;

import br.com.dual.sgpe.database.DatabaseConnection;
import br.com.dual.sgpe.exception.PersistenciaException;
import br.com.dual.sgpe.model.entity.Projeto;
import br.com.dual.sgpe.model.enums.StatusProjeto;
import br.com.dual.sgpe.model.filter.ProjetoFiltro;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Acesso a dados da tabela {@code projetos}. Recebe e devolve entidades
 * {@link Projeto}; converte datas {@link LocalDate} para TEXT ISO e o status
 * para TEXT (via {@code name()}). Abre uma conexão por operação.
 */
public class ProjetoDao {

    private static final String COLUNAS =
        "id, nome, descricao, data_inicio, data_termino_prevista, status";

    private final DatabaseConnection databaseConnection;

    public ProjetoDao(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    /**
     * Persiste um novo projeto e atualiza o campo {@code id} da entidade.
     *
     * @param projeto entidade a inserir (id ignorado; será preenchido após inserção)
     * @return id gerado pelo banco de dados
     * @throws PersistenciaException em falha de SQL ou se o id não for retornado
     */
    public int inserir(Projeto projeto) {
        String sql = "INSERT INTO projetos (nome, descricao, data_inicio, data_termino_prevista, status) "
            + "VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = databaseConnection.getConnection();
             // RETURN_GENERATED_KEYS: recupera o id autoincrement gerado pelo SQLite
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preencher(statement, projeto);
            statement.executeUpdate();
            try (ResultSet chaves = statement.getGeneratedKeys()) {
                if (chaves.next()) {
                    int id = chaves.getInt(1);
                    projeto.setId(id);
                    return id;
                }
                throw new PersistenciaException("Falha ao obter o id gerado do projeto.");
            }
        } catch (SQLException excecao) {
            throw new PersistenciaException("Erro ao inserir projeto.", excecao);
        }
    }

    /**
     * Atualiza todos os campos do projeto identificado por {@code projeto.getId()}.
     *
     * @param projeto entidade com os novos valores e id existente
     * @throws PersistenciaException em falha de SQL
     */
    public void atualizar(Projeto projeto) {
        String sql = "UPDATE projetos SET nome = ?, descricao = ?, data_inicio = ?, "
            + "data_termino_prevista = ?, status = ? WHERE id = ?";
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            preencher(statement, projeto);
            statement.setInt(6, projeto.getId());
            statement.executeUpdate();
        } catch (SQLException excecao) {
            throw new PersistenciaException("Erro ao atualizar projeto.", excecao);
        }
    }

    /**
     * Remove o projeto pelo id. Verifique {@link #isReferenciado} antes de
     * chamar para evitar dependências órfãs em tarefas e equipes vinculadas.
     *
     * @param id identificador do projeto a remover
     * @throws PersistenciaException em falha de SQL
     */
    public void excluir(int id) {
        String sql = "DELETE FROM projetos WHERE id = ?";
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException excecao) {
            throw new PersistenciaException("Erro ao excluir projeto.", excecao);
        }
    }

    /**
     * Retorna todos os projetos cadastrados, ordenados por id.
     *
     * @return lista (possivelmente vazia) de projetos
     * @throws PersistenciaException em falha de SQL
     */
    public List<Projeto> listarTodos() {
        String sql = "SELECT " + COLUNAS + " FROM projetos ORDER BY id";
        List<Projeto> projetos = new ArrayList<>();
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                projetos.add(mapear(resultSet));
            }
        } catch (SQLException excecao) {
            throw new PersistenciaException("Erro ao listar projetos.", excecao);
        }
        return projetos;
    }

    /**
     * Busca um projeto pelo id.
     *
     * @param id identificador do projeto
     * @return {@link Optional} contendo a entidade, ou vazio se não encontrado
     * @throws PersistenciaException em falha de SQL
     */
    public Optional<Projeto> buscarPorId(int id) {
        String sql = "SELECT " + COLUNAS + " FROM projetos WHERE id = ?";
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
            throw new PersistenciaException("Erro ao buscar projeto por id.", excecao);
        }
    }

    /**
     * Lista projetos filtrando pelo status informado. O enum é persistido como
     * TEXT via {@code name()}.
     *
     * @param status status desejado (ex.: {@code ATIVO}, {@code CONCLUIDO})
     * @return lista (possivelmente vazia) de projetos com esse status
     * @throws PersistenciaException em falha de SQL
     */
    public List<Projeto> findByStatus(StatusProjeto status) {
        String sql = "SELECT " + COLUNAS + " FROM projetos WHERE status = ? ORDER BY id";
        List<Projeto> projetos = new ArrayList<>();
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    projetos.add(mapear(resultSet));
                }
            }
        } catch (SQLException excecao) {
            throw new PersistenciaException("Erro ao listar projetos por status.", excecao);
        }
        return projetos;
    }

    /**
     * Lista os projetos vinculados à equipe via {@code projeto_equipe}, ordenados
     * por nome. Substitui a resolução N+1 (um {@code buscarPorId} por vínculo) por
     * um único JOIN.
     *
     * @param equipeId id da equipe
     * @return lista (possivelmente vazia) de projetos vinculados à equipe
     * @throws PersistenciaException em falha de SQL
     */
    public List<Projeto> listarPorEquipe(int equipeId) {
        String sql = "SELECT p.id, p.nome, p.descricao, p.data_inicio, p.data_termino_prevista, p.status "
            + "FROM projetos p JOIN projeto_equipe pe ON pe.projeto_id = p.id "
            + "WHERE pe.equipe_id = ? ORDER BY p.nome";
        List<Projeto> projetos = new ArrayList<>();
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, equipeId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    projetos.add(mapear(resultSet));
                }
            }
        } catch (SQLException excecao) {
            throw new PersistenciaException("Erro ao listar projetos da equipe.", excecao);
        }
        return projetos;
    }

    /**
     * Lista os projetos cujos ids constam em {@code ids}, montando uma cláusula
     * {@code IN} com um placeholder por id. Resolve um conjunto de projetos em uma
     * única consulta, em vez de uma busca por id para cada elemento.
     *
     * @param ids coleção de ids de projeto a buscar
     * @return lista de projetos encontrados; vazia quando {@code ids} é vazio
     * @throws PersistenciaException em falha de SQL
     */
    public List<Projeto> listarPorIds(Collection<Integer> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        // Monta os placeholders (?, ?, ...) conforme a quantidade de ids informados
        String placeholders = ids.stream().map(id -> "?").collect(Collectors.joining(", "));
        String sql = "SELECT " + COLUNAS + " FROM projetos WHERE id IN (" + placeholders + ") ORDER BY id";
        List<Projeto> projetos = new ArrayList<>();
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int indice = 1;
            for (Integer id : ids) {
                statement.setInt(indice++, id);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    projetos.add(mapear(resultSet));
                }
            }
        } catch (SQLException excecao) {
            throw new PersistenciaException("Erro ao listar projetos por ids.", excecao);
        }
        return projetos;
    }

    /**
     * Executa consulta dinâmica com filtros opcionais: nome (LIKE case-insensitive),
     * status, intervalo de data de início e equipe associada via subquery em
     * {@code projeto_equipe}. Parâmetros nulos são ignorados na construção da query.
     *
     * @param filtro critérios de busca; {@code null} equivale a {@link #listarTodos()}
     * @return lista de projetos que satisfazem todos os filtros informados
     * @throws PersistenciaException em falha de SQL
     */
    public List<Projeto> consultar(ProjetoFiltro filtro) {
        if (filtro == null) {
            return listarTodos();
        }
        // SQL construído dinamicamente com cláusulas AND condicionais por filtro ativo
        StringBuilder sql = new StringBuilder("SELECT " + COLUNAS + " FROM projetos WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (filtro.getNome() != null && !filtro.getNome().isBlank()) {
            sql.append(" AND LOWER(nome) LIKE LOWER(?)");
            params.add("%" + filtro.getNome().trim() + "%");
        }
        if (filtro.getStatus() != null) {
            sql.append(" AND status = ?");
            params.add(filtro.getStatus().name());
        }
        if (filtro.getDataInicioDe() != null && filtro.getDataInicioAte() != null) {
            sql.append(" AND data_inicio BETWEEN ? AND ?");
            params.add(filtro.getDataInicioDe().toString());
            params.add(filtro.getDataInicioAte().toString());
        }
        if (filtro.getEquipeId() != null) {
            sql.append(" AND id IN (SELECT projeto_id FROM projeto_equipe WHERE equipe_id = ?)");
            params.add(filtro.getEquipeId());
        }
        sql.append(" ORDER BY id");

        List<Projeto> projetos = new ArrayList<>();
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    projetos.add(mapear(resultSet));
                }
            }
        } catch (SQLException excecao) {
            throw new PersistenciaException("Erro ao consultar projetos.", excecao);
        }
        return projetos;
    }

    /**
     * Verifica se o projeto possui tarefas ou equipes vinculadas, consultando
     * {@code tarefas} e a tabela de junção {@code projeto_equipe} em uma única
     * query com dois subselects somados.
     *
     * @param projetoId id do projeto a verificar
     * @return {@code true} se houver ao menos uma dependência
     * @throws PersistenciaException em falha de SQL
     */
    public boolean isReferenciado(int projetoId) {
        // Soma contagens de tarefas e vínculos de equipe para detectar dependências
        String sql = "SELECT "
            + "(SELECT COUNT(*) FROM tarefas WHERE projeto_id = ?) + "
            + "(SELECT COUNT(*) FROM projeto_equipe WHERE projeto_id = ?) AS total";
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, projetoId);
            statement.setInt(2, projetoId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt("total") > 0;
            }
        } catch (SQLException excecao) {
            throw new PersistenciaException("Erro ao verificar referências do projeto.", excecao);
        }
    }

    private void preencher(PreparedStatement statement, Projeto projeto) throws SQLException {
        statement.setString(1, projeto.getNome());
        statement.setString(2, projeto.getDescricao());
        statement.setString(3, projeto.getDataInicio() != null ? projeto.getDataInicio().toString() : null);
        statement.setString(4, projeto.getDataTerminoPrevista() != null
            ? projeto.getDataTerminoPrevista().toString() : null);
        statement.setString(5, projeto.getStatus() != null ? projeto.getStatus().name() : null);
    }

    private Projeto mapear(ResultSet resultSet) throws SQLException {
        return new Projeto(
            resultSet.getInt("id"),
            resultSet.getString("nome"),
            resultSet.getString("descricao"),
            LocalDate.parse(resultSet.getString("data_inicio")),
            LocalDate.parse(resultSet.getString("data_termino_prevista")),
            StatusProjeto.valueOf(resultSet.getString("status"))
        );
    }
}
