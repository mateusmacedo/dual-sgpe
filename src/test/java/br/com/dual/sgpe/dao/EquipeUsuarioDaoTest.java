package br.com.dual.sgpe.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.dual.sgpe.database.DatabaseConnection;
import br.com.dual.sgpe.database.DatabaseMigrator;
import br.com.dual.sgpe.model.entity.Equipe;
import br.com.dual.sgpe.model.entity.Usuario;
import br.com.dual.sgpe.model.enums.PerfilUsuario;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EquipeUsuarioDaoTest {

    @TempDir
    Path tempDir;

    private EquipeUsuarioDao dao;
    private int equipeId;
    private int usuario1Id;
    private int usuario2Id;

    @BeforeEach
    void setUp() {
        String url = "jdbc:sqlite:" + tempDir.resolve("test.db");
        DatabaseConnection connection = new DatabaseConnection(url);
        new DatabaseMigrator(connection).migrate();

        EquipeDao equipeDao = new EquipeDao(connection);
        UsuarioDao usuarioDao = new UsuarioDao(connection);
        dao = new EquipeUsuarioDao(connection);

        equipeId = equipeDao.inserir(new Equipe("Equipe Teste", "Descrição"));
        usuario1Id = usuarioDao.inserir(new Usuario("Usuário 1", "11111111111",
            "u1@test.com", "Dev", "user1", "senha1", PerfilUsuario.GERENTE));
        usuario2Id = usuarioDao.inserir(new Usuario("Usuário 2", "22222222222",
            "u2@test.com", "Dev", "user2", "senha2", PerfilUsuario.GERENTE));
    }

    @Test
    void vincularEExisteVinculo() {
        dao.vincular(equipeId, usuario1Id);

        assertTrue(dao.existeVinculo(equipeId, usuario1Id));
        assertFalse(dao.existeVinculo(equipeId, usuario2Id));
    }

    @Test
    void desvincularRemoveVinculo() {
        dao.vincular(equipeId, usuario1Id);

        dao.desvincular(equipeId, usuario1Id);

        assertFalse(dao.existeVinculo(equipeId, usuario1Id));
    }

    @Test
    void listarUsuarioIdsRetornaVinculados() {
        dao.vincular(equipeId, usuario1Id);
        dao.vincular(equipeId, usuario2Id);

        List<Integer> ids = dao.listarUsuarioIds(equipeId);
        assertEquals(2, ids.size());
        assertTrue(ids.contains(usuario1Id));
        assertTrue(ids.contains(usuario2Id));
    }

    @Test
    void removerTodosDaEquipeLimpaVinculos() {
        dao.vincular(equipeId, usuario1Id);
        dao.vincular(equipeId, usuario2Id);

        dao.removerTodosDaEquipe(equipeId);

        assertEquals(0, dao.listarUsuarioIds(equipeId).size());
    }

    @Test
    void listarUsuarioIdsSemVinculosRetornaVazio() {
        assertEquals(0, dao.listarUsuarioIds(equipeId).size());
    }

    @Test
    void listarEquipeIdsDoUsuarioRetornaEquipesDoUsuario() {
        dao.vincular(equipeId, usuario1Id);

        List<Integer> ids = dao.listarEquipeIdsDoUsuario(usuario1Id);

        assertEquals(1, ids.size());
        assertTrue(ids.contains(equipeId));
    }

    @Test
    void listarEquipeIdsDoUsuarioIsolaPorUsuario() {
        dao.vincular(equipeId, usuario1Id);

        assertEquals(0, dao.listarEquipeIdsDoUsuario(usuario2Id).size());
    }
}
