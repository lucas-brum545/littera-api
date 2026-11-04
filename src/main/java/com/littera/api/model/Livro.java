package com.littera.api.model;

import jakarta.persistence.*;

@Entity
@Table(name = "livro")
public class Livro extends ItemAcervo {

    @Column(nullable = false, length = 100)
    private String autor;

    @Column(nullable = false, length = 20)
    private String isbn;

    @Column(nullable = false, length = 50)
    private String genero;

    // Getters e Setters específicos de Livro
    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getGenero() {
        return genero;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }
}