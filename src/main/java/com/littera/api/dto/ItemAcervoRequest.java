package com.littera.api.dto;

import com.littera.api.model.TipoItemAcervo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ItemAcervoRequest(
        @NotNull(message = "O código é obrigatório")
        Long codigo,

        @NotBlank(message = "O título é obrigatório")
        String titulo,

        @NotNull(message = "O ano é obrigatório")
        Integer ano,

        @NotNull(message = "O tipo de item é obrigatório")
        TipoItemAcervo tipo,

        @NotBlank(message = "A localização é obrigatória")
        String localizacao,

        Integer totalDisponivel,
        Integer totalEmprestados,

        LocalDate cadastradoEm,

        String idioma,

        String descricao,
        String capa,

        // Campos específicos de Livro (opcionais no Request geral, validados no Service conforme o tipo)
        String autor,
        String isbn,
        String genero,


        // Campos específicos de Revista
        String edicao,
        String issn,
        String mesAnoPublicacao
) {
}