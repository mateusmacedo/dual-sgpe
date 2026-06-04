package br.com.dual.sgpe.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.dual.sgpe.dao.EquipeDao;
import br.com.dual.sgpe.dao.EquipeUsuarioDao;
import br.com.dual.sgpe.dao.ProjetoDao;
import br.com.dual.sgpe.dao.ProjetoEquipeDao;
import br.com.dual.sgpe.dao.UsuarioDao;
import br.com.dual.sgpe.security.EscopoColaborador;
import br.com.dual.sgpe.database.DatabaseConnection;
import br.com.dual.sgpe.database.DatabaseMigrator;
import br.com.dual.sgpe.exception.ExclusaoBloqueadaException;
import br.com.dual.sgpe.exception.RegistroDuplicadoException;
import br.com.dual.sgpe.exception.ValidacaoException;
import br.com.dual.sgpe.model.entity.Equipe;
import br.com.dual.sgpe.model.entity.Usuario;
import br.com.dual.sgpe.model.enums.PerfilUsuario;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EquipeControllerTest {

    @TempDir
    Path tempDir;

    private DatabaseConnection connection;
    private EquipeController controller;
    private int usuario1Id;
    private int usuario2Id;
    private UsuarioDao usuarioDao;

    /** Perfil padrão do solicitante nos testes; gerencia os usuários GERENTE do setUp. */
    private static final PerfilUsuario SOLICITANTE = PerfilUsuario.GERENTE;

    @BeforeEach
    void setUp() {
        String url = "jdbc:sqlite:" + tempDir.resolve("test.db");
        connection = new DatabaseConnection(url);
        new DatabaseMigrator(connection).migrate();

        EquipeDao equipeDao = new EquipeDao(connection);
        EquipeUsuarioDao equipeUsuarioDao = new EquipeUsuarioDao(connection);
        ProjetoEquipeDao projetoEquipeDao = new ProjetoEquipeDao(connection);
        ProjetoDao projetoDao = new ProjetoDao(connection);
        usuarioDao = new UsuarioDao(connection);
        EscopoColaborador escopo = new EscopoColaborador(equipeUsuarioDao, projetoEquipeDao, projetoDao);
        controller = new EquipeController(equipeDao, equipeUsuarioDao, usuarioDao, projetoEquipeDao, escopo);

        usuario1Id = usuarioDao.inserir(new Usuario("Usuário 1", "11111111111",
            "u1@test.com", "Dev", "user1", "senha1", PerfilUsuario.GERENTE));
        usuario2Id = usuarioDao.inserir(new Usuario("Usuário 2", "22222222222",
            "u2@test.com", "Dev", "user2", "senha2", PerfilUsuario.GERENTE));
    }

    @Test
    void salvarSemNomeLancaValidacao() {
        assertThrows(ValidacaoException.class,
            () -> controller.salvar(new Equipe("   ", "Desc")));
    }

    @Test
    void salvarOk() {
        Equipe salva = controller.salvar(new Equipe("Equipe A", "Descrição"));

        assertNotNull(salva.getId());
        assertEquals("Equipe A", salva.getNome());
    }

    @Test
    void adicionarMembroInexistenteLancaValidacao() {
        Equipe equipe = controller.salvar(new Equipe("Equipe A", "Desc"));

        assertThrows(ValidacaoException.class,
            () -> controller.adicionarMembro(equipe.getId(), 9999, SOLICITANTE));
    }

    @Test
    void adicionarMembroDuplicadoLancaRegistroDuplicado() {
        Equipe equipe = controller.salvar(new Equipe("Equipe A", "Desc"));
        controller.adicionarMembro(equipe.getId(), usuario1Id, SOLICITANTE);

        assertThrows(RegistroDuplicadoException.class,
            () -> controller.adicionarMembro(equipe.getId(), usuario1Id, SOLICITANTE));
    }

    @Test
    void excluirLivreRemoveEquipeEVinculos() {
        Equipe equipe = controller.salvar(new Equipe("Equipe A", "Desc"));
        controller.adicionarMembro(equipe.getId(), usuario1Id, SOLICITANTE);

        controller.excluir(equipe.getId());

        assertTrue(controller.buscarPorId(equipe.getId()).isEmpty());
    }

    @Test
    void excluirBloqueadaLancaExclusaoBloqueada() throws SQLException {
        Equipe equipe = controller.salvar(new Equipe("Equipe A", "Desc"));
        try (Connection conexao = connection.getConnection();
             Statement statement = conexao.createStatement()) {
            statement.executeUpdate(
                "INSERT INTO projetos (nome, descricao, data_inicio, data_termino_prevista, status) "
                    + "VALUES ('Projeto', 'Desc', '2026-01-01', '2026-12-31', 'PLANEJADO')");
            statement.executeUpdate(
                "INSERT INTO projeto_equipe (projeto_id, equipe_id) VALUES (1, " + equipe.getId() + ")");
        }

        assertThrows(ExclusaoBloqueadaException.class,
            () -> controller.excluir(equipe.getId()));
    }

    @Test
    void removerMembroDesvincula() {
        Equipe equipe = controller.salvar(new Equipe("Equipe A", "Desc"));
        controller.adicionarMembro(equipe.getId(), usuario1Id, SOLICITANTE);

        controller.removerMembro(equipe.getId(), usuario1Id);

        assertEquals(0, controller.listarMembros(equipe.getId()).size());
    }

    @Test
    void atualizarSemNomeLancaValidacao() {
        Equipe equipe = controller.salvar(new Equipe("Equipe A", "Desc"));
        equipe.setNome("   ");

        assertThrows(ValidacaoException.class, () -> controller.atualizar(equipe));
    }

    @Test
    void atualizarSemIdLancaValidacao() {
        assertThrows(ValidacaoException.class,
            () -> controller.atualizar(new Equipe("Equipe A", "Desc")));
    }

    @Test
    void listarMembrosRetornaCorreto() {
        Equipe equipe = controller.salvar(new Equipe("Equipe A", "Desc"));
        controller.adicionarMembro(equipe.getId(), usuario1Id, SOLICITANTE);
        controller.adicionarMembro(equipe.getId(), usuario2Id, SOLICITANTE);

        assertEquals(2, controller.listarMembros(equipe.getId()).size());
    }

    @Test
    void adicionarMembroSuperiorLancaValidacao() {
        Equipe equipe = controller.salvar(new Equipe("Equipe A", "Desc"));
        int adminId = usuarioDao.inserir(new Usuario("Admin", "99999999999",
            "admin@test.com", "Diretor", "admin", "senha", PerfilUsuario.ADMINISTRADOR));

        // GERENTE não pode adicionar à equipe um usuário ADMINISTRADOR (nível superior)
        assertThrows(ValidacaoException.class,
            () -> controller.adicionarMembro(equipe.getId(), adminId, PerfilUsuario.GERENTE));
    }

    @Test
    void adicionarMembroAdminComoAdminLancaValidacao() {
        Equipe equipe = controller.salvar(new Equipe("Equipe A", "Desc"));
        int adminId = usuarioDao.inserir(new Usuario("Admin", "99999999999",
            "admin@test.com", "Diretor", "admin", "senha", PerfilUsuario.ADMINISTRADOR));

        // Nem mesmo ADMINISTRADOR pode adicionar outro admin como membro de equipe
        assertThrows(ValidacaoException.class,
            () -> controller.adicionarMembro(equipe.getId(), adminId, PerfilUsuario.ADMINISTRADOR));
    }

    @Test
    void listarUsuariosExcluiAdministrador() {
        usuarioDao.inserir(new Usuario("Admin", "99999999999",
            "admin@test.com", "Diretor", "admin", "senha", PerfilUsuario.ADMINISTRADOR));

        var usuarios = controller.listarUsuarios(PerfilUsuario.ADMINISTRADOR);

        assertTrue(usuarios.stream()
            .noneMatch(u -> u.getPerfil() == PerfilUsuario.ADMINISTRADOR));
    }
}
