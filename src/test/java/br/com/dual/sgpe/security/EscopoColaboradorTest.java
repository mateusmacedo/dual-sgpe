package br.com.dual.sgpe.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.dual.sgpe.dao.EquipeDao;
import br.com.dual.sgpe.dao.EquipeUsuarioDao;
import br.com.dual.sgpe.dao.ProjetoDao;
import br.com.dual.sgpe.dao.ProjetoEquipeDao;
import br.com.dual.sgpe.dao.UsuarioDao;
import br.com.dual.sgpe.database.DatabaseConnection;
import br.com.dual.sgpe.database.DatabaseMigrator;
import br.com.dual.sgpe.model.entity.Equipe;
import br.com.dual.sgpe.model.entity.Projeto;
import br.com.dual.sgpe.model.entity.Usuario;
import br.com.dual.sgpe.model.enums.PerfilUsuario;
import br.com.dual.sgpe.model.enums.StatusProjeto;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EscopoColaboradorTest {

    @TempDir
    Path tempDir;

    private EscopoColaborador escopo;
    private int colaboradorId;
    private int outroUsuarioId;
    private int projeto1Id;
    private int projeto2Id;
    private int projeto3Id;

    @BeforeEach
    void setUp() {
        String url = "jdbc:sqlite:" + tempDir.resolve("test.db");
        DatabaseConnection connection = new DatabaseConnection(url);
        new DatabaseMigrator(connection).migrate();

        UsuarioDao usuarioDao = new UsuarioDao(connection);
        EquipeDao equipeDao = new EquipeDao(connection);
        ProjetoDao projetoDao = new ProjetoDao(connection);
        EquipeUsuarioDao equipeUsuarioDao = new EquipeUsuarioDao(connection);
        ProjetoEquipeDao projetoEquipeDao = new ProjetoEquipeDao(connection);

        escopo = new EscopoColaborador(equipeUsuarioDao, projetoEquipeDao, projetoDao);

        colaboradorId = usuarioDao.inserir(new Usuario("Colaborador", "11111111111",
            "colab@test.com", "Dev", "colab", "senha", PerfilUsuario.COLABORADOR));
        outroUsuarioId = usuarioDao.inserir(new Usuario("Outro", "22222222222",
            "outro@test.com", "Dev", "outro", "senha", PerfilUsuario.COLABORADOR));

        int equipe1Id = equipeDao.inserir(new Equipe("Equipe 1", "Descrição"));
        int equipe2Id = equipeDao.inserir(new Equipe("Equipe 2", "Descrição"));

        projeto1Id = projetoDao.inserir(novoProjeto("Projeto 1"));
        projeto2Id = projetoDao.inserir(novoProjeto("Projeto 2"));
        projeto3Id = projetoDao.inserir(novoProjeto("Projeto 3"));

        // Colaborador participa da Equipe 1; Equipe 1 -> Projetos 1 e 2.
        equipeUsuarioDao.vincular(equipe1Id, colaboradorId);
        projetoEquipeDao.vincular(projeto1Id, equipe1Id);
        projetoEquipeDao.vincular(projeto2Id, equipe1Id);

        // Projeto 3 pertence à Equipe 2, da qual o colaborador não participa.
        projetoEquipeDao.vincular(projeto3Id, equipe2Id);
    }

    private Projeto novoProjeto(String nome) {
        return new Projeto(nome, "Descrição", LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 12, 31), StatusProjeto.PLANEJADO);
    }

    @Test
    void projetosDoColaboradorRetornaApenasProjetosDasSuasEquipes() {
        List<Projeto> projetos = escopo.projetosDoColaborador(colaboradorId);

        assertEquals(2, projetos.size());
        List<Integer> ids = projetos.stream().map(Projeto::getId).toList();
        assertTrue(ids.contains(projeto1Id));
        assertTrue(ids.contains(projeto2Id));
        assertFalse(ids.contains(projeto3Id));
    }

    @Test
    void projetoIdsDoColaboradorRetornaIdsSemDuplicatas() {
        Set<Integer> ids = escopo.projetoIdsDoColaborador(colaboradorId);

        assertEquals(2, ids.size());
        assertTrue(ids.contains(projeto1Id));
        assertTrue(ids.contains(projeto2Id));
        assertFalse(ids.contains(projeto3Id));
    }

    @Test
    void colaboradorSemEquipeNaoEnxergaProjetos() {
        assertEquals(0, escopo.projetosDoColaborador(outroUsuarioId).size());
        assertEquals(0, escopo.projetoIdsDoColaborador(outroUsuarioId).size());
    }
}
