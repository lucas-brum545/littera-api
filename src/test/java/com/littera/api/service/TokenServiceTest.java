package com.littera.api.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.littera.api.model.TipoUsuario;
import com.littera.api.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Teste unitário puro: não sobe o Spring. O @Value do segredo é preenchido
 * por reflexão, já que não há contexto para injetá-lo.
 */
class TokenServiceTest {

    private static final String SEGREDO = "segredo-de-teste-com-tamanho-razoavel";
    private static final String EMAIL = "lucas@teste.com";

    private TokenService tokenService;
    private Usuario admin;

    @BeforeEach
    void setUp() {
        tokenService = servicoComSegredo(SEGREDO);

        admin = new Usuario();
        admin.setId(1L);
        admin.setEmail(EMAIL);
        admin.setTipoUsuario(TipoUsuario.ADMIN);
    }

    private TokenService servicoComSegredo(String segredo) {
        TokenService service = new TokenService();
        ReflectionTestUtils.setField(service, "secret", segredo);
        return service;
    }

    private String base64Url(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------ gerarToken

    @Test
    @DisplayName("gerarToken: devolve um JWT com três partes")
    void gerarToken_formato() {
        String token = tokenService.gerarToken(admin);

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("gerarToken: carrega emissor, e-mail, id e tipo do usuário")
    void gerarToken_conteudo() {
        DecodedJWT jwt = JWT.decode(tokenService.gerarToken(admin));

        assertThat(jwt.getIssuer()).isEqualTo("littera-api");
        assertThat(jwt.getSubject()).isEqualTo(EMAIL);
        assertThat(jwt.getClaim("id").asLong()).isEqualTo(1L);
        assertThat(jwt.getClaim("tipoUsuario").asString()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("gerarToken: expira em cerca de 8 horas")
    void gerarToken_expiracao() {
        Instant antes = Instant.now();
        DecodedJWT jwt = JWT.decode(tokenService.gerarToken(admin));

        Duration validade = Duration.between(antes, jwt.getExpiresAt().toInstant());

        assertThat(validade).isBetween(Duration.ofHours(8).minusMinutes(1), Duration.ofHours(8).plusMinutes(1));
    }

    @Test
    @DisplayName("gerarToken: usuário LEITOR recebe o tipo LEITOR no token")
    void gerarToken_leitor() {
        admin.setTipoUsuario(TipoUsuario.LEITOR);

        DecodedJWT jwt = JWT.decode(tokenService.gerarToken(admin));

        assertThat(jwt.getClaim("tipoUsuario").asString()).isEqualTo("LEITOR");
    }

    @Test
    @DisplayName("gerarToken: usuário sem tipo lança NullPointerException (comportamento atual)")
    void gerarToken_semTipo() {
        admin.setTipoUsuario(null);

        assertThatThrownBy(() -> tokenService.gerarToken(admin))
                .isInstanceOf(NullPointerException.class);
    }

    // ---------------------------------------------------------- validarToken

    @Test
    @DisplayName("validarToken: token válido devolve o e-mail do usuário")
    void validarToken_valido() {
        String token = tokenService.gerarToken(admin);

        assertThat(tokenService.validarToken(token)).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("validarToken: ida e volta funciona para cada usuário")
    void validarToken_idaEVolta() {
        Usuario outro = new Usuario();
        outro.setId(2L);
        outro.setEmail("maria@teste.com");
        outro.setTipoUsuario(TipoUsuario.LEITOR);

        assertThat(tokenService.validarToken(tokenService.gerarToken(admin))).isEqualTo(EMAIL);
        assertThat(tokenService.validarToken(tokenService.gerarToken(outro))).isEqualTo("maria@teste.com");
    }

    @Test
    @DisplayName("validarToken: token assinado com outro segredo é recusado")
    void validarToken_outroSegredo() {
        String tokenDeOutroServidor = servicoComSegredo("segredo-diferente").gerarToken(admin);

        assertThat(tokenService.validarToken(tokenDeOutroServidor)).isEmpty();
    }

    @Test
    @DisplayName("validarToken: token expirado é recusado")
    void validarToken_expirado() {
        String expirado = JWT.create()
                .withIssuer("littera-api")
                .withSubject(EMAIL)
                .withExpiresAt(Instant.now().minusSeconds(60))
                .sign(Algorithm.HMAC256(SEGREDO));

        assertThat(tokenService.validarToken(expirado)).isEmpty();
    }

    @Test
    @DisplayName("validarToken: token com emissor diferente é recusado")
    void validarToken_emissorErrado() {
        String emissorErrado = JWT.create()
                .withIssuer("outra-api")
                .withSubject(EMAIL)
                .withExpiresAt(Instant.now().plusSeconds(3600))
                .sign(Algorithm.HMAC256(SEGREDO));

        assertThat(tokenService.validarToken(emissorErrado)).isEmpty();
    }

    @Test
    @DisplayName("validarToken: payload alterado com a assinatura original é recusado")
    void validarToken_payloadAdulterado() {
        String[] partes = tokenService.gerarToken(admin).split("\\.");
        String payloadFalso = base64Url(
                "{\"iss\":\"littera-api\",\"sub\":\"invasor@teste.com\",\"exp\":"
                        + Instant.now().plusSeconds(3600).getEpochSecond() + "}");
        String adulterado = partes[0] + "." + payloadFalso + "." + partes[2];

        assertThat(tokenService.validarToken(adulterado)).isEmpty();
    }

    @Test
    @DisplayName("validarToken: textos que não são JWT são recusados sem lançar exceção")
    void validarToken_lixo() {
        for (String lixo : new String[]{"", "   ", "abc", "a.b.c", "Bearer xyz", "...."}) {
            assertThat(tokenService.validarToken(lixo))
                    .as("token: '%s'", lixo)
                    .isEmpty();
        }
    }
}