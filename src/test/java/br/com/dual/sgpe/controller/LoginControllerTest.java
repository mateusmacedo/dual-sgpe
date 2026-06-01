package br.com.dual.sgpe.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.dual.sgpe.config.AppConfig;
import br.com.dual.sgpe.dao.UsuarioDao;
import br.com.dual.sgpe.database.DatabaseConnection;
import br.com.dual.sgpe.database.DatabaseMigrator;
import br.com.dual.sgpe.exception.AutenticacaoException;
import br.com.dual.sgpe.model.entity.Usuario;
import br.com.dual.sgpe.model.enums.PerfilUsuario;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LoginControllerTest {

    private static final String CPF_VALIDO = "52998224725";

    @TempDir
    Path tempDir;

    private UsuarioDao usuarioDao;
    private LoginController controller;

    @BeforeEach
    void setUp() {
        String url = "jdbc:sqlite:" + tempDir.resolve("test.db");
        DatabaseConnection connection = new DatabaseConnection(url);
        new DatabaseMigrator(connection).migrate();
        usuarioDao = new UsuarioDao(connection);
        controller = new LoginController(usuarioDao);
        AppConfig.getInstance().limpar();
        usuarioDao.inserir(new Usuario("Administrador", CPF_VALIDO, "admin@exemplo.com",
            "Administrador", "admin", "senha123", PerfilUsuario.ADMINISTRADOR));
    }

    @Test
    void autenticarCredenciaisValidasRetornaUsuarioESetaSessao() {
        Usuario autenticado = controller.autenticar("admin", "senha123");

        assertEquals("admin", autenticado.getLogin());
        assertEquals(PerfilUsuario.ADMINISTRADOR, autenticado.getPerfil());
        assertSame(autenticado, AppConfig.getInstance().getUsuarioAutenticado());
    }

    @Test
    void autenticarLoginInexistenteLancaAutenticacao() {
        AutenticacaoException excecao = assertThrows(AutenticacaoException.class,
            () -> controller.autenticar("fantasma", "qualquer"));

        assertTrue(excecao.getMessage().toLowerCase().contains("credenciais"));
        assertNull(AppConfig.getInstance().getUsuarioAutenticado());
    }

    @Test
    void autenticarSenhaIncorretaLancaAutenticacao() {
        assertThrows(AutenticacaoException.class,
            () -> controller.autenticar("admin", "senha-errada"));

        assertNull(AppConfig.getInstance().getUsuarioAutenticado());
    }

    @Test
    void autenticarSenhaCaseSensitiveLancaAutenticacao() {
        assertThrows(AutenticacaoException.class,
            () -> controller.autenticar("admin", "SENHA123"));
    }
}
