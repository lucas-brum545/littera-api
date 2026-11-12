package com.littera.api.controller;

import com.littera.api.dto.ItemAcervoRequest;
import com.littera.api.dto.ItemAcervoResponse;
import com.littera.api.exception.RecursoNaoEncontradoException;
import com.littera.api.model.Livro;
import com.littera.api.model.Revista;
import com.littera.api.model.TipoItemAcervo;
import com.littera.api.repository.UsuarioRepository;
import com.littera.api.service.ItemAcervoService;
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
@WebMvcTest(ItemAcervoController.class)
@AutoConfigureMockMvc(addFilters = false) // desliga Spring Security/filtros nesta fatia
class ItemAcervoControllerTest {

    private static final String JSON_LIVRO = """
            {
              "tipo": "LIVRO",
              "titulo": "Clean Code",
              "ano": 2008,
              "localizacao": "A1",
              "autor": "Robert C. Martin",
              "isbn": "978-0132350884",
              "genero": "Tecnologia",
              "totalDisponivel": 3
            }
            """;

    private static final String JSON_REVISTA = """
            {
              "tipo": "REVISTA",
              "titulo": "Superinteressante",
              "ano": 2024,
              "localizacao": "B2",
              "edicao": "450",
              "issn": "0104-1789",
              "mesAnoPublicacao": "05/2024"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemAcervoService itemAcervoService;

    // O SecurityFilter (@Component) entra na fatia web e precisa destas duas dependências.
    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private ItemAcervoResponse livroResponse;
    private ItemAcervoResponse revistaResponse;

    @BeforeEach
    void setUp() {
        Livro livro = new Livro();
        livro.setCodigo(1L);
        livro.setTitulo("Clean Code");
        livro.setAno(2008);
        livro.setTipo(TipoItemAcervo.LIVRO);
        livro.setLocalizacao("A1");
        livro.setAutor("Robert C. Martin");
        livro.setIsbn("978-0132350884");
        livro.setGenero("Tecnologia");

        Revista revista = new Revista();
        revista.setCodigo(2L);
        revista.setTitulo("Superinteressante");
        revista.setAno(2024);
        revista.setTipo(TipoItemAcervo.REVISTA);
        revista.setLocalizacao("B2");
        revista.setEdicao("450");
        revista.setIssn("0104-1789");
        revista.setMesAnoPublicacao("05/2024");

        livroResponse = new ItemAcervoResponse(livro);
        revistaResponse = new ItemAcervoResponse(revista);
    }

    // ----------------------------------------------------------------- GET

    @Test
    @DisplayName("GET /api/acervo: 200 com livros e revistas na mesma lista")
    void listar() throws Exception {
        when(itemAcervoService.buscarTodos()).thenReturn(List.of(livroResponse, revistaResponse));

        mockMvc.perform(get("/api/acervo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].tipo").value("LIVRO"))
                .andExpect(jsonPath("$[1].tipo").value("REVISTA"));
    }

    @Test
    @DisplayName("GET /api/acervo: 200 com lista vazia")
    void listar_vazio() throws Exception {
        when(itemAcervoService.buscarTodos()).thenReturn(List.of());

        mockMvc.perform(get("/api/acervo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/acervo/{id}: 200 com os campos que o front consome")
    void buscarPorId() throws Exception {
        when(itemAcervoService.buscarPorId(1L)).thenReturn(livroResponse);

        mockMvc.perform(get("/api/acervo/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(1))
                .andExpect(jsonPath("$.titulo").value("Clean Code"))
                .andExpect(jsonPath("$.tipo").value("LIVRO"))
                .andExpect(jsonPath("$.autor").value("Robert C. Martin"));
    }

    @Test
    @DisplayName("GET /api/acervo/{id}: 404 quando não existe")
    void buscarPorId_inexistente() throws Exception {
        when(itemAcervoService.buscarPorId(99L))
                .thenThrow(new RecursoNaoEncontradoException("Item de acervo não encontrado com o ID: 99"));

        mockMvc.perform(get("/api/acervo/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/acervo/abc: id não numérico vira 400")
    void buscarPorId_idNaoNumerico() throws Exception {
        mockMvc.perform(get("/api/acervo/abc"))
                .andExpect(status().isBadRequest());

        verify(itemAcervoService, never()).buscarPorId(any());
    }

    // ---------------------------------------------------------------- POST

    @Test
    @DisplayName("POST /api/acervo: livro retorna 201 e chega ao service desserializado")
    void criar_livro() throws Exception {
        when(itemAcervoService.salvar(any(ItemAcervoRequest.class))).thenReturn(livroResponse);

        mockMvc.perform(post("/api/acervo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_LIVRO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Clean Code"));

        ArgumentCaptor<ItemAcervoRequest> captor = ArgumentCaptor.forClass(ItemAcervoRequest.class);
        verify(itemAcervoService).salvar(captor.capture());
        ItemAcervoRequest enviado = captor.getValue();
        assertThat(enviado.tipo()).isEqualTo(TipoItemAcervo.LIVRO);
        assertThat(enviado.titulo()).isEqualTo("Clean Code");
        assertThat(enviado.ano()).isEqualTo(2008);
        assertThat(enviado.autor()).isEqualTo("Robert C. Martin");
        assertThat(enviado.isbn()).isEqualTo("978-0132350884");
        assertThat(enviado.genero()).isEqualTo("Tecnologia");
        assertThat(enviado.totalDisponivel()).isEqualTo(3);
    }

    @Test
    @DisplayName("POST /api/acervo: revista retorna 201 e leva os campos específicos")
    void criar_revista() throws Exception {
        when(itemAcervoService.salvar(any(ItemAcervoRequest.class))).thenReturn(revistaResponse);

        mockMvc.perform(post("/api/acervo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_REVISTA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("REVISTA"));

        ArgumentCaptor<ItemAcervoRequest> captor = ArgumentCaptor.forClass(ItemAcervoRequest.class);
        verify(itemAcervoService).salvar(captor.capture());
        assertThat(captor.getValue().tipo()).isEqualTo(TipoItemAcervo.REVISTA);
        assertThat(captor.getValue().edicao()).isEqualTo("450");
        assertThat(captor.getValue().issn()).isEqualTo("0104-1789");
        assertThat(captor.getValue().mesAnoPublicacao()).isEqualTo("05/2024");
    }

    @Test
    @DisplayName("POST /api/acervo: erro de validação do service (IllegalArgumentException) vira 400")
    void criar_validacao() throws Exception {
        when(itemAcervoService.salvar(any(ItemAcervoRequest.class)))
                .thenThrow(new IllegalArgumentException("O campo autor é obrigatório."));

        mockMvc.perform(post("/api/acervo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_LIVRO))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/acervo: JSON malformado vira 400 e o service nem é chamado")
    void criar_jsonMalformado() throws Exception {
        mockMvc.perform(post("/api/acervo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{titulo: isto não é json"))
                .andExpect(status().isBadRequest());

        verify(itemAcervoService, never()).salvar(any());
    }

    @Test
    @DisplayName("POST /api/acervo: tipo inexistente no enum vira 400")
    void criar_tipoInvalido() throws Exception {
        mockMvc.perform(post("/api/acervo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"GIBI\",\"titulo\":\"X\"}"))
                .andExpect(status().isBadRequest());

        verify(itemAcervoService, never()).salvar(any());
    }

    @Test
    @DisplayName("POST /api/acervo: Content-Type que não é JSON vira 415")
    void criar_semContentType() throws Exception {
        mockMvc.perform(post("/api/acervo")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("qualquer coisa"))
                .andExpect(status().isUnsupportedMediaType());
    }

    // ----------------------------------------------------------------- PUT

    @Test
    @DisplayName("PUT /api/acervo/{id}: 200 e repassa o id da URL e o corpo ao service")
    void atualizar() throws Exception {
        when(itemAcervoService.atualizar(eq(1L), any(ItemAcervoRequest.class))).thenReturn(livroResponse);

        mockMvc.perform(put("/api/acervo/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_LIVRO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(1));

        ArgumentCaptor<ItemAcervoRequest> captor = ArgumentCaptor.forClass(ItemAcervoRequest.class);
        verify(itemAcervoService).atualizar(eq(1L), captor.capture());
        assertThat(captor.getValue().titulo()).isEqualTo("Clean Code");
    }

    @Test
    @DisplayName("PUT /api/acervo/{id}: 404 quando o item não existe")
    void atualizar_inexistente() throws Exception {
        when(itemAcervoService.atualizar(eq(99L), any(ItemAcervoRequest.class)))
                .thenThrow(new RecursoNaoEncontradoException("Item de acervo não encontrado com o ID: 99"));

        mockMvc.perform(put("/api/acervo/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_LIVRO))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/acervo/{id}: troca de tipo recusada pelo service vira 400")
    void atualizar_tipoDiferente() throws Exception {
        when(itemAcervoService.atualizar(eq(1L), any(ItemAcervoRequest.class)))
                .thenThrow(new IllegalArgumentException("Não é permitido alterar o tipo de um item existente."));

        mockMvc.perform(put("/api/acervo/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_REVISTA))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------- DELETE

    @Test
    @DisplayName("DELETE /api/acervo/{id}: 204 sem corpo")
    void deletar() throws Exception {
        mockMvc.perform(delete("/api/acervo/1"))
                .andExpect(status().isNoContent());

        verify(itemAcervoService).deletar(1L);
    }

    @Test
    @DisplayName("DELETE /api/acervo/{id}: 404 quando o item não existe")
    void deletar_inexistente() throws Exception {
        doThrow(new RecursoNaoEncontradoException("Item de acervo não encontrado com o ID: 99"))
                .when(itemAcervoService).deletar(99L);

        mockMvc.perform(delete("/api/acervo/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/acervo/{id}: item com empréstimos ligados (violação de FK) vira 409")
    void deletar_itemEmUso() throws Exception {
        doThrow(new DataIntegrityViolationException("fk_emprestimo_item"))
                .when(itemAcervoService).deletar(1L);

        mockMvc.perform(delete("/api/acervo/1"))
                .andExpect(status().isConflict());
    }
}