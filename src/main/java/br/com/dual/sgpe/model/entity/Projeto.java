package br.com.dual.sgpe.model.entity;

import br.com.dual.sgpe.model.enums.StatusProjeto;
import java.time.LocalDate;

/**
 * Entidade de projeto. Datas como {@link LocalDate} (armazenadas como TEXT ISO
 * no banco). O {@code id} é nulo antes da persistência.
 */
public class Projeto {

    private Integer id;
    private String nome;
    private String descricao;
    private LocalDate dataInicio;
    private LocalDate dataTerminoPrevista;
    private StatusProjeto status;

    public Projeto() {
    }

    public Projeto(String nome, String descricao, LocalDate dataInicio,
                   LocalDate dataTerminoPrevista, StatusProjeto status) {
        this(null, nome, descricao, dataInicio, dataTerminoPrevista, status);
    }

    public Projeto(Integer id, String nome, String descricao, LocalDate dataInicio,
                   LocalDate dataTerminoPrevista, StatusProjeto status) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.dataInicio = dataInicio;
        this.dataTerminoPrevista = dataTerminoPrevista;
        this.status = status;
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

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDate dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDate getDataTerminoPrevista() {
        return dataTerminoPrevista;
    }

    public void setDataTerminoPrevista(LocalDate dataTerminoPrevista) {
        this.dataTerminoPrevista = dataTerminoPrevista;
    }

    public StatusProjeto getStatus() {
        return status;
    }

    public void setStatus(StatusProjeto status) {
        this.status = status;
    }
}
