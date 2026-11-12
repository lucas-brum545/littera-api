package com.littera.api.service;

import com.littera.api.dto.LoginRequest;
import com.littera.api.dto.UsuarioResumo;
import com.littera.api.exception.RecursoNaoEncontradoException;
import com.littera.api.model.Usuario;
import com.littera.api.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private Usuario usuarioMock;

    @BeforeEach
    void setUp() {
        // Criando um usuário de teste genérico para os cenários
        usuarioMock = new Usuario();
        usuarioMock.setId(1L);
        usuarioMock.setNome("Lucas Brum");
        usuarioMock.setEmail("lucas@littera.com");
        usuarioMock.setSenhaHash("$2a$10$hashedPasswordExample");
        usuarioMock.setDataNascimento(LocalDate.of(1995, 5, 19));
        usuarioMock.setTelefone("51999999999");
        usuarioMock.setBloqueado(false);
    }

    @Test
    @DisplayName("Deve carregar o usuário com sucesso pelo username (email)")
    void loadUserByUsername_Success() {
        when(usuarioRepository.findByEmail("lucas@littera.com")).thenReturn(Optional.of(usuarioMock));

        UserDetails userDetails = authService.loadUserByUsername("lucas@littera.com");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("lucas@littera.com");
        verify(usuarioRepository, times(1)).findByEmail("lucas@littera.com");
    }

    @Test
    @DisplayName("Deve lançar UsernameNotFoundException quando o usuário não for encontrado")
    void loadUserByUsername_NotFound() {
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.loadUserByUsername("inexistente@littera.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Usuário não encontrado");
    }

    @Test
    @DisplayName("Deve autenticar com sucesso e retornar UsuarioResumo quando credenciais forem válidas")
    void autenticar_Success() {
        LoginRequest request = new LoginRequest("lucas@littera.com", "senha123");

        when(usuarioRepository.findByEmail("lucas@littera.com")).thenReturn(Optional.of(usuarioMock));
        when(passwordEncoder.matches("senha123", usuarioMock.getSenhaHash())).thenReturn(true);

        UsuarioResumo resultado = authService.autenticar(request);

        assertThat(resultado).isNotNull();
        assertThat(resultado.email()).isEqualTo("lucas@littera.com");
        verify(passwordEncoder, times(1)).matches("senha123", usuarioMock.getSenhaHash());
    }

    @Test
    @DisplayName("Deve lançar RecursoNaoEncontradoException quando o e-mail não existir na autenticação")
    void autenticar_EmailNotFound() {
        LoginRequest request = new LoginRequest("errado@littera.com", "senha123");

        when(usuarioRepository.findByEmail("errado@littera.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.autenticar(request))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("E-mail ou senha inválidos.");

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("Deve lançar RuntimeException quando a senha informada for inválida")
    void autenticar_InvalidPassword() {
        LoginRequest request = new LoginRequest("lucas@littera.com", "senhaErrada");

        when(usuarioRepository.findByEmail("lucas@littera.com")).thenReturn(Optional.of(usuarioMock));
        when(passwordEncoder.matches("senhaErrada", usuarioMock.getSenhaHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.autenticar(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("E-mail ou senha inválidos.");
    }
}