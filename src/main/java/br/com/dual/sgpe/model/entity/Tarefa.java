package br.com.dual.sgpe.model.entity;

import br.com.dual.sgpe.model.enums.StatusTarefa;
import java.time.LocalDate;

/**
 * Entidade de domínio que representa uma tarefa vinculada a um projeto.
 *
 * <p>Mapeada para a tabela {@code tarefas}. O atributo {@code id} é {@code null}
 * antes da persistência. Cada tarefa pertence a exatamente um projeto e possui
 * um único responsável; ambos são referenciados por chave estrangeira.
 */
public class Tarefa {

    private Integer id;
    private String titulo;
    private String descricao;
    /** FK para a coluna {@code id} da tabela {@code projetos}. */
    private int projetoId;
    /** FK para a coluna {@code id} da tabela {@code usuarios} (responsável pela tarefa). */
    private int responsavelId;
    private LocalDate dataInicio;
    private LocalDate dataTerminoPrevista;
    private StatusTarefa status;

    public Tarefa() {
    }

    public Tarefa(String titulo, String descricao, int projetoId, int responsavelId,
                  LocalDate dataInicio, LocalDate dataTerminoPrevista, StatusTarefa status) {
        this(null, titulo, descricao, projetoId, responsavelId, dataInicio, dataTerminoPrevista, status);
    }

    public Tarefa(Integer id, String titulo, String descricao, int projetoId, int responsavelId,
                  LocalDate dataInicio, LocalDate dataTerminoPrevista, StatusTarefa status) {
        this.id = id;
        this.titulo = titulo;
        this.descricao = descricao;
        this.projetoId = projetoId;
        this.responsavelId = responsavelId;
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

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public int getProjetoId() {
        return projetoId;
    }

    public void setProjetoId(int projetoId) {
        this.projetoId = projetoId;
    }

    public int getResponsavelId() {
        return responsavelId;
    }

    public void setResponsavelId(int responsavelId) {
        this.responsavelId = responsavelId;
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

    public StatusTarefa getStatus() {
        return status;
    }

    public void setStatus(StatusTarefa status) {
        this.status = status;
    }
}
