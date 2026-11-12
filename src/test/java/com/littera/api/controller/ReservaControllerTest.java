package com.littera.api.controller;

import com.littera.api.dto.ReservaRequest;
import com.littera.api.dto.ReservaResponse;
import com.littera.api.exception.RecursoNaoEncontradoException;
import com.littera.api.repository.UsuarioRepository;
import com.littera.api.service.ReservaService;
import com.littera.api.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de "fatia" web: só o controller, o @ControllerAdvice e o MockMvc.
 * O service é um mock; aqui se testa rota, status HTTP, JSON de entrada e tratamento de erro.
 *
 * Os DTOs de reserva não foram enviados, então o ReservaResponse é criado por reflexão
 * (todos os campos com valor padrão) e o JSON de entrada usa nomes assumidos.
 * Ajuste JSON_RESERVA aos componentes reais do ReservaRequest.
 */
@WebMvcTest(ReservaController.class)
@AutoConfigureMockMvc(addFilters = false) // desliga Spring Security/filtros nesta fatia
class ReservaControllerTest {

    // ASSUMIDO: confirme os nomes dos campos do ReservaRequest.
    private static final String JSON_RESERVA = """
            {"codigoUsuario":1,"codigoItemAcervo":10}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservaService reservaService;

    // O SecurityFilter (@Component) entra na fatia web e precisa destas duas dependências.
    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private ReservaResponse response;

    @BeforeEach
    void setUp() throws Exception {
        response = instanciar(ReservaResponse.class);
    }

    /** Cria um DTO sem conhecer seus campos: record com valores padrão, ou classe com construtor vazio. */
    private static <T> T instanciar(Class<T> tipo) throws Exception {
        if (tipo.isRecord()) {
            RecordComponent[] componentes = tipo.getRecordComponents();
            Class<?>[] tipos = new Class<?>[componentes.length];
            Object[] argumentos = new Object[componentes.length];
            for (int i = 0; i < componentes.length; i++) {
                tipos[i] = componentes[i].getType();
                argumentos[i] = tipos[i].isPrimitive() ? Array.get(Array.newInstance(tipos[i], 1), 0) : null;
            }
            Constructor<T> construtor = tipo.getDeclaredConstructor(tipos);
            construtor.setAccessible(true);
            return construtor.newInstance(argumentos);
        }
        Constructor<T> construtor = tipo.getDeclaredConstructor();
        construtor.setAccessible(true);
        return construtor.newInstance();
    }

    // ----------------------------------------------------------------- GET

    @Test
    @DisplayName("GET /api/reservas: 200 com a lista")
    void listarTodas() throws Exception {
        when(reservaService.listarTodas()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/reservas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/reservas: 200 com lista vazia")
    void listarTodas_vazio() throws Exception {
        when(reservaService.listarTodas()).thenReturn(List.of());

        mockMvc.perform(get("/api/reservas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/reservas/{id}: 200 quando existe")
    void buscarPorId() throws Exception {
        when(reservaService.buscarPorId(1L)).thenReturn(response);

        mockMvc.perform(get("/api/reservas/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/reservas/{id}: 404 quando não existe")
    void buscarPorId_inexistente() throws Exception {
        when(reservaService.buscarPorId(99L))
                .thenThrow(new RecursoNaoEncontradoException("Reserva não encontrada com o ID: 99"));

        mockMvc.perform(get("/api/reservas/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/reservas/abc: id não numérico vira 400")
    void buscarPorId_idNaoNumerico() throws Exception {
        mockMvc.perform(get("/api/reservas/abc"))
                .andExpect(status().isBadRequest());

        verify(reservaService, never()).buscarPorId(any());
    }

    // ---------------------------------------------------------------- POST

    @Test
    @DisplayName("POST /api/reservas: 201 e o corpo chega ao service")
    void criar() throws Exception {
        when(reservaService.salvar(any(ReservaRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_RESERVA))
                .andExpect(status().isCreated());

        ArgumentCaptor<ReservaRequest> captor = ArgumentCaptor.forClass(ReservaRequest.class);
        verify(reservaService).salvar(captor.capture());
        // Quando enviar o ReservaRequest, troque por asserts nos campos (ex.: codigoUsuario()).
        assertThat(captor.getValue()).isNotNull();
    }

    @Test
    @DisplayName("POST /api/reservas: usuário ou item inexistente vira 404")
    void criar_recursoInexistente() throws Exception {
        when(reservaService.salvar(any(ReservaRequest.class)))
                .thenThrow(new RecursoNaoEncontradoException("Usuário não encontrado com o ID: 99"));

        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_RESERVA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/reservas: IllegalArgumentException do service vira 400")
    void criar_dadosInvalidos() throws Exception {
        when(reservaService.salvar(any(ReservaRequest.class)))
                .thenThrow(new IllegalArgumentException("Dados inválidos."));

        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_RESERVA))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/reservas: JSON malformado vira 400 e o service nem é chamado")
    void criar_jsonMalformado() throws Exception {
        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{codigoUsuario: isto não é json"))
                .andExpect(status().isBadRequest());

        verify(reservaService, never()).salvar(any());
    }

    @Test
    @DisplayName("POST /api/reservas: Content-Type que não é JSON vira 415")
    void criar_semContentType() throws Exception {
        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("qualquer coisa"))
                .andExpect(status().isUnsupportedMediaType());
    }

    // ----------------------------------------------------------------- PUT

    @Test
    @DisplayName("PUT /api/reservas/{id}: 200 e repassa o id da URL ao service")
    void atualizar() throws Exception {
        when(reservaService.atualizar(eq(5L), any(ReservaRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/reservas/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_RESERVA))
                .andExpect(status().isOk());

        verify(reservaService).atualizar(eq(5L), any(ReservaRequest.class));
    }

    @Test
    @DisplayName("PUT /api/reservas/{id}: 404 quando a reserva não existe")
    void atualizar_inexistente() throws Exception {
        when(reservaService.atualizar(eq(99L), any(ReservaRequest.class)))
                .thenThrow(new RecursoNaoEncontradoException("Reserva não encontrada com o ID: 99"));

        mockMvc.perform(put("/api/reservas/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_RESERVA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/reservas/{id}: regra recusada pelo service (IllegalArgumentException) vira 400")
    void atualizar_regraRecusada() throws Exception {
        when(reservaService.atualizar(eq(5L), any(ReservaRequest.class)))
                .thenThrow(new IllegalArgumentException("Reserva não está ativa."));

        mockMvc.perform(put("/api/reservas/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_RESERVA))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------- DELETE

    @Test
    @DisplayName("DELETE /api/reservas/{id}: 204 sem corpo")
    void deletar() throws Exception {
        mockMvc.perform(delete("/api/reservas/5"))
                .andExpect(status().isNoContent());

        verify(reservaService).deletar(5L);
    }

    @Test
    @DisplayName("DELETE /api/reservas/{id}: 404 quando não existe")
    void deletar_inexistente() throws Exception {
        doThrow(new RecursoNaoEncontradoException("Reserva não encontrada com o ID: 99"))
                .when(reservaService).deletar(99L);

        mockMvc.perform(delete("/api/reservas/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/reservas/{id}: violação de integridade vira 409")
    void deletar_emUso() throws Exception {
        doThrow(new DataIntegrityViolationException("fk"))
                .when(reservaService).deletar(5L);

        mockMvc.perform(delete("/api/reservas/5"))
                .andExpect(status().isConflict());
    }
}