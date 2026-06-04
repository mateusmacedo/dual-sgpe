package br.com.dual.sgpe.model.enums;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Verifica a hierarquia de perfis: {@code ADMINISTRADOR (3) > GERENTE (2) >
 * COLABORADOR (1)}, cobrindo os 9 pares de {@link PerfilUsuario#podeGerenciar}.
 */
class PerfilUsuarioTest {

    @Test
    void administradorGerenciaTodos() {
        assertTrue(PerfilUsuario.ADMINISTRADOR.podeGerenciar(PerfilUsuario.ADMINISTRADOR));
        assertTrue(PerfilUsuario.ADMINISTRADOR.podeGerenciar(PerfilUsuario.GERENTE));
        assertTrue(PerfilUsuario.ADMINISTRADOR.podeGerenciar(PerfilUsuario.COLABORADOR));
    }

    @Test
    void gerenteGerenciaGerenteEColaboradorMasNaoAdministrador() {
        assertFalse(PerfilUsuario.GERENTE.podeGerenciar(PerfilUsuario.ADMINISTRADOR));
        assertTrue(PerfilUsuario.GERENTE.podeGerenciar(PerfilUsuario.GERENTE));
        assertTrue(PerfilUsuario.GERENTE.podeGerenciar(PerfilUsuario.COLABORADOR));
    }

    @Test
    void colaboradorGerenciaApenasColaborador() {
        assertFalse(PerfilUsuario.COLABORADOR.podeGerenciar(PerfilUsuario.ADMINISTRADOR));
        assertFalse(PerfilUsuario.COLABORADOR.podeGerenciar(PerfilUsuario.GERENTE));
        assertTrue(PerfilUsuario.COLABORADOR.podeGerenciar(PerfilUsuario.COLABORADOR));
    }

    @Test
    void administradorNaoEhDesignavel() {
        assertFalse(PerfilUsuario.ADMINISTRADOR.isDesignavel());
    }

    @Test
    void gerenteEhDesignavel() {
        assertTrue(PerfilUsuario.GERENTE.isDesignavel());
    }

    @Test
    void colaboradorEhDesignavel() {
        assertTrue(PerfilUsuario.COLABORADOR.isDesignavel());
    }
}
