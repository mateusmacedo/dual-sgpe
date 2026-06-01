package br.com.dual.sgpe.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.dual.sgpe.dao.ProjetoDao;
import br.com.dual.sgpe.dao.TarefaDao;
import br.com.dual.sgpe.dao.UsuarioDao;
import br.com.dual.sgpe.database.DatabaseConnection;
import br.com.dual.sgpe.database.DatabaseMigrator;
import br.com.dual.sgpe.exception.ValidacaoException;
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

class TarefaControllerTest {

    @TempDir
    Path tempDir;

    private TarefaController controller;
    private int projetoId;
    private int usuarioId;

    @BeforeEach
    void setUp() {
        String url = "jdbc:sqlite:" + tempDir.resolve("test.db");
        DatabaseConnection connection = new DatabaseConnection(url);
        new DatabaseMigrator(connection).migrate();

        ProjetoDao projetoDao = new ProjetoDao(connection);
        UsuarioDao usuarioDao = new UsuarioDao(connection);
        TarefaDao tarefaDao = new TarefaDao(connection);
        controller = new TarefaController(tarefaDao, projetoDao, usuarioDao);

        projetoId = projetoDao.inserir(new Projeto("Projeto Teste", "Descrição",
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), StatusProjeto.PLANEJADO));
        usuarioId = usuarioDao.inserir(new Usuario("Usuário Teste", "12345678901",
            "teste@email.com", "Desenvolvedor", "user1", "senha123", PerfilUsuario.GERENTE));
    }

    private Tarefa nova(LocalDate inicio, LocalDate termino, StatusTarefa status) {
        return new Tarefa("Tarefa Teste", "Descrição", projetoId, usuarioId, inicio, termino, status);
    }

    @Test
    void salvarSemStatusUsaPendente() {
        Tarefa salva = controller.salvar(
            nova(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), null));

        assertNotNull(salva.getId());
        assertEquals(StatusTarefa.PENDENTE, salva.getStatus());
    }

    @Test
    void salvarComProjetoInexistenteLancaValidacao() {
        Tarefa tarefa = new Tarefa("Tarefa", "Desc", 9999, usuarioId,
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), StatusTarefa.PENDENTE);

        assertThrows(ValidacaoException.class, () -> controller.salvar(tarefa));
    }

    @Test
    void salvarComResponsavelInexistenteLancaValidacao() {
        Tarefa tarefa = new Tarefa("Tarefa", "Desc", projetoId, 9999,
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), StatusTarefa.PENDENTE);

        assertThrows(ValidacaoException.class, () -> controller.salvar(tarefa));
    }

    @Test
    void salvarTerminoAntesDoInicioLancaValidacao() {
        assertThrows(ValidacaoException.class, () -> controller.salvar(
            nova(LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 5), StatusTarefa.PENDENTE)));
    }

    @Test
    void salvarTerminoIgualAoInicioAceita() {
        Tarefa salva = controller.salvar(
            nova(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1), StatusTarefa.PENDENTE));

        assertNotNull(salva.getId());
    }

    @Test
    void atualizarStatusPersiste() {
        Tarefa salva = controller.salvar(
            nova(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), StatusTarefa.PENDENTE));

        controller.atualizarStatus(salva.getId(), StatusTarefa.CONCLUIDA);

        assertEquals(StatusTarefa.CONCLUIDA,
            controller.buscarPorId(salva.getId()).orElseThrow().getStatus());
    }

    @Test
    void findByProjetoIdFiltra() {
        controller.salvar(nova(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), StatusTarefa.PENDENTE));
        controller.salvar(nova(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), StatusTarefa.EM_ANDAMENTO));

        assertEquals(2, controller.findByProjetoId(projetoId).size());
        assertEquals(0, controller.findByProjetoId(9999).size());
    }

    @Test
    void atualizarSemIdLancaValidacao() {
        Tarefa tarefa = nova(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), StatusTarefa.PENDENTE);

        assertThrows(ValidacaoException.class, () -> controller.atualizar(tarefa));
    }

    @Test
    void excluirRemoveTarefa() {
        Tarefa salva = controller.salvar(
            nova(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), StatusTarefa.PENDENTE));

        controller.excluir(salva.getId());

        assertTrue(controller.buscarPorId(salva.getId()).isEmpty());
    }

    @Test
    void salvarSemTituloLancaValidacao() {
        Tarefa tarefa = new Tarefa("   ", "Desc", projetoId, usuarioId,
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), StatusTarefa.PENDENTE);

        assertThrows(ValidacaoException.class, () -> controller.salvar(tarefa));
    }
}
