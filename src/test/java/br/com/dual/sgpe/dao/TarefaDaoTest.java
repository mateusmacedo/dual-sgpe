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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TarefaDaoTest {

    @TempDir
    Path tempDir;

    private TarefaDao dao;
    private int projetoId;
    private int usuarioId;

    @BeforeEach
    void setUp() {
        String url = "jdbc:sqlite:" + tempDir.resolve("test.db");
        DatabaseConnection connection = new DatabaseConnection(url);
        new DatabaseMigrator(connection).migrate();

        ProjetoDao projetoDao = new ProjetoDao(connection);
        UsuarioDao usuarioDao = new UsuarioDao(connection);
        dao = new TarefaDao(connection);

        projetoId = projetoDao.inserir(new Projeto("Projeto Teste", "Descrição",
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), StatusProjeto.PLANEJADO));
        usuarioId = usuarioDao.inserir(new Usuario("Usuário Teste", "12345678901",
            "teste@email.com", "Desenvolvedor", "user1", "senha123", PerfilUsuario.GERENTE));
    }

    private Tarefa nova(String titulo, StatusTarefa status) {
        return new Tarefa(titulo, "Descrição", projetoId, usuarioId,
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), status);
    }

    @Test
    void inserirEBuscarPorIdPreservaCampos() {
        int id = dao.inserir(nova("Tarefa A", StatusTarefa.PENDENTE));

        Tarefa encontrada = dao.buscarPorId(id).orElseThrow();
        assertEquals("Tarefa A", encontrada.getTitulo());
        assertEquals(projetoId, encontrada.getProjetoId());
        assertEquals(usuarioId, encontrada.getResponsavelId());
        assertEquals(LocalDate.of(2026, 6, 1), encontrada.getDataInicio());
        assertEquals(LocalDate.of(2026, 6, 30), encontrada.getDataTerminoPrevista());
        assertEquals(StatusTarefa.PENDENTE, encontrada.getStatus());
    }

    @Test
    void findByProjetoIdFiltra() {
        dao.inserir(nova("Tarefa A", StatusTarefa.PENDENTE));
        dao.inserir(nova("Tarefa B", StatusTarefa.EM_ANDAMENTO));

        assertEquals(2, dao.findByProjetoId(projetoId).size());
        assertEquals(0, dao.findByProjetoId(9999).size());
    }

    @Test
    void findByResponsavelIdFiltra() {
        dao.inserir(nova("Tarefa A", StatusTarefa.PENDENTE));

        assertEquals(1, dao.findByResponsavelId(usuarioId).size());
        assertEquals(0, dao.findByResponsavelId(9999).size());
    }

    @Test
    void atualizarPersisteMudancas() {
        int id = dao.inserir(nova("Tarefa A", StatusTarefa.PENDENTE));
        Tarefa tarefa = dao.buscarPorId(id).orElseThrow();
        tarefa.setStatus(StatusTarefa.CONCLUIDA);

        dao.atualizar(tarefa);

        assertEquals(StatusTarefa.CONCLUIDA, dao.buscarPorId(id).orElseThrow().getStatus());
    }

    @Test
    void buscarPorIdInexistenteRetornaEmpty() {
        assertTrue(dao.buscarPorId(9999).isEmpty());
    }

    @Test
    void excluirRemove() {
        int id = dao.inserir(nova("Tarefa A", StatusTarefa.PENDENTE));

        dao.excluir(id);

        assertTrue(dao.buscarPorId(id).isEmpty());
    }
}
