package br.com.dual.sgpe.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.dual.sgpe.database.DatabaseConnection;
import br.com.dual.sgpe.database.DatabaseMigrator;
import br.com.dual.sgpe.model.entity.Projeto;
import br.com.dual.sgpe.model.entity.Tarefa;
import br.com.dual.sgpe.model.entity.Usuario;
import br.com.dual.sgpe.model.enums.PerfilUsuario;
import br.com.dual.sgpe.model.enums.StatusProjeto;
import br.com.dual.sgpe.model.enums.StatusTarefa;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RelatorioDaoTest {

    @TempDir
    Path tempDir;

    private RelatorioDao dao;
    private int projetoId;

    @BeforeEach
    void setUp() {
        String url = "jdbc:sqlite:" + tempDir.resolve("test.db");
        DatabaseConnection connection = new DatabaseConnection(url);
        new DatabaseMigrator(connection).migrate();
        dao = new RelatorioDao(connection);

        ProjetoDao projetoDao = new ProjetoDao(connection);
        UsuarioDao usuarioDao = new UsuarioDao(connection);
        TarefaDao tarefaDao = new TarefaDao(connection);

        projetoId = projetoDao.inserir(new Projeto("Projeto Teste", "Desc",
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), StatusProjeto.EM_ANDAMENTO));

        int u1 = usuarioDao.inserir(new Usuario("User 1", "11111111111",
            "u1@t.com", "Dev", "user1", "s1", PerfilUsuario.GERENTE));
        int u2 = usuarioDao.inserir(new Usuario("User 2", "22222222222",
            "u2@t.com", "Dev", "user2", "s2", PerfilUsuario.GERENTE));

        tarefaDao.inserir(new Tarefa("T1", "Desc", projetoId, u1,
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31), StatusTarefa.CONCLUIDA));
        tarefaDao.inserir(new Tarefa("T2", "Desc", projetoId, u1,
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31), StatusTarefa.CONCLUIDA));
        tarefaDao.inserir(new Tarefa("T3", "Desc", projetoId, u2,
            LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30), StatusTarefa.PENDENTE));
        tarefaDao.inserir(new Tarefa("T4", "Desc", projetoId, u1,
            LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30), StatusTarefa.CANCELADA));
    }

    @Test
    void contarTarefasRetornaTotal() {
        assertEquals(4, dao.contarTarefasPorProjeto(projetoId));
    }

    @Test
    void contarConcluidasRetornaApenasConcluidas() {
        assertEquals(2, dao.contarConcluidasPorProjeto(projetoId));
    }

    @Test
    void contarPendentesExcluiCancelada() {
        assertEquals(1, dao.contarPendentesPorProjeto(projetoId));
    }

    @Test
    void listarResponsavelIdsRetornaDistinct() {
        List<Integer> ids = dao.listarResponsavelIds(projetoId);
        assertEquals(2, ids.size());
    }

    @Test
    void projetoSemTarefasRetornaZeros() {
        assertEquals(0, dao.contarTarefasPorProjeto(9999));
        assertEquals(0, dao.contarConcluidasPorProjeto(9999));
        assertEquals(0, dao.contarPendentesPorProjeto(9999));
        assertTrue(dao.listarResponsavelIds(9999).isEmpty());
    }
}
