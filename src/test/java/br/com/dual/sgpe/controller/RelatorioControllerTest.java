package br.com.dual.sgpe.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.dual.sgpe.dao.EquipeDao;
import br.com.dual.sgpe.dao.ProjetoDao;
import br.com.dual.sgpe.dao.ProjetoEquipeDao;
import br.com.dual.sgpe.dao.RelatorioDao;
import br.com.dual.sgpe.dao.TarefaDao;
import br.com.dual.sgpe.dao.UsuarioDao;
import br.com.dual.sgpe.database.DatabaseConnection;
import br.com.dual.sgpe.database.DatabaseMigrator;
import br.com.dual.sgpe.exception.ValidacaoException;
import br.com.dual.sgpe.model.dto.RelatorioProjeto;
import br.com.dual.sgpe.model.entity.Equipe;
import br.com.dual.sgpe.model.entity.Projeto;
import br.com.dual.sgpe.model.entity.Tarefa;
import br.com.dual.sgpe.model.entity.Usuario;
import br.com.dual.sgpe.model.enums.PerfilUsuario;
import br.com.dual.sgpe.model.enums.StatusProjeto;
import br.com.dual.sgpe.model.enums.StatusTarefa;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RelatorioControllerTest {

    @TempDir
    Path tempDir;

    private RelatorioController controller;
    private ProjetoDao projetoDao;
    private TarefaDao tarefaDao;
    private EquipeDao equipeDao;
    private ProjetoEquipeDao projetoEquipeDao;
    private int usuarioId;

    @BeforeEach
    void setUp() {
        String url = "jdbc:sqlite:" + tempDir.resolve("test.db");
        DatabaseConnection connection = new DatabaseConnection(url);
        new DatabaseMigrator(connection).migrate();

        projetoDao = new ProjetoDao(connection);
        RelatorioDao relatorioDao = new RelatorioDao(connection);
        projetoEquipeDao = new ProjetoEquipeDao(connection);
        equipeDao = new EquipeDao(connection);
        UsuarioDao usuarioDao = new UsuarioDao(connection);
        tarefaDao = new TarefaDao(connection);
        controller = new RelatorioController(projetoDao, relatorioDao,
            projetoEquipeDao, equipeDao, usuarioDao);

        usuarioId = usuarioDao.inserir(new Usuario("Usuário Teste", "11111111111",
            "u@t.com", "Dev", "user1", "s1", PerfilUsuario.GERENTE));
    }

    private int criarProjeto(String nome) {
        return projetoDao.inserir(new Projeto(nome, "Desc",
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), StatusProjeto.EM_ANDAMENTO));
    }

    private void criarTarefa(int projetoId, StatusTarefa status) {
        tarefaDao.inserir(new Tarefa("Tarefa", "Desc", projetoId, usuarioId,
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30), status));
    }

    @Test
    void percentual50ComDuasDe4Concluidas() {
        int pid = criarProjeto("P1");
        criarTarefa(pid, StatusTarefa.CONCLUIDA);
        criarTarefa(pid, StatusTarefa.CONCLUIDA);
        criarTarefa(pid, StatusTarefa.PENDENTE);
        criarTarefa(pid, StatusTarefa.EM_ANDAMENTO);

        RelatorioProjeto r = controller.gerar(pid);

        assertEquals(4, r.getTotalTarefas());
        assertEquals(2, r.getTarefasConcluidas());
        assertEquals(2, r.getTarefasPendentes());
        assertEquals(50.0, r.getPercentualConclusao(), 0.01);
    }

    @Test
    void percentual100TodasConcluidas() {
        int pid = criarProjeto("P1");
        criarTarefa(pid, StatusTarefa.CONCLUIDA);
        criarTarefa(pid, StatusTarefa.CONCLUIDA);
        criarTarefa(pid, StatusTarefa.CONCLUIDA);

        RelatorioProjeto r = controller.gerar(pid);

        assertEquals(100.0, r.getPercentualConclusao(), 0.01);
        assertEquals(3, r.getTarefasConcluidas());
    }

    @Test
    void percentual0SemTarefas() {
        int pid = criarProjeto("P1");

        RelatorioProjeto r = controller.gerar(pid);

        assertEquals(0.0, r.getPercentualConclusao(), 0.01);
        assertEquals(0, r.getTotalTarefas());
        assertTrue(r.isSemTarefas());
    }

    @Test
    void responsaveisDistinct() {
        int pid = criarProjeto("P1");
        criarTarefa(pid, StatusTarefa.PENDENTE);
        criarTarefa(pid, StatusTarefa.PENDENTE);
        criarTarefa(pid, StatusTarefa.PENDENTE);

        RelatorioProjeto r = controller.gerar(pid);

        assertEquals(1, r.getResponsaveis().size());
    }

    @Test
    void equipesVinculadas() {
        int pid = criarProjeto("P1");
        int eid = equipeDao.inserir(new Equipe("Equipe A", "Desc"));
        projetoEquipeDao.vincular(pid, eid);

        RelatorioProjeto r = controller.gerar(pid);

        assertEquals(1, r.getEquipes().size());
        assertEquals("Equipe A", r.getEquipes().get(0).getNome());
    }

    @Test
    void projetoInexistenteLancaValidacao() {
        assertThrows(ValidacaoException.class, () -> controller.gerar(9999));
    }

    @Test
    void pendentesExcluiCancelada() {
        int pid = criarProjeto("P1");
        criarTarefa(pid, StatusTarefa.PENDENTE);
        criarTarefa(pid, StatusTarefa.CANCELADA);

        RelatorioProjeto r = controller.gerar(pid);

        assertEquals(2, r.getTotalTarefas());
        assertEquals(1, r.getTarefasPendentes());
    }
}
