package com.littera.api.dto;

import com.littera.api.model.TipoUsuario;

import java.time.LocalDate;

public record RegistroRequest(
        String nome, String email, String senha, TipoUsuario tipoUsuario, String telefone, LocalDate dataNascimento
) {
}
