package br.com.dual.sgpe.model.entity;

import br.com.dual.sgpe.model.enums.PerfilUsuario;

/**
 * Entidade de domínio que representa um usuário do sistema (RF002).
 *
 * <p>Mapeada para a tabela {@code usuarios}. O atributo {@code id} é {@code null}
 * antes da persistência e recebe o valor gerado pelo banco após a inserção.
 */
public class Usuario {

    private Integer id;
    private String nomeCompleto;
    private String cpf;
    private String email;
    private String cargo;
    private String login;
    /** Senha armazenada em texto simples (hash não implementado na versão atual). */
    private String senha;
    private PerfilUsuario perfil;

    public Usuario() {
    }

    public Usuario(String nomeCompleto, String cpf, String email, String cargo,
                   String login, String senha, PerfilUsuario perfil) {
        this(null, nomeCompleto, cpf, email, cargo, login, senha, perfil);
    }

    public Usuario(Integer id, String nomeCompleto, String cpf, String email, String cargo,
                   String login, String senha, PerfilUsuario perfil) {
        this.id = id;
        this.nomeCompleto = nomeCompleto;
        this.cpf = cpf;
        this.email = email;
        this.cargo = cargo;
        this.login = login;
        this.senha = senha;
        this.perfil = perfil;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public void setNomeCompleto(String nomeCompleto) {
        this.nomeCompleto = nomeCompleto;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public PerfilUsuario getPerfil() {
        return perfil;
    }

    public void setPerfil(PerfilUsuario perfil) {
        this.perfil = perfil;
    }
}
