package com.littera.api.controller;

import com.littera.api.dto.EmprestimoRequest;
import com.littera.api.dto.EmprestimoResponse;
import com.littera.api.dto.EmprestimoResumo;
import com.littera.api.exception.RecursoNaoEncontradoException;
import com.littera.api.model.Emprestimo;
import com.littera.api.model.Livro;
import com.littera.api.model.TipoItemAcervo;
import com.littera.api.model.TipoUsuario;
import com.littera.api.model.Usuario;
import com.littera.api.repository.UsuarioRepository;
import com.littera.api.service.EmprestimoService;
import com.littera.api.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
 * Teste de "fatia" web: sobe só o controller, o @ControllerAdvice e o MockMvc.
 * O service é um mock; aqui se testa rota, status HTTP, JSON de entrada e tratamento de erro.
 */
@WebMvcTest(EmprestimoController.class)
@AutoConfigureMockMvc(addFilters = false) // desliga Spring Security/filtros nesta fatia
class EmprestimoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmprestimoService emprestimoService;

    // O SecurityFilter (@Component) entra na fatia web e precisa destas duas dependências.
    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private Emprestimo emprestimo;
    private EmprestimoResponse response;
    private EmprestimoResumo resumo;

    @BeforeEach
    void setUp() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("Lucas Brum");
        usuario.setEmail("lucas@teste.com");
        usuario.setTipoUsuario(TipoUsuario.LEITOR);

        Livro livro = new Livro();
        livro.setCodigo(10L);
        livro.setTitulo("Clean Code");
        livro.setTipo(TipoItemAcervo.LIVRO);
        livro.setAutor("Robert C. Martin");

        emprestimo = new Emprestimo();
        emprestimo.setCodigo(100L);
        emprestimo.setUsuario(usuario);
        emprestimo.setItemAcervo(livro);
        emprestimo.setDataEmprestimo(LocalDate.of(2026, 11, 8));
        emprestimo.setDataDevolucaoPrevista(LocalDate.of(2026, 11, 22));

        response = EmprestimoResponse.fromEntity(emprestimo);
        resumo = EmprestimoResumo.fromEntity(emprestimo);
    }

    // ----------------------------------------------------------------- GET

    @Test
    @DisplayName("GET /api/emprestimos: 200 com a lista")
    void listarTodos() throws Exception {
        when(emprestimoService.listarTodos()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/emprestimos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/emprestimos: 200 com lista vazia")
    void listarTodos_vazio() throws Exception {
        when(emprestimoService.listarTodos()).thenReturn(List.of());

        mockMvc.perform(get("/api/emprestimos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/emprestimos/{codigo}: 200 quando existe")
    void buscarId() throws Exception {
        when(emprestimoService.buscarId(100L)).thenReturn(response);

        mockMvc.perform(get("/api/emprestimos/100"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/emprestimos/{codigo}: 404 quando não existe")
    void buscarId_inexistente() throws Exception {
        when(emprestimoService.buscarId(999L))
                .thenThrow(new RecursoNaoEncontradoException("Empréstimo não encontrado com o código: 999"));

        mockMvc.perform(get("/api/emprestimos/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/emprestimos/{id}/resumo: 200 quando existe")
    void buscarPorId_resumo() throws Exception {
        when(emprestimoService.buscarPorIdResumo(100L)).thenReturn(Optional.of(resumo));

        mockMvc.perform(get("/api/emprestimos/100/resumo"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/emprestimos/{id}/resumo: 404 quando o Optional vem vazio")
    void buscarPorId_resumoInexistente() throws Exception {
        when(emprestimoService.buscarPorIdResumo(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/emprestimos/999/resumo"))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- POST

    @Test
    @DisplayName("POST /api/emprestimos: 201 e o JSON chega ao service corretamente desserializado")
    void criar() throws Exception {
        when(emprestimoService.salvar(any(EmprestimoRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/emprestimos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigoUsuario":1,"codigoItemAcervo":10,"dataEmprestimo":"2026-11-08"}
                                """))
                .andExpect(status().isCreated());

        ArgumentCaptor<EmprestimoRequest> captor = ArgumentCaptor.forClass(EmprestimoRequest.class);
        verify(emprestimoService).salvar(captor.capture());
        assertThat(captor.getValue().codigoUsuario()).isEqualTo(1L);
        assertThat(captor.getValue().codigoItemAcervo()).isEqualTo(10L);
        assertThat(captor.getValue().dataEmprestimo()).isEqualTo(LocalDate.of(2026, 11, 8));
    }

    @Test
    @DisplayName("POST /api/emprestimos: usuário ou item inexistente vira 404")
    void criar_recursoInexistente() throws Exception {
        when(emprestimoService.salvar(any(EmprestimoRequest.class)))
                .thenThrow(new RecursoNaoEncontradoException("Usuário não encontrado com o ID: 99"));

        mockMvc.perform(post("/api/emprestimos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigoUsuario\":99,\"codigoItemAcervo\":10}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/emprestimos: IllegalArgumentException do service vira 400")
    void criar_dadosInvalidos() throws Exception {
        when(emprestimoService.salvar(any(EmprestimoRequest.class)))
                .thenThrow(new IllegalArgumentException("Dados inválidos."));

        mockMvc.perform(post("/api/emprestimos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigoUsuario\":1,\"codigoItemAcervo\":10}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/emprestimos: JSON malformado vira 400 e o service nem é chamado")
    void criar_jsonMalformado() throws Exception {
        mockMvc.perform(post("/api/emprestimos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{codigoUsuario: isto não é json"))
                .andExpect(status().isBadRequest());

        verify(emprestimoService, never()).salvar(any());
    }

    @Test
    @DisplayName("POST /api/emprestimos: sem Content-Type JSON vira 415")
    void criar_semContentType() throws Exception {
        mockMvc.perform(post("/api/emprestimos")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("qualquer coisa"))
                .andExpect(status().isUnsupportedMediaType());
    }

    // ----------------------------------------------------------------- PUT

    @Test
    @DisplayName("PUT /api/emprestimos/{codigo}: 200 e repassa o código e o corpo ao service")
    void atualizar() throws Exception {
        when(emprestimoService.atualizar(any(Long.class), any(EmprestimoRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/emprestimos/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigoUsuario":1,"codigoItemAcervo":10,"dataDevolucaoPrevista":"2026-11-20"}
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<EmprestimoRequest> captor = ArgumentCaptor.forClass(EmprestimoRequest.class);
        verify(emprestimoService).atualizar(any(Long.class), captor.capture());
        assertThat(captor.getValue().dataDevolucaoPrevista()).isEqualTo(LocalDate.of(2026, 11, 20));
    }

    @Test
    @DisplayName("PUT /api/emprestimos/{codigo}: 404 quando o empréstimo não existe")
    void atualizar_inexistente() throws Exception {
        when(emprestimoService.atualizar(any(Long.class), any(EmprestimoRequest.class)))
                .thenThrow(new RecursoNaoEncontradoException("Empréstimo não encontrado com o código: 999"));

        mockMvc.perform(put("/api/emprestimos/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigoUsuario\":1,\"codigoItemAcervo\":10}"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------- DELETE

    @Test
    @DisplayName("DELETE /api/emprestimos/{codigo}: 204 sem corpo")
    void deletar() throws Exception {
        mockMvc.perform(delete("/api/emprestimos/100"))
                .andExpect(status().isNoContent());

        verify(emprestimoService).deletar(100L);
    }

    @Test
    @DisplayName("DELETE /api/emprestimos/{codigo}: 404 quando não existe")
    void deletar_inexistente() throws Exception {
        doThrow(new RecursoNaoEncontradoException("Empréstimo não encontrado com o código: 999"))
                .when(emprestimoService).deletar(999L);

        mockMvc.perform(delete("/api/emprestimos/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/emprestimos/abc: código não numérico vira 400")
    void deletar_codigoNaoNumerico() throws Exception {
        mockMvc.perform(delete("/api/emprestimos/abc"))
                .andExpect(status().isBadRequest());

        verify(emprestimoService, never()).deletar(any());
    }
}