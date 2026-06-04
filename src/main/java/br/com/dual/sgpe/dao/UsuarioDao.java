package br.com.dual.sgpe.dao;

import br.com.dual.sgpe.database.DatabaseConnection;
import br.com.dual.sgpe.exception.PersistenciaException;
import br.com.dual.sgpe.model.entity.Usuario;
import br.com.dual.sgpe.model.enums.PerfilUsuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Acesso a dados da tabela {@code usuarios}. Isola o JDBC das demais camadas:
 * recebe e devolve entidades {@link Usuario}, nunca {@link ResultSet}.
 *
 * <p>Abre uma conexão por operação (try-with-resources) a partir da
 * {@link DatabaseConnection} injetada, o que permite apontar para um banco
 * temporário em testes.
 */
public class UsuarioDao {

    private static final String COLUNAS = "id, nome_completo, cpf, email, cargo, login, senha, perfil";

    private final DatabaseConnection databaseConnection;

    public UsuarioDao(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    /**
     * Persiste um novo usuário e atualiza o campo {@code id} da entidade.
     *
     * @param usuario entidade a inserir (id ignorado; será preenchido após inserção)
     * @return id gerado pelo banco de dados
     * @throws PersistenciaException em falha de SQL ou se o id não for retornado
     */
    public int inserir(Usuario usuario) {
        String sql = "INSERT INTO usuarios (nome_completo, cpf, email, cargo, login, senha, perfil) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = databaseConnection.getConnection();
             // RETURN_GENERATED_KEYS: recupera o id autoincrement gerado pelo SQLite
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preencher(statement, usuario);
            statement.executeUpdate();
            try (ResultSet chaves = statement.getGeneratedKeys()) {
                if (chaves.next()) {
                    int id = chaves.getInt(1);
                    usuario.setId(id);
                    return id;
                }
                throw new PersistenciaException("Falha ao obter o id gerado do usuário.");
            }
        } catch (SQLException exception) {
            throw new PersistenciaException("Erro ao inserir usuário.", exception);
        }
    }

    /**
     * Atualiza todos os campos do usuário identificado por {@code usuario.getId()}.
     *
     * @param usuario entidade com os novos valores e id existente
     * @throws PersistenciaException em falha de SQL
     */
    public void atualizar(Usuario usuario) {
        String sql = "UPDATE usuarios SET nome_completo = ?, cpf = ?, email = ?, cargo = ?, "
            + "login = ?, senha = ?, perfil = ? WHERE id = ?";
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            preencher(statement, usuario);
            statement.setInt(8, usuario.getId());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new PersistenciaException("Erro ao atualizar usuário.", exception);
        }
    }

    /**
     * Remove o usuário pelo id. Verifique {@link #isReferenciado} antes de
     * chamar para evitar dependências órfãs em tarefas e equipes.
     *
     * @param id identificador do usuário a remover
     * @throws PersistenciaException em falha de SQL
     */
    public void excluir(int id) {
        String sql = "DELETE FROM usuarios WHERE id = ?";
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new PersistenciaException("Erro ao excluir usuário.", exception);
        }
    }

    /**
     * Retorna todos os usuários cadastrados, ordenados por id.
     *
     * @return lista (possivelmente vazia) de usuários
     * @throws PersistenciaException em falha de SQL
     */
    public List<Usuario> listarTodos() {
        String sql = "SELECT " + COLUNAS + " FROM usuarios ORDER BY id";
        List<Usuario> usuarios = new ArrayList<>();
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                usuarios.add(mapear(resultSet));
            }
        } catch (SQLException exception) {
            throw new PersistenciaException("Erro ao listar usuários.", exception);
        }
        return usuarios;
    }

    /**
     * Busca um usuário pelo id.
     *
     * @param id identificador do usuário
     * @return {@link Optional} contendo a entidade, ou vazio se não encontrado
     * @throws PersistenciaException em falha de SQL
     */
    public Optional<Usuario> buscarPorId(int id) {
        String sql = "SELECT " + COLUNAS + " FROM usuarios WHERE id = ?";
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapear(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new PersistenciaException("Erro ao buscar usuário por id.", exception);
        }
    }

    /**
     * Busca um usuário pelo login. Usado principalmente no fluxo de autenticação.
     *
     * @param login login do usuário
     * @return {@link Optional} contendo a entidade, ou vazio se não encontrado
     * @throws PersistenciaException em falha de SQL
     */
    public Optional<Usuario> buscarPorLogin(String login) {
        String sql = "SELECT " + COLUNAS + " FROM usuarios WHERE login = ?";
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, login);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapear(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new PersistenciaException("Erro ao buscar usuário por login.", exception);
        }
    }

    /**
     * Lista os usuários membros da equipe via {@code equipe_usuario}, ordenados
     * por nome completo. Substitui a resolução N+1 (um {@code buscarPorId} por
     * vínculo) por um único JOIN.
     *
     * @param equipeId id da equipe
     * @return lista (possivelmente vazia) de usuários membros da equipe
     * @throws PersistenciaException em falha de SQL
     */
    public List<Usuario> listarPorEquipe(int equipeId) {
        String sql = "SELECT u.id, u.nome_completo, u.cpf, u.email, u.cargo, u.login, u.senha, u.perfil "
            + "FROM usuarios u JOIN equipe_usuario eu ON eu.usuario_id = u.id "
            + "WHERE eu.equipe_id = ? ORDER BY u.nome_completo";
        return listarPorParametro(sql, equipeId);
    }

    /**
     * Lista os responsáveis distintos por tarefas do projeto via {@code tarefas}.
     * O {@code DISTINCT} garante que cada usuário apareça uma única vez, mesmo
     * responsável por múltiplas tarefas. Substitui a resolução N+1 anterior por
     * um único JOIN.
     *
     * @param projetoId id do projeto
     * @return lista (possivelmente vazia) de responsáveis distintos do projeto
     * @throws PersistenciaException em falha de SQL
     */
    public List<Usuario> listarResponsaveisPorProjeto(int projetoId) {
        String sql = "SELECT DISTINCT u.id, u.nome_completo, u.cpf, u.email, u.cargo, u.login, u.senha, u.perfil "
            + "FROM usuarios u JOIN tarefas t ON t.responsavel_id = u.id "
            + "WHERE t.projeto_id = ?";
        return listarPorParametro(sql, projetoId);
    }

    private List<Usuario> listarPorParametro(String sql, int parametro) {
        List<Usuario> usuarios = new ArrayList<>();
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, parametro);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    usuarios.add(mapear(resultSet));
                }
            }
        } catch (SQLException exception) {
            throw new PersistenciaException("Erro ao listar usuários.", exception);
        }
        return usuarios;
    }

    /**
     * Verifica se já existe usuário com o login informado (para validação de unicidade
     * no cadastro).
     *
     * @param login login a verificar
     * @return {@code true} se o login já estiver em uso
     * @throws PersistenciaException em falha de SQL
     */
    public boolean existsByLogin(String login) {
        return contar("SELECT COUNT(*) FROM usuarios WHERE login = ?", login) > 0;
    }

    /**
     * Verifica se já existe usuário com o CPF informado (para validação de unicidade
     * no cadastro).
     *
     * @param cpf CPF a verificar
     * @return {@code true} se o CPF já estiver em uso
     * @throws PersistenciaException em falha de SQL
     */
    public boolean existsByCpf(String cpf) {
        return contar("SELECT COUNT(*) FROM usuarios WHERE cpf = ?", cpf) > 0;
    }

    /**
     * Verifica se já existe usuário com o e-mail informado (para validação de unicidade
     * no cadastro).
     *
     * @param email e-mail a verificar
     * @return {@code true} se o e-mail já estiver em uso
     * @throws PersistenciaException em falha de SQL
     */
    public boolean existsByEmail(String email) {
        return contar("SELECT COUNT(*) FROM usuarios WHERE email = ?", email) > 0;
    }

    /**
     * Verifica se o login já está em uso por outro usuário (excluindo o próprio
     * usuário em edição). Usado na validação de unicidade ao atualizar cadastro.
     *
     * @param login login a verificar
     * @param id    id do usuário que deve ser ignorado na contagem
     * @return {@code true} se outro usuário já possuir esse login
     * @throws PersistenciaException em falha de SQL
     */
    public boolean existsByLoginExcetoId(String login, int id) {
        return contar("SELECT COUNT(*) FROM usuarios WHERE login = ? AND id <> ?", login, id) > 0;
    }

    /**
     * Verifica se o CPF já está em uso por outro usuário (excluindo o próprio
     * usuário em edição). Usado na validação de unicidade ao atualizar cadastro.
     *
     * @param cpf CPF a verificar
     * @param id  id do usuário que deve ser ignorado na contagem
     * @return {@code true} se outro usuário já possuir esse CPF
     * @throws PersistenciaException em falha de SQL
     */
    public boolean existsByCpfExcetoId(String cpf, int id) {
        return contar("SELECT COUNT(*) FROM usuarios WHERE cpf = ? AND id <> ?", cpf, id) > 0;
    }

    /**
     * Verifica se o e-mail já está em uso por outro usuário (excluindo o próprio
     * usuário em edição). Usado na validação de unicidade ao atualizar cadastro.
     *
     * @param email e-mail a verificar
     * @param id    id do usuário que deve ser ignorado na contagem
     * @return {@code true} se outro usuário já possuir esse e-mail
     * @throws PersistenciaException em falha de SQL
     */
    public boolean existsByEmailExcetoId(String email, int id) {
        return contar("SELECT COUNT(*) FROM usuarios WHERE email = ? AND id <> ?", email, id) > 0;
    }

    /**
     * Verifica se o usuário possui referências ativas em tarefas ou equipes,
     * consultando {@code tarefas} e {@code equipe_usuario} em uma única query
     * com dois subselects somados.
     *
     * @param usuarioId id do usuário a verificar
     * @return {@code true} se houver ao menos uma dependência
     * @throws PersistenciaException em falha de SQL
     */
    public boolean isReferenciado(int usuarioId) {
        // Soma contagens de responsabilidades em tarefas e vínculos em equipes
        String sql = "SELECT "
            + "(SELECT COUNT(*) FROM tarefas WHERE responsavel_id = ?) + "
            + "(SELECT COUNT(*) FROM equipe_usuario WHERE usuario_id = ?) AS total";
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, usuarioId);
            statement.setInt(2, usuarioId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt("total") > 0;
            }
        } catch (SQLException exception) {
            throw new PersistenciaException("Erro ao verificar referências do usuário.", exception);
        }
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
    private long contar(String sql, Object... params) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0L;
            }
        } catch (SQLException exception) {
            throw new PersistenciaException("Erro ao verificar existência de usuário.", exception);
        }
    }

    private void preencher(PreparedStatement statement, Usuario usuario) throws SQLException {
        statement.setString(1, usuario.getNomeCompleto());
        statement.setString(2, usuario.getCpf());
        statement.setString(3, usuario.getEmail());
        statement.setString(4, usuario.getCargo());
        statement.setString(5, usuario.getLogin());
        statement.setString(6, usuario.getSenha());
        statement.setString(7, usuario.getPerfil() != null ? usuario.getPerfil().name() : null);
    }

    private Usuario mapear(ResultSet resultSet) throws SQLException {
        return new Usuario(
            resultSet.getInt("id"),
            resultSet.getString("nome_completo"),
            resultSet.getString("cpf"),
            resultSet.getString("email"),
            resultSet.getString("cargo"),
            resultSet.getString("login"),
            resultSet.getString("senha"),
            PerfilUsuario.valueOf(resultSet.getString("perfil"))
        );
    }
}
