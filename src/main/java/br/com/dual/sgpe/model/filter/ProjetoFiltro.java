package br.com.dual.sgpe.model.filter;

import br.com.dual.sgpe.model.enums.StatusProjeto;
import java.time.LocalDate;

/**
 * Critérios de consulta para busca de projetos.
 *
 * <p>Campos {@code null} são ignorados pela camada DAO (filtro opcional).
 * {@code dataInicioDe} e {@code dataInicioAte} formam um intervalo fechado
 * sobre a coluna {@code data_inicio} da tabela {@code projetos}.
 */
public class ProjetoFiltro {

    private String nome;
    private StatusProjeto status;
    private LocalDate dataInicioDe;
    private LocalDate dataInicioAte;
    /** FK para a coluna {@code id} da tabela {@code equipes}; filtra projetos da equipe. */
    private Integer equipeId;

    public ProjetoFiltro() {
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public StatusProjeto getStatus() {
        return status;
    }

    public void setStatus(StatusProjeto status) {
        this.status = status;
    }

    public LocalDate getDataInicioDe() {
        return dataInicioDe;
    }

    public void setDataInicioDe(LocalDate dataInicioDe) {
        this.dataInicioDe = dataInicioDe;
    }

    public LocalDate getDataInicioAte() {
        return dataInicioAte;
    }

    public void setDataInicioAte(LocalDate dataInicioAte) {
        this.dataInicioAte = dataInicioAte;
    }

    public Integer getEquipeId() {
        return equipeId;
    }

    public void setEquipeId(Integer equipeId) {
        this.equipeId = equipeId;
    }
}
