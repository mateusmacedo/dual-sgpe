package br.com.dual.sgpe.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.dual.sgpe.database.DatabaseConnection;
import br.com.dual.sgpe.database.DatabaseMigrator;
import br.com.dual.sgpe.model.entity.Equipe;
import br.com.dual.sgpe.model.entity.Projeto;
import br.com.dual.sgpe.model.enums.StatusProjeto;
import br.com.dual.sgpe.model.filter.ProjetoFiltro;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjetoDaoConsultaTest {

    @TempDir
    Path tempDir;

    private ProjetoDao dao;
    private int equipeId;

    @BeforeEach
    void setUp() {
        String url = "jdbc:sqlite:" + tempDir.resolve("test.db");
        DatabaseConnection connection = new DatabaseConnection(url);
        new DatabaseMigrator(connection).migrate();
        dao = new ProjetoDao(connection);

        dao.inserir(new Projeto("Alpha", "Desc", LocalDate.of(2026, 1, 15),
            LocalDate.of(2026, 6, 30), StatusProjeto.PLANEJADO));
        dao.inserir(new Projeto("Beta", "Desc", LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 9, 30), StatusProjeto.EM_ANDAMENTO));
        dao.inserir(new Projeto("Gamma", "Desc", LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 12, 31), StatusProjeto.EM_ANDAMENTO));

        EquipeDao equipeDao = new EquipeDao(connection);
        equipeId = equipeDao.inserir(new Equipe("Equipe A", "Desc"));

        ProjetoEquipeDao projetoEquipeDao = new ProjetoEquipeDao(connection);
        projetoEquipeDao.vincular(1, equipeId);
    }

    @Test
    void semFiltroRetornaTodos() {
        List<Projeto> resultado = dao.consultar(new ProjetoFiltro());

        assertEquals(3, resultado.size());
    }

    @Test
    void filtroPorNomeParcialCaseInsensitive() {
        ProjetoFiltro filtro = new ProjetoFiltro();
        filtro.setNome("alp");

        List<Projeto> resultado = dao.consultar(filtro);

        assertEquals(1, resultado.size());
        assertEquals("Alpha", resultado.get(0).getNome());
    }

    @Test
    void filtroPorStatus() {
        ProjetoFiltro filtro = new ProjetoFiltro();
        filtro.setStatus(StatusProjeto.EM_ANDAMENTO);

        List<Projeto> resultado = dao.consultar(filtro);

        assertEquals(2, resultado.size());
    }

    @Test
    void filtroPorPeriodoDeInicio() {
        ProjetoFiltro filtro = new ProjetoFiltro();
        filtro.setDataInicioDe(LocalDate.of(2026, 2, 1));
        filtro.setDataInicioAte(LocalDate.of(2026, 4, 30));

        List<Projeto> resultado = dao.consultar(filtro);

        assertEquals(1, resultado.size());
        assertEquals("Beta", resultado.get(0).getNome());
    }

    @Test
    void filtroPorEquipeVinculada() {
        ProjetoFiltro filtro = new ProjetoFiltro();
        filtro.setEquipeId(equipeId);

        List<Projeto> resultado = dao.consultar(filtro);

        assertEquals(1, resultado.size());
        assertEquals("Alpha", resultado.get(0).getNome());
    }

    @Test
    void combinacaoDeFiltrosAplicaAnd() {
        ProjetoFiltro filtro = new ProjetoFiltro();
        filtro.setStatus(StatusProjeto.EM_ANDAMENTO);
        filtro.setNome("bet");

        List<Projeto> resultado = dao.consultar(filtro);

        assertEquals(1, resultado.size());
        assertEquals("Beta", resultado.get(0).getNome());
    }

    @Test
    void filtroSemResultadoRetornaListaVazia() {
        ProjetoFiltro filtro = new ProjetoFiltro();
        filtro.setNome("inexistente");

        List<Projeto> resultado = dao.consultar(filtro);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void periodoComApenasDataDeIgnoraFiltro() {
        ProjetoFiltro filtro = new ProjetoFiltro();
        filtro.setDataInicioDe(LocalDate.of(2026, 5, 1));

        List<Projeto> resultado = dao.consultar(filtro);

        assertEquals(3, resultado.size());
    }

    @Test
    void periodoComApenasDataAteIgnoraFiltro() {
        ProjetoFiltro filtro = new ProjetoFiltro();
        filtro.setDataInicioAte(LocalDate.of(2026, 4, 30));

        List<Projeto> resultado = dao.consultar(filtro);

        assertEquals(3, resultado.size());
    }

    @Test
    void consultarComFiltroNuloRetornaTodos() {
        assertEquals(3, dao.consultar(null).size());
    }
}
