package br.com.dual.sgpe.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.dual.sgpe.dao.UsuarioDao;
import br.com.dual.sgpe.database.DatabaseConnection;
import br.com.dual.sgpe.database.DatabaseMigrator;
import br.com.dual.sgpe.exception.ExclusaoBloqueadaException;
import br.com.dual.sgpe.exception.RegistroDuplicadoException;
import br.com.dual.sgpe.exception.ValidacaoException;
import br.com.dual.sgpe.model.entity.Usuario;
import br.com.dual.sgpe.model.enums.PerfilUsuario;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UsuarioControllerTest {

    private static final String CPF_VALIDO = "52998224725";
    private static final String CPF_VALIDO_2 = "11144477735";

    @TempDir
    Path tempDir;

    private DatabaseConnection connection;
    private UsuarioController controller;

    @BeforeEach
    void setUp() {
        String url = "jdbc:sqlite:" + tempDir.resolve("test.db");
        connection = new DatabaseConnection(url);
        new DatabaseMigrator(connection).migrate();
        controller = new UsuarioController(new UsuarioDao(connection));
    }

    private Usuario novo(String login, String cpf) {
        return new Usuario("João Silva", cpf, login + "@exemplo.com", "Desenvolvedor",
            login, "senha123", PerfilUsuario.COLABORADOR);
    }

    @Test
    void salvarValidoPersisteComId() {
        Usuario salvo = controller.salvar(novo("joao", CPF_VALIDO));

        assertNotNull(salvo.getId());
        assertTrue(salvo.getId() > 0);
        assertEquals(1, controller.listarTodos().size());
    }

    @Test
    void salvarLoginDuplicadoLancaExcecao() {
        controller.salvar(novo("joao", CPF_VALIDO));

        RegistroDuplicadoException excecao = assertThrows(RegistroDuplicadoException.class,
            () -> controller.salvar(novo("joao", CPF_VALIDO_2)));
        assertTrue(excecao.getMessage().toLowerCase().contains("login"));
    }

    @Test
    void salvarCpfDuplicadoLancaExcecao() {
        controller.salvar(novo("joao", CPF_VALIDO));

        assertThrows(RegistroDuplicadoException.class,
            () -> controller.salvar(novo("maria", CPF_VALIDO)));
    }

    @Test
    void salvarEmailDuplicadoLancaExcecao() {
        controller.salvar(novo("joao", CPF_VALIDO));

        Usuario mesmoEmail = novo("maria", CPF_VALIDO_2);
        mesmoEmail.setEmail("joao@exemplo.com");
        RegistroDuplicadoException excecao = assertThrows(RegistroDuplicadoException.class,
            () -> controller.salvar(mesmoEmail));
        assertTrue(excecao.getMessage().toLowerCase().contains("mail"));
    }

    @Test
    void salvarCpfInvalidoLancaValidacao() {
        assertThrows(ValidacaoException.class,
            () -> controller.salvar(novo("joao", "12345678900")));
    }

    @Test
    void salvarCpfComMascaraAceitaENormaliza() {
        Usuario salvo = controller.salvar(novo("joao", "529.982.247-25"));

        assertEquals(CPF_VALIDO,
            controller.buscarPorId(salvo.getId()).orElseThrow().getCpf());
    }

    @Test
    void salvarSemNomeLancaValidacao() {
        Usuario usuario = novo("joao", CPF_VALIDO);
        usuario.setNomeCompleto("   ");

        assertThrows(ValidacaoException.class, () -> controller.salvar(usuario));
    }

    @Test
    void salvarSemPerfilLancaValidacao() {
        Usuario usuario = novo("joao", CPF_VALIDO);
        usuario.setPerfil(null);

        assertThrows(ValidacaoException.class, () -> controller.salvar(usuario));
    }

    @Test
    void salvarEmailInvalidoLancaValidacao() {
        Usuario usuario = novo("joao", CPF_VALIDO);
        usuario.setEmail("email-invalido");

        assertThrows(ValidacaoException.class, () -> controller.salvar(usuario));
    }

    @Test
    void atualizarAlteraMantemId() {
        Usuario salvo = controller.salvar(novo("joao", CPF_VALIDO));
        salvo.setNomeCompleto("João Editado");

        Usuario atualizado = controller.atualizar(salvo);

        assertEquals(salvo.getId(), atualizado.getId());
        assertEquals("João Editado",
            controller.buscarPorId(salvo.getId()).orElseThrow().getNomeCompleto());
    }

    @Test
    void excluirReferenciadoLancaExcecao() throws SQLException {
        Usuario salvo = controller.salvar(novo("joao", CPF_VALIDO));

        try (Connection conexao = connection.getConnection();
             Statement statement = conexao.createStatement()) {
            statement.executeUpdate(
                "INSERT INTO projetos (nome, descricao, data_inicio, data_termino_prevista, status) "
                    + "VALUES ('Projeto', 'Descricao', '2026-01-01', '2026-02-01', 'ABERTO')");
            statement.executeUpdate(
                "INSERT INTO tarefas (titulo, descricao, projeto_id, responsavel_id, data_inicio, data_termino_prevista, status) "
                    + "VALUES ('Tarefa', 'Descricao', 1, " + salvo.getId() + ", '2026-01-01', '2026-02-01', 'ABERTO')");
        }

        assertThrows(ExclusaoBloqueadaException.class, () -> controller.excluir(salvo.getId()));
    }

    @Test
    void excluirNaoReferenciadoRemove() {
        Usuario salvo = controller.salvar(novo("joao", CPF_VALIDO));

        controller.excluir(salvo.getId());

        assertTrue(controller.buscarPorId(salvo.getId()).isEmpty());
    }
}
