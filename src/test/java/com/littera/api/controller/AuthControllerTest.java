package com.littera.api.controller;

import com.littera.api.config.SecurityConfig;
import com.littera.api.dto.RegistroRequest;
import com.littera.api.dto.UsuarioResponse;
import com.littera.api.model.TipoUsuario;
import com.littera.api.model.Usuario;
import com.littera.api.repository.UsuarioRepository;
import com.littera.api.service.AuthService;
import com.littera.api.service.TokenService;
import com.littera.api.service.UsuarioService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@DisplayName("AuthController - Testes de Autenticacao")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private UsuarioService usuarioService;
    @MockitoBean private AuthenticationManager authenticationManager;
    @MockitoBean private UsuarioRepository usuarioRepository;
    @MockitoBean private TokenService tokenService;
    @MockitoBean private PasswordEncoder passwordEncoder;
    @MockitoBean private AuthService authService;
    @MockitoBean private com.littera.api.config.SecurityFilter securityFilter;

    @BeforeEach
    void configurarSecurityFilterPassthrough() throws Exception {
        doAnswer(invocation -> {
            HttpServletRequest req = invocation.getArgument(0);
            HttpServletResponse res = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(securityFilter).doFilter(any(), any(), any());
    }

    // =========================================================================
    // REGISTRO DE USUARIO
    // =========================================================================
    @Nested
    @DisplayName("POST /api/auth/register")
    class Register {

        @Test
        @DisplayName("Deve registrar usuario com sucesso e retornar 201 Created")
        void deveRegistrarUsuarioComSucesso() throws Exception {
            when(usuarioService.registrarUsuario(any(RegistroRequest.class)))
                    .thenReturn(new UsuarioResponse(1L, "Teste Automacao", "teste.auto@email.com"));

            String json = """
                {
                    "nome": "Teste Automacao",
                    "email": "teste.auto@email.com",
                    "senha": "senhaSegura123",
                    "telefone": "51999998888",
                    "dataNascimento": "1990-01-01"
                }
            """;

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.nome").value("Teste Automacao"))
                    .andExpect(jsonPath("$.email").value("teste.auto@email.com"));
        }

        @Test
        @DisplayName("Deve retornar 400 ao tentar registrar sem corpo")
        void deveRetornar400SemCorpo() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().is4xxClientError());
        }
    }

    // =========================================================================
    // LOGIN
    // =========================================================================
    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("Login com credenciais validas deve retornar 200 OK e token JWT")
        void loginComCredenciaisValidasDeveRetornar200() throws Exception {
            Usuario usuarioMock = mock(Usuario.class);
            when(usuarioMock.getId()).thenReturn(1L);
            when(usuarioMock.getNome()).thenReturn("Admin Littera");
            when(usuarioMock.getEmail()).thenReturn("admin@littera.com");
            when(usuarioMock.getTipoUsuario()).thenReturn(TipoUsuario.ADMIN);

            UsernamePasswordAuthenticationToken authResult =
                    new UsernamePasswordAuthenticationToken(usuarioMock, null, List.of());
            when(authenticationManager.authenticate(any())).thenReturn(authResult);
            when(tokenService.gerarToken(any(Usuario.class))).thenReturn("mock-jwt-token-12345");

            String json = """
                {
                    "email": "admin@littera.com",
                    "senha": "senhaSegura123"
                }
            """;

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("mock-jwt-token-12345"))
                    .andExpect(jsonPath("$.nome").value("Admin Littera"))
                    .andExpect(jsonPath("$.email").value("admin@littera.com"))
                    .andExpect(jsonPath("$.tipoUsuario").value("ADMIN"));
        }

        @Test
        @DisplayName("Login com senha errada deve retornar 401 Unauthorized")
        void loginComSenhaErradaDeveRetornar401() throws Exception {
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Credenciais invalidas"));

            String json = """
                {
                    "email": "admin@littera.com",
                    "senha": "senhaErrada"
                }
            """;

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.erro").value("E-mail ou senha inválidos."));
        }

        @Test
        @DisplayName("Login com email inexistente deve retornar 401 Unauthorized")
        void loginComEmailInexistenteDeveRetornar401() throws Exception {
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Usuario nao encontrado"));

            String json = """
                {
                    "email": "naoexiste@littera.com",
                    "senha": "qualquerSenha"
                }
            """;

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isUnauthorized());
        }
    }
}
