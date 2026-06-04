package br.com.dual.sgpe.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.dual.sgpe.dao.EquipeUsuarioDao;
import br.com.dual.sgpe.dao.ProjetoDao;
import br.com.dual.sgpe.dao.ProjetoEquipeDao;
import br.com.dual.sgpe.dao.TarefaDao;
import br.com.dual.sgpe.dao.UsuarioDao;
import br.com.dual.sgpe.security.EscopoColaborador;
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

    /** Perfil padrão do solicitante nos testes; gerencia o responsável GERENTE do setUp. */
    private static final PerfilUsuario SOLICITANTE = PerfilUsuario.GERENTE;

    @TempDir
    Path tempDir;

    private TarefaController controller;
    private UsuarioDao usuarioDao;
    private int projetoId;
    private int usuarioId;

    @BeforeEach
    void setUp() {
        String url = "jdbc:sqlite:" + tempDir.resolve("test.db");
        DatabaseConnection connection = new DatabaseConnection(url);
        new DatabaseMigrator(connection).migrate();

        ProjetoDao projetoDao = new ProjetoDao(connection);
        usuarioDao = new UsuarioDao(connection);
        TarefaDao tarefaDao = new TarefaDao(connection);
        EquipeUsuarioDao equipeUsuarioDao = new EquipeUsuarioDao(connection);
        ProjetoEquipeDao projetoEquipeDao = new ProjetoEquipeDao(connection);
        EscopoColaborador escopo = new EscopoColaborador(equipeUsuarioDao, projetoEquipeDao, projetoDao);
        controller = new TarefaController(tarefaDao, projetoDao, usuarioDao, escopo);

        projetoId = projetoDao.inserir(new Projeto("Projeto Teste", "Descrição",
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), StatusProjeto.PLANEJADO));
        usuarioId = usuarioDao.inserir(new Usuario("Usuário Teste", "12345678901",
            "teste@email.com", "Desenvolvedor", "user1", "senha123", PerfilUsuario.GERENTE));
    }

    private Tarefa nova(LocalDate inicio, LocalDate termino, StatusTarefa status) {
        return new Tarefa("Tarefa Teste", "Descrição", projetoId, usuarioId, inicio, termino, status);
    }

    /** Recupera o usuário responsável pelas tarefas criadas via {@link #nova}. */
    private Usuario responsavelTarefa() {
        return usuarioDao.buscarPorId(usuarioId).orElseThrow();
    }

    @Test
    void salvarSemStatusUsaPendente() {
        Tarefa salva = controller.salvar(
            nova(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), null), SOLICITANTE);

        assertNotNull(salva.getId());
        assertEquals(StatusTarefa.PENDENTE, salva.getStatus());
    }

    @Test
    void salvarComProjetoInexistenteLancaValidacao() {
        Tarefa tarefa = new Tarefa("Tarefa", "Desc", 9999, usuarioId,
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), StatusTarefa.PENDENTE);

        assertThrows(ValidacaoException.class, () -> controller.salvar(tarefa, SOLICITANTE));
    }

    @Test
    void salvarComResponsavelInexistenteLancaValidacao() {
        Tarefa tarefa = new Tarefa("Tarefa", "Desc", projetoId, 9999,
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), StatusTarefa.PENDENTE);

        assertThrows(ValidacaoException.class, () -> controller.salvar(tarefa, SOLICITANTE));
    }

    @Test
    void salvarTerminoAntesDoInicioLancaValidacao() {
        assertThrows(ValidacaoException.class, () -> controller.salvar(
            nova(LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 5), StatusTarefa.PENDENTE), SOLICITANTE));
    }

    @Test
    void salvarTerminoIgualAoInicioAceita() {
        Tarefa salva = controller.salvar(
            nova(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1), StatusTarefa.PENDENTE), SOLICITANTE);

        assertNotNull(salva.getId());
    }

    @Test
    void atualizarStatusPersiste() {
        Tarefa salva = controller.salvar(
            nova(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), StatusTarefa.PENDENTE), SOLICITANTE);

        controller.atualizarStatus(salva.getId(), StatusTarefa.CONCLUIDA, responsavelTarefa());

        assertEquals(StatusTarefa.CONCLUIDA,
            controller.buscarPorId(salva.getId()).orElseThrow().getStatus());
    }

    @Test
    void atualizarStatusPeloResponsavelAtualiza() {
        Tarefa salva = controller.salvar(
            nova(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), StatusTarefa.PENDENTE), SOLICITANTE);

        // O responsável pela tarefa pode atualizar o status da própria tarefa
        controller.atualizarStatus(salva.getId(), StatusTarefa.EM_ANDAMENTO, responsavelTarefa());

        assertEquals(StatusTarefa.EM_ANDAMENTO,
            controller.buscarPorId(salva.getId()).orElseThrow().getStatus());
    }

    @Test
    void atualizarStatusPorNaoResponsavelLancaValidacao() {
        Tarefa salva = controller.salvar(
            nova(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), StatusTarefa.PENDENTE), SOLICITANTE);
        int outroId = usuarioDao.inserir(new Usuario("Outro", "98765432100",
            "outro@email.com", "Analista", "outro", "senha123", PerfilUsuario.COLABORADOR));
        Usuario naoResponsavel = usuarioDao.buscarPorId(outroId).orElseThrow();

        // Quem não é o responsável pela tarefa não pode atualizar o seu status
        assertThrows(ValidacaoException.class,
            () -> controller.atualizarStatus(salva.getId(), StatusTarefa.CONCLUIDA, naoResponsavel));
    }

    @Test
    void findByProjetoIdFiltra() {
        controller.salvar(nova(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), StatusTarefa.PENDENTE), SOLICITANTE);
        controller.salvar(nova(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), StatusTarefa.EM_ANDAMENTO), SOLICITANTE);

        assertEquals(2, controller.findByProjetoId(projetoId).size());
        assertEquals(0, controller.findByProjetoId(9999).size());
    }

    @Test
    void atualizarSemIdLancaValidacao() {
        Tarefa tarefa = nova(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), StatusTarefa.PENDENTE);

        assertThrows(ValidacaoException.class, () -> controller.atualizar(tarefa, SOLICITANTE));
    }

    @Test
    void excluirRemoveTarefa() {
        Tarefa salva = controller.salvar(
            nova(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), StatusTarefa.PENDENTE), SOLICITANTE);

        controller.excluir(salva.getId());

        assertTrue(controller.buscarPorId(salva.getId()).isEmpty());
    }

    @Test
    void salvarSemTituloLancaValidacao() {
        Tarefa tarefa = new Tarefa("   ", "Desc", projetoId, usuarioId,
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), StatusTarefa.PENDENTE);

        assertThrows(ValidacaoException.class, () -> controller.salvar(tarefa, SOLICITANTE));
    }

    @Test
    void salvarComResponsavelSuperiorLancaValidacao() {
        int adminId = usuarioDao.inserir(new Usuario("Admin", "99999999999",
            "admin@email.com", "Diretor", "admin", "senha123", PerfilUsuario.ADMINISTRADOR));
        Tarefa tarefa = new Tarefa("Tarefa", "Desc", projetoId, adminId,
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), StatusTarefa.PENDENTE);

        // GERENTE não pode designar tarefa a um responsável ADMINISTRADOR (nível superior)
        assertThrows(ValidacaoException.class,
            () -> controller.salvar(tarefa, PerfilUsuario.GERENTE));
    }

    @Test
    void salvarComResponsavelAdminComoAdminLancaValidacao() {
        int adminId = usuarioDao.inserir(new Usuario("Admin", "99999999999",
            "admin@email.com", "Diretor", "admin", "senha123", PerfilUsuario.ADMINISTRADOR));
        Tarefa tarefa = new Tarefa("Tarefa", "Desc", projetoId, adminId,
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), StatusTarefa.PENDENTE);

        // Nem mesmo ADMINISTRADOR pode designar outro admin como responsável
        assertThrows(ValidacaoException.class,
            () -> controller.salvar(tarefa, PerfilUsuario.ADMINISTRADOR));
    }

    @Test
    void salvarForaDoEscopoLancaValidacao() {
        int gerenteId = usuarioDao.inserir(new Usuario("Gerente", "33333333333",
            "gerente@email.com", "Gestor", "gerente", "senha123", PerfilUsuario.GERENTE));
        Usuario gerente = usuarioDao.buscarPorId(gerenteId).orElseThrow();
        Tarefa tarefa = nova(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), StatusTarefa.PENDENTE);

        assertThrows(ValidacaoException.class, () -> controller.salvar(tarefa, gerente));
    }

    @Test
    void excluirForaDoEscopoLancaValidacao() {
        Tarefa salva = controller.salvar(
            nova(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), StatusTarefa.PENDENTE), SOLICITANTE);
        int gerenteId = usuarioDao.inserir(new Usuario("Gerente", "33333333333",
            "gerente@email.com", "Gestor", "gerente", "senha123", PerfilUsuario.GERENTE));
        Usuario gerente = usuarioDao.buscarPorId(gerenteId).orElseThrow();

        assertThrows(ValidacaoException.class,
            () -> controller.excluir(salva.getId(), gerente));
    }

    @Test
    void listarResponsaveisExcluiAdministrador() {
        usuarioDao.inserir(new Usuario("Admin", "99999999999",
            "admin@email.com", "Diretor", "admin", "senha123", PerfilUsuario.ADMINISTRADOR));

        var responsaveis = controller.listarResponsaveis(PerfilUsuario.ADMINISTRADOR);

        assertTrue(responsaveis.stream()
            .noneMatch(u -> u.getPerfil() == PerfilUsuario.ADMINISTRADOR));
    }
}
