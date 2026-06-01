package br.com.dual.sgpe.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.dual.sgpe.model.enums.PerfilUsuario;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PermissoesPerfilTest {

    @Test
    void administradorAcessaTodosOsSeisRecursos() {
        Set<Recurso> recursos = PermissoesPerfil.recursosDe(PerfilUsuario.ADMINISTRADOR);

        assertEquals(6, recursos.size());
        assertTrue(recursos.contains(Recurso.USUARIOS));
        assertTrue(recursos.contains(Recurso.PROJETOS));
        assertTrue(recursos.contains(Recurso.TAREFAS));
        assertTrue(recursos.contains(Recurso.EQUIPES));
        assertTrue(recursos.contains(Recurso.CONSULTA));
        assertTrue(recursos.contains(Recurso.RELATORIO));
    }

    @Test
    void gerenteAcessaCincoRecursosSemUsuarios() {
        Set<Recurso> recursos = PermissoesPerfil.recursosDe(PerfilUsuario.GERENTE);

        assertEquals(5, recursos.size());
        assertFalse(recursos.contains(Recurso.USUARIOS));
        assertTrue(recursos.contains(Recurso.PROJETOS));
        assertTrue(recursos.contains(Recurso.TAREFAS));
        assertTrue(recursos.contains(Recurso.EQUIPES));
        assertTrue(recursos.contains(Recurso.CONSULTA));
        assertTrue(recursos.contains(Recurso.RELATORIO));
    }

    @Test
    void colaboradorAcessaApenasConsultaTarefasRelatorio() {
        Set<Recurso> recursos = PermissoesPerfil.recursosDe(PerfilUsuario.COLABORADOR);

        assertEquals(3, recursos.size());
        assertTrue(recursos.contains(Recurso.CONSULTA));
        assertTrue(recursos.contains(Recurso.TAREFAS));
        assertTrue(recursos.contains(Recurso.RELATORIO));
        assertFalse(recursos.contains(Recurso.USUARIOS));
        assertFalse(recursos.contains(Recurso.PROJETOS));
        assertFalse(recursos.contains(Recurso.EQUIPES));
    }

    @Test
    void podeAcessarRefleteOMapaDePermissoes() {
        assertTrue(PermissoesPerfil.podeAcessar(PerfilUsuario.ADMINISTRADOR, Recurso.USUARIOS));
        assertFalse(PermissoesPerfil.podeAcessar(PerfilUsuario.GERENTE, Recurso.USUARIOS));
        assertTrue(PermissoesPerfil.podeAcessar(PerfilUsuario.COLABORADOR, Recurso.TAREFAS));
        assertFalse(PermissoesPerfil.podeAcessar(PerfilUsuario.COLABORADOR, Recurso.PROJETOS));
    }

    @Test
    void recursosDeRetornaCopiaDefensiva() {
        Set<Recurso> recursos = PermissoesPerfil.recursosDe(PerfilUsuario.COLABORADOR);
        recursos.clear();

        assertEquals(3, PermissoesPerfil.recursosDe(PerfilUsuario.COLABORADOR).size());
    }
}
