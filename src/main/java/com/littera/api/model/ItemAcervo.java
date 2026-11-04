package com.littera.api.model;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "item_acervo")
@Inheritance(strategy = InheritanceType.JOINED) // Define como vira tabela
public abstract class ItemAcervo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    @Column(nullable = false, length = 200)
    private String titulo;

    private Integer ano;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoItemAcervo tipo; // 'LIVRO' ou 'REVISTA'

    @Column(name = "total_disponivel")
    private Integer totalDisponivel = 1;

    @Column(name = "total_emprestados")
    private Integer totalEmprestados = 0;

    @Column(nullable = false, length = 50)
    private String localizacao;

    @Column(name = "cadastrado_em")
    private LocalDate cadastradoEm;

    @Column(length = 20)
    private String idioma;

    @Column(columnDefinition = "TEXT")
    private String capa;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "link_google")
    private String linkOpenLibrary;

    @Column(name = "link_crossref")
    private String linkCrossref;

    // Construtores, Getters e Setters

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public Integer getAno() {
        return ano;
    }

    public void setAno(Integer ano) {
        this.ano = ano;
    }

    public TipoItemAcervo getTipoItemAcervo() {
        return tipo;
    }

    public void setTipoItemAcervo(TipoItemAcervo tipo) {
        this.tipo = tipo;
    }

    public String getLocalizacao() {
        return localizacao;
    }

    public void setLocalizacao(String localizacao) {
        this.localizacao = localizacao;
    }

    public TipoItemAcervo getTipo() {
        return tipo;
    }

    public void setTipo(TipoItemAcervo tipo) {
        this.tipo = tipo;
    }

    public LocalDate getCadastradoEm() {
        return cadastradoEm;
    }

    public void setCadastradoEm(LocalDate cadastradoEm) {
        this.cadastradoEm = cadastradoEm;
    }

    public String getIdioma() {
        return idioma;
    }

    public void setIdioma(String idioma) {
        this.idioma = idioma;
    }

    public Integer getQuantidadeDisponivel() {
        return totalDisponivel;
    }

    public void setQuantidadeDisponivel(Integer totalDisponivel) {
        this.totalDisponivel = totalDisponivel;
    }

    public Integer getTotalEmprestados() {
        return totalEmprestados;
    }

    public void setTotalEmprestados(Integer totalEmprestados) {
        this.totalEmprestados = totalEmprestados;
    }

    public String getCapa() {
        return capa;
    }

    public void setCapa(String capa) {
        this.capa = capa;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getLinkOpenLibrary() {
        return linkOpenLibrary;
    }

    public void setLinkOpenLibrary(String linkOpenLibrary) {
        this.linkOpenLibrary = linkOpenLibrary;
    }

    public String getLinkCrossref() {
        return linkCrossref;
    }

    public void setLinkCrossref(String linkCrossref) {
        this.linkCrossref = linkCrossref;
    }
}
