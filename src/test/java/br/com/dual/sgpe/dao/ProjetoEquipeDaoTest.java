package br.com.dual.sgpe.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.dual.sgpe.database.DatabaseConnection;
import br.com.dual.sgpe.database.DatabaseMigrator;
import br.com.dual.sgpe.model.entity.Equipe;
import br.com.dual.sgpe.model.entity.Projeto;
import br.com.dual.sgpe.model.enums.StatusProjeto;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjetoEquipeDaoTest {

    @TempDir
    Path tempDir;

    private ProjetoEquipeDao dao;
    private int projetoId;
    private int equipe1Id;
    private int equipe2Id;

    @BeforeEach
    void setUp() {
        String url = "jdbc:sqlite:" + tempDir.resolve("test.db");
        DatabaseConnection connection = new DatabaseConnection(url);
        new DatabaseMigrator(connection).migrate();

        ProjetoDao projetoDao = new ProjetoDao(connection);
        EquipeDao equipeDao = new EquipeDao(connection);
        dao = new ProjetoEquipeDao(connection);

        projetoId = projetoDao.inserir(new Projeto("Projeto Teste", "Descrição",
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), StatusProjeto.PLANEJADO));
        equipe1Id = equipeDao.inserir(new Equipe("Equipe 1", "Descrição"));
        equipe2Id = equipeDao.inserir(new Equipe("Equipe 2", "Descrição"));
    }

    @Test
    void vincularEExisteVinculo() {
        dao.vincular(projetoId, equipe1Id);

        assertTrue(dao.existeVinculo(projetoId, equipe1Id));
        assertFalse(dao.existeVinculo(projetoId, equipe2Id));
    }

    @Test
    void desvincularRemoveVinculo() {
        dao.vincular(projetoId, equipe1Id);

        dao.desvincular(projetoId, equipe1Id);

        assertFalse(dao.existeVinculo(projetoId, equipe1Id));
    }

    @Test
    void listarEquipeIdsRetornaVinculados() {
        dao.vincular(projetoId, equipe1Id);
        dao.vincular(projetoId, equipe2Id);

        List<Integer> ids = dao.listarEquipeIds(projetoId);
        assertEquals(2, ids.size());
        assertTrue(ids.contains(equipe1Id));
        assertTrue(ids.contains(equipe2Id));
    }

    @Test
    void listarProjetoIdsRetornaVinculados() {
        dao.vincular(projetoId, equipe1Id);

        List<Integer> ids = dao.listarProjetoIds(equipe1Id);
        assertEquals(1, ids.size());
        assertTrue(ids.contains(projetoId));
    }

    @Test
    void listarEquipeIdsSemVinculosRetornaVazio() {
        assertEquals(0, dao.listarEquipeIds(projetoId).size());
    }
}
