package br.com.dual.sgpe.security;

import br.com.dual.sgpe.model.enums.PerfilUsuario;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Mapeamento declarativo de RBAC: associa cada {@link PerfilUsuario} ao
 * conjunto de {@link Recurso}s que pode acessar (PRD §6).
 *
 * <p>Regra de acesso isolada da camada de view, para ser testável sem Swing.
 */
public final class PermissoesPerfil {

    private static final Map<PerfilUsuario, Set<Recurso>> RECURSOS_POR_PERFIL = criarMapa();

    private PermissoesPerfil() {
    }

    private static Map<PerfilUsuario, Set<Recurso>> criarMapa() {
        Map<PerfilUsuario, Set<Recurso>> mapa = new EnumMap<>(PerfilUsuario.class);
        mapa.put(PerfilUsuario.ADMINISTRADOR, EnumSet.of(
            Recurso.USUARIOS, Recurso.PROJETOS, Recurso.TAREFAS,
            Recurso.EQUIPES, Recurso.CONSULTA, Recurso.RELATORIO));
        mapa.put(PerfilUsuario.GERENTE, EnumSet.of(
            Recurso.PROJETOS, Recurso.TAREFAS, Recurso.EQUIPES,
            Recurso.CONSULTA, Recurso.RELATORIO));
        mapa.put(PerfilUsuario.COLABORADOR, EnumSet.of(
            Recurso.CONSULTA, Recurso.TAREFAS, Recurso.RELATORIO));
        return mapa;
    }

    /**
     * Retorna uma cópia defensiva do conjunto de recursos permitidos ao perfil.
     * Perfil desconhecido ou nulo resulta em conjunto vazio (nega tudo).
     */
    public static Set<Recurso> recursosDe(PerfilUsuario perfil) {
        Set<Recurso> recursos = RECURSOS_POR_PERFIL.get(perfil);
        if (recursos == null) {
            return EnumSet.noneOf(Recurso.class);
        }
        return EnumSet.copyOf(recursos);
    }

    /**
     * Indica se o perfil pode acessar o recurso informado.
     */
    public static boolean podeAcessar(PerfilUsuario perfil, Recurso recurso) {
        Set<Recurso> recursos = RECURSOS_POR_PERFIL.get(perfil);
        return recursos != null && recursos.contains(recurso);
    }
}
