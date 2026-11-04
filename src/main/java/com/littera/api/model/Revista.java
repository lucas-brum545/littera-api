package com.littera.api.model;

import jakarta.persistence.*;

@Entity
@Table(name = "revista")
public class Revista extends ItemAcervo {

    @Column(name = "edicao", nullable = false, length = 10)
    private String edicao;

    @Column(name = "issn", nullable = false, length = 20)
    private String issn;

    @Column(name = "mes_ano_publicacao", nullable = false, length = 7)
    private String mesAnoPublicacao;

    // Getters e Setters específicos de Revista
    public String getEdicao() {
        return edicao;
    }

    public void setEdicao(String edicao) {
        this.edicao = edicao;
    }

    public String getIssn() {
        return issn;
    }

    public void setIssn(String issn) {
        this.issn = issn;
    }

    public String getMesAnoPublicacao() {
        return mesAnoPublicacao;
    }

    public void setMesAnoPublicacao(String mesAnoPublicacao) {
        this.mesAnoPublicacao = mesAnoPublicacao;
    }
}