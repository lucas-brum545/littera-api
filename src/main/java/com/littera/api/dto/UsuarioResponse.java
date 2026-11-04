package com.littera.api.dto;

public record UsuarioResponse(
        Long id,
        String nome,
        String email
) {
}
