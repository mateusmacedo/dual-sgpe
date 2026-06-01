package br.com.dual.sgpe.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.dual.sgpe.dao.EquipeDao;
import br.com.dual.sgpe.dao.EquipeUsuarioDao;
import br.com.dual.sgpe.dao.UsuarioDao;
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

    @BeforeEach
    void setUp() {
        String url = "jdbc:sqlite:" + tempDir.resolve("test.db");
        connection = new DatabaseConnection(url);
        new DatabaseMigrator(connection).migrate();

        EquipeDao equipeDao = new EquipeDao(connection);
        EquipeUsuarioDao equipeUsuarioDao = new EquipeUsuarioDao(connection);
        UsuarioDao usuarioDao = new UsuarioDao(connection);
        controller = new EquipeController(equipeDao, equipeUsuarioDao, usuarioDao);

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
            () -> controller.adicionarMembro(equipe.getId(), 9999));
    }

    @Test
    void adicionarMembroDuplicadoLancaRegistroDuplicado() {
        Equipe equipe = controller.salvar(new Equipe("Equipe A", "Desc"));
        controller.adicionarMembro(equipe.getId(), usuario1Id);

        assertThrows(RegistroDuplicadoException.class,
            () -> controller.adicionarMembro(equipe.getId(), usuario1Id));
    }

    @Test
    void excluirLivreRemoveEquipeEVinculos() {
        Equipe equipe = controller.salvar(new Equipe("Equipe A", "Desc"));
        controller.adicionarMembro(equipe.getId(), usuario1Id);

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
        controller.adicionarMembro(equipe.getId(), usuario1Id);

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
        controller.adicionarMembro(equipe.getId(), usuario1Id);
        controller.adicionarMembro(equipe.getId(), usuario2Id);

        assertEquals(2, controller.listarMembros(equipe.getId()).size());
    }
}
