package br.com.dual.sgpe.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.dual.sgpe.dao.EquipeDao;
import br.com.dual.sgpe.dao.ProjetoDao;
import br.com.dual.sgpe.dao.ProjetoEquipeDao;
import br.com.dual.sgpe.database.DatabaseConnection;
import br.com.dual.sgpe.database.DatabaseMigrator;
import br.com.dual.sgpe.exception.ExclusaoBloqueadaException;
import br.com.dual.sgpe.exception.RegistroDuplicadoException;
import br.com.dual.sgpe.exception.ValidacaoException;
import br.com.dual.sgpe.model.entity.Equipe;
import br.com.dual.sgpe.model.entity.Projeto;
import br.com.dual.sgpe.model.enums.StatusProjeto;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjetoControllerTest {

    @TempDir
    Path tempDir;

    private DatabaseConnection connection;
    private ProjetoController controller;

    @BeforeEach
    void setUp() {
        String url = "jdbc:sqlite:" + tempDir.resolve("test.db");
        connection = new DatabaseConnection(url);
        new DatabaseMigrator(connection).migrate();
        controller = new ProjetoController(new ProjetoDao(connection),
            new ProjetoEquipeDao(connection), new EquipeDao(connection));
    }

    private Projeto novo(String nome, LocalDate inicio, LocalDate termino, StatusProjeto status) {
        return new Projeto(nome, "Descrição", inicio, termino, status);
    }

    @Test
    void salvarSemStatusUsaPlanejado() {
        Projeto salvo = controller.salvar(
            novo("Projeto A", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), null));

        assertNotNull(salvo.getId());
        assertEquals(StatusProjeto.PLANEJADO, salvo.getStatus());
    }

    @Test
    void salvarSemNomeLancaValidacao() {
        assertThrows(ValidacaoException.class, () -> controller.salvar(
            novo("   ", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), StatusProjeto.PLANEJADO)));
    }

    @Test
    void salvarTerminoAntesDoInicioLancaValidacao() {
        assertThrows(ValidacaoException.class, () -> controller.salvar(
            novo("A", LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 5), StatusProjeto.PLANEJADO)));
    }

    @Test
    void salvarTerminoIgualAoInicioAceita() {
        Projeto salvo = controller.salvar(
            novo("A", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1), StatusProjeto.PLANEJADO));

        assertNotNull(salvo.getId());
    }

    @Test
    void atualizarMudaStatus() {
        Projeto salvo = controller.salvar(
            novo("A", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), StatusProjeto.PLANEJADO));
        salvo.setStatus(StatusProjeto.EM_ANDAMENTO);

        controller.atualizar(salvo);

        assertEquals(StatusProjeto.EM_ANDAMENTO,
            controller.buscarPorId(salvo.getId()).orElseThrow().getStatus());
    }

    @Test
    void excluirReferenciadoLancaExcecao() throws SQLException {
        Projeto salvo = controller.salvar(
            novo("A", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), StatusProjeto.PLANEJADO));
        try (Connection conexao = connection.getConnection();
             Statement statement = conexao.createStatement()) {
            statement.executeUpdate("INSERT INTO equipes (nome, descricao) VALUES ('Equipe', 'Desc')");
            statement.executeUpdate(
                "INSERT INTO projeto_equipe (projeto_id, equipe_id) VALUES (" + salvo.getId() + ", 1)");
        }

        assertThrows(ExclusaoBloqueadaException.class, () -> controller.excluir(salvo.getId()));
    }

    @Test
    void listarPorStatusFiltra() {
        controller.salvar(novo("A", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), StatusProjeto.PLANEJADO));
        controller.salvar(novo("B", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), StatusProjeto.CONCLUIDO));

        assertEquals(1, controller.listarPorStatus(StatusProjeto.PLANEJADO).size());
    }

    @Test
    void vincularEquipeInexistenteLancaValidacao() {
        Projeto salvo = controller.salvar(
            novo("A", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), StatusProjeto.PLANEJADO));

        assertThrows(ValidacaoException.class,
            () -> controller.vincularEquipe(salvo.getId(), 9999));
    }

    @Test
    void vincularEquipeDuplicadaLancaRegistroDuplicado() {
        Projeto salvo = controller.salvar(
            novo("A", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), StatusProjeto.PLANEJADO));
        Equipe equipe = new Equipe("Equipe A", "Desc");
        new EquipeDao(connection).inserir(equipe);
        controller.vincularEquipe(salvo.getId(), equipe.getId());

        assertThrows(RegistroDuplicadoException.class,
            () -> controller.vincularEquipe(salvo.getId(), equipe.getId()));
    }

    @Test
    void listarEquipesDoProjetoRetornaCorreto() {
        Projeto salvo = controller.salvar(
            novo("A", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), StatusProjeto.PLANEJADO));
        EquipeDao equipeDao = new EquipeDao(connection);
        Equipe e1 = new Equipe("Equipe 1", "Desc");
        Equipe e2 = new Equipe("Equipe 2", "Desc");
        equipeDao.inserir(e1);
        equipeDao.inserir(e2);
        controller.vincularEquipe(salvo.getId(), e1.getId());
        controller.vincularEquipe(salvo.getId(), e2.getId());

        java.util.List<Equipe> resultado = controller.listarEquipesDoProjeto(salvo.getId());
        assertEquals(2, resultado.size());
        assertTrue(resultado.stream().anyMatch(e -> e.getId().equals(e1.getId())));
        assertTrue(resultado.stream().anyMatch(e -> e.getId().equals(e2.getId())));
    }

    @Test
    void vincularProjetoInexistenteLancaValidacao() {
        Equipe equipe = new Equipe("Equipe A", "Desc");
        new EquipeDao(connection).inserir(equipe);

        assertThrows(ValidacaoException.class,
            () -> controller.vincularEquipe(9999, equipe.getId()));
    }

    @Test
    void listarProjetosDaEquipeRetornaCorreto() {
        Projeto p1 = controller.salvar(
            novo("P1", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), StatusProjeto.PLANEJADO));
        Projeto p2 = controller.salvar(
            novo("P2", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), StatusProjeto.PLANEJADO));
        Equipe equipe = new Equipe("Equipe A", "Desc");
        new EquipeDao(connection).inserir(equipe);
        controller.vincularEquipe(p1.getId(), equipe.getId());
        controller.vincularEquipe(p2.getId(), equipe.getId());

        java.util.List<Projeto> resultado = controller.listarProjetosDaEquipe(equipe.getId());
        assertEquals(2, resultado.size());
    }

    @Test
    void listarPorStatusNuloRetornaTodos() {
        controller.salvar(novo("A", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), StatusProjeto.PLANEJADO));
        controller.salvar(novo("B", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), StatusProjeto.CONCLUIDO));

        assertEquals(2, controller.listarPorStatus(null).size());
    }
}
