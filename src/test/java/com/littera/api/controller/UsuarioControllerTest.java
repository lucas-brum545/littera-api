package com.littera.api.controller;

import com.littera.api.dto.UsuarioRequest;
import com.littera.api.dto.UsuarioResumo;
import com.littera.api.exception.RecursoNaoEncontradoException;
import com.littera.api.model.TipoUsuario;
import com.littera.api.model.Usuario;
import com.littera.api.repository.UsuarioRepository;
import com.littera.api.service.TokenService;
import com.littera.api.service.UsuarioService;
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

import java.time.LocalDate;
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
 * O service é um mock; aqui se testa rota, status HTTP, JSON de entrada/saída e erros.
 */
@WebMvcTest(UsuarioController.class)
@AutoConfigureMockMvc(addFilters = false) // desliga Spring Security/filtros nesta fatia
class UsuarioControllerTest {

    private static final String JSON_COMPLETO = """
            {
              "nome": "Lucas Brum",
              "email": "lucas@teste.com",
              "telefone": "51999991111",
              "dataNascimento": "1995-05-19",
              "senha": "12345678",
              "tipoUsuario": "ADMIN"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    // O SecurityFilter (@Component) entra na fatia web e precisa destas duas dependências.
    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private Usuario usuario;
    private UsuarioResumo resumo;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("Lucas Brum");
        usuario.setEmail("lucas@teste.com");
        usuario.setTelefone("51999991111");
        usuario.setDataNascimento(LocalDate.of(1995, 5, 19));
        usuario.setSenhaHash("$2a$10$hashqualquer");
        usuario.setTipoUsuario(TipoUsuario.ADMIN);
        usuario.setBloqueado(false);
        usuario.setQuantidadeEmprestada(0);

        resumo = UsuarioResumo.fromEntity(usuario);
    }

    // ----------------------------------------------------------------- GET

    @Test
    @DisplayName("GET /api/usuarios: 200 com a lista no formato que o front consome")
    void listarTodos() throws Exception {
        when(usuarioService.listarTodos()).thenReturn(List.of(resumo));

        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nome").value("Lucas Brum"))
                .andExpect(jsonPath("$[0].email").value("lucas@teste.com"))
                .andExpect(jsonPath("$[0].dataNascimento").value("1995-05-19"))
                .andExpect(jsonPath("$[0].tipoUsuario").value("ADMIN"))
                .andExpect(jsonPath("$[0].quantidadeEmprestada").value(0));
    }

    @Test
    @DisplayName("GET /api/usuarios: nunca expõe senha nem hash")
    void listarTodos_naoExpoeSenha() throws Exception {
        when(usuarioService.listarTodos()).thenReturn(List.of(resumo));

        mockMvc.perform(get("/api/usuarios"))
                .andExpect(jsonPath("$[0].senha").doesNotExist())
                .andExpect(jsonPath("$[0].senhaHash").doesNotExist())
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/usuarios: 200 com lista vazia")
    void listarTodos_vazio() throws Exception {
        when(usuarioService.listarTodos()).thenReturn(List.of());

        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/usuarios/{id}: 200 quando existe")
    void buscarPorId() throws Exception {
        when(usuarioService.buscarPorId(1L)).thenReturn(resumo);

        mockMvc.perform(get("/api/usuarios/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("lucas@teste.com"));
    }

    @Test
    @DisplayName("GET /api/usuarios/{id}: 404 quando não existe")
    void buscarPorId_inexistente() throws Exception {
        when(usuarioService.buscarPorId(99L))
                .thenThrow(new RecursoNaoEncontradoException("Usuário não encontrado com o ID: 99"));

        mockMvc.perform(get("/api/usuarios/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/usuarios/abc: id não numérico vira 400")
    void buscarPorId_idNaoNumerico() throws Exception {
        mockMvc.perform(get("/api/usuarios/abc"))
                .andExpect(status().isBadRequest());

        verify(usuarioService, never()).buscarPorId(any());
    }

    // ---------------------------------------------------------------- POST

    @Test
    @DisplayName("POST /api/usuarios: 201, JSON desserializado corretamente e resposta sem senha")
    void criar() throws Exception {
        when(usuarioService.salvar(any(UsuarioRequest.class))).thenReturn(usuario);

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_COMPLETO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Lucas Brum"))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist());

        ArgumentCaptor<UsuarioRequest> captor = ArgumentCaptor.forClass(UsuarioRequest.class);
        verify(usuarioService).salvar(captor.capture());
        UsuarioRequest enviado = captor.getValue();
        assertThat(enviado.nome()).isEqualTo("Lucas Brum");
        assertThat(enviado.email()).isEqualTo("lucas@teste.com");
        assertThat(enviado.telefone()).isEqualTo("51999991111");
        assertThat(enviado.dataNascimento()).isEqualTo(LocalDate.of(1995, 5, 19));
        assertThat(enviado.senha()).isEqualTo("12345678");
        assertThat(enviado.tipoUsuario()).isEqualTo(TipoUsuario.ADMIN);
    }

    @Test
    @DisplayName("POST /api/usuarios: sem tipoUsuario no JSON o service recebe null (padrão LEITOR é do service)")
    void criar_semTipo() throws Exception {
        when(usuarioService.salvar(any(UsuarioRequest.class))).thenReturn(usuario);

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Maria","email":"maria@teste.com","telefone":"5133334444",
                                 "dataNascimento":"2000-01-01","senha":"12345678"}
                                """))
                .andExpect(status().isCreated());

        ArgumentCaptor<UsuarioRequest> captor = ArgumentCaptor.forClass(UsuarioRequest.class);
        verify(usuarioService).salvar(captor.capture());
        assertThat(captor.getValue().tipoUsuario()).isNull();
    }

    @Test
    @DisplayName("POST /api/usuarios: e-mail duplicado ou dado inválido (IllegalArgumentException) vira 400")
    void criar_dadosInvalidos() throws Exception {
        when(usuarioService.salvar(any(UsuarioRequest.class)))
                .thenThrow(new IllegalArgumentException("Já existe um usuário com este e-mail."));

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_COMPLETO))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/usuarios: JSON malformado vira 400 e o service nem é chamado")
    void criar_jsonMalformado() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{nome: isto não é json"))
                .andExpect(status().isBadRequest());

        verify(usuarioService, never()).salvar(any());
    }

    @Test
    @DisplayName("POST /api/usuarios: tipoUsuario fora do enum vira 400")
    void criar_tipoInvalido() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"X","email":"x@teste.com","tipoUsuario":"SUPERADMIN"}
                                """))
                .andExpect(status().isBadRequest());

        verify(usuarioService, never()).salvar(any());
    }

    @Test
    @DisplayName("POST /api/usuarios: Content-Type que não é JSON vira 415")
    void criar_semContentType() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("qualquer coisa"))
                .andExpect(status().isUnsupportedMediaType());
    }

    // ----------------------------------------------------------------- PUT

    @Test
    @DisplayName("PUT /api/usuarios/{id}: 200 e repassa o id da URL e o corpo ao service")
    void atualizar() throws Exception {
        when(usuarioService.atualizar(eq(1L), any(UsuarioRequest.class))).thenReturn(resumo);

        mockMvc.perform(put("/api/usuarios/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_COMPLETO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.senhaHash").doesNotExist());

        ArgumentCaptor<UsuarioRequest> captor = ArgumentCaptor.forClass(UsuarioRequest.class);
        verify(usuarioService).atualizar(eq(1L), captor.capture());
        assertThat(captor.getValue().email()).isEqualTo("lucas@teste.com");
    }

    @Test
    @DisplayName("PUT /api/usuarios/{id}: atualização parcial chega com os demais campos nulos")
    void atualizar_parcial() throws Exception {
        when(usuarioService.atualizar(eq(1L), any(UsuarioRequest.class))).thenReturn(resumo);

        mockMvc.perform(put("/api/usuarios/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Novo Nome\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<UsuarioRequest> captor = ArgumentCaptor.forClass(UsuarioRequest.class);
        verify(usuarioService).atualizar(eq(1L), captor.capture());
        assertThat(captor.getValue().nome()).isEqualTo("Novo Nome");
        assertThat(captor.getValue().email()).isNull();
        assertThat(captor.getValue().senha()).isNull();
        assertThat(captor.getValue().tipoUsuario()).isNull();
    }

    @Test
    @DisplayName("PUT /api/usuarios/{id}: 404 quando o usuário não existe")
    void atualizar_inexistente() throws Exception {
        when(usuarioService.atualizar(eq(99L), any(UsuarioRequest.class)))
                .thenThrow(new RecursoNaoEncontradoException("Usuário não encontrado com o ID: 99"));

        mockMvc.perform(put("/api/usuarios/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_COMPLETO))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/usuarios/{id}: e-mail de outro usuário (IllegalArgumentException) vira 400")
    void atualizar_emailDuplicado() throws Exception {
        when(usuarioService.atualizar(eq(1L), any(UsuarioRequest.class)))
                .thenThrow(new IllegalArgumentException("Já existe um usuário com este e-mail."));

        mockMvc.perform(put("/api/usuarios/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_COMPLETO))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------- DELETE

    @Test
    @DisplayName("DELETE /api/usuarios/{id}: 204 sem corpo")
    void deletar() throws Exception {
        mockMvc.perform(delete("/api/usuarios/1"))
                .andExpect(status().isNoContent());

        verify(usuarioService).deletar(1L);
    }

    @Test
    @DisplayName("DELETE /api/usuarios/{id}: 404 quando não existe")
    void deletar_inexistente() throws Exception {
        doThrow(new RecursoNaoEncontradoException("Usuário não encontrado com o ID: 99"))
                .when(usuarioService).deletar(99L);

        mockMvc.perform(delete("/api/usuarios/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/usuarios/{id}: usuário com empréstimos ligados (violação de FK) vira 409")
    void deletar_usuarioEmUso() throws Exception {
        doThrow(new DataIntegrityViolationException("fk_emprestimo_usuario"))
                .when(usuarioService).deletar(1L);

        mockMvc.perform(delete("/api/usuarios/1"))
                .andExpect(status().isConflict());
    }
}