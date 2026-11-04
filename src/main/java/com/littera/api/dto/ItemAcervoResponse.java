package com.littera.api.dto;

import com.littera.api.model.ItemAcervo;
import com.littera.api.model.Livro;
import com.littera.api.model.Revista;
import com.littera.api.model.TipoItemAcervo;

import java.time.LocalDate;

public record ItemAcervoResponse (
        Long codigo,
        String titulo,
        Integer ano,
        TipoItemAcervo tipo,
        String localizacao,
        LocalDate cadastradoEm,
        String idioma,
        String descricao,
        String capa,

        // Atributos gerais / de Livro partilhados ou específicos
        Integer totalDisponivel,
        Integer totalEmprestados,

        // Atributos específicos de Livro
        String autor,
        String isbn,
        String genero,

        // Atributos específicos de Revista
        String edicao,
        String issn,
        String mesAnoPublicacao
){
    // Construtor polimórfico adaptado à tua estrutura exata
    public ItemAcervoResponse(ItemAcervo item) {
        this(
                item.getCodigo(),
                item.getTitulo(),
                item.getAno(),
                item.getTipo(),
                item.getLocalizacao(),
                item.getCadastradoEm(),
                item.getIdioma(),
                item.getDescricao(),
                item.getCapa(),
                item.getQuantidadeDisponivel(),
                item.getTotalEmprestados(),

                // Atributos de Livro (se for Livro preenche, senão null)
                item instanceof Livro l ? l.getAutor() : null,
                item instanceof Livro l ? l.getIsbn() : null,
                item instanceof Livro l ? l.getGenero() : null,

                // Atributos de Revista (se for Revista preenche, senão null)
                item instanceof Revista r ? r.getEdicao() : null,
                item instanceof Revista r ? r.getIssn() : null,
                item instanceof Revista r ? r.getMesAnoPublicacao() : null
        );
    }
}