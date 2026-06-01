package br.com.dual.sgpe.model.entity;

/**
 * Entidade de domínio que representa uma equipe de trabalho.
 *
 * <p>Mapeada para a tabela {@code equipes}. O atributo {@code id} é {@code null}
 * antes da persistência e recebe o valor gerado pelo banco após a inserção.
 * Uma equipe pode estar associada a múltiplos projetos.
 */
public class Equipe {

    private Integer id;
    private String nome;
    private String descricao;

    public Equipe() {
    }

    public Equipe(String nome, String descricao) {
        this(null, nome, descricao);
    }

    public Equipe(Integer id, String nome, String descricao) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}
