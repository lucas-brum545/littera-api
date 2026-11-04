package com.littera.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class DeepLTranslationService {

    private static final String DEEPL_URL = "https://api-free.deepl.com/v2/translate"; // ou api.deepl.com para Pro[cite: 7, 10]

    private final RestClient restClient;
    private final ObjectMapper objectMapper; // ou Jackson2ObjectMapperBuilder se preferir

    @Value("${DEEPL_API_KEY:}")
    private String apiKey;


    public DeepLTranslationService(RestClient.Builder builder) {
        this.restClient = builder.build();
        this.objectMapper = com.fasterxml.jackson.databind.json.JsonMapper.builder().build();
    }

    public String traduzir(String textoParaTraduzir) {
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("DEEPL_API_KEY não está configurada.");
            return textoParaTraduzir;
        }

        try {
            // Corpo da requisição para a DeepL
            Map<String, Object> corpoRequisicao = Map.of(
                    "text", List.of(textoParaTraduzir),
                    "target_lang", "PT"
            );

            // Faz a requisição recebendo o resultado como String pura
            String respostaJson = restClient.post()
                    .uri(DEEPL_URL)
                    .header("Authorization", "DeepL-Auth-Key " + apiKey) //[cite: 4]
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corpoRequisicao)
                    .retrieve()
                    .body(String.class);

            // Converte a string JSON com o ObjectMapper de forma segura
            JsonNode root = objectMapper.readTree(respostaJson);
            JsonNode traducoes = root.path("translations");

            if (traducoes.isArray() && !traducoes.isEmpty()) {
                return traducoes.get(0).path("text").asText(textoParaTraduzir);
            }

            return textoParaTraduzir;

        } catch (Exception e) {
            System.err.println("Falha ao traduzir descrição com DeepL: " + e.getMessage());
            return textoParaTraduzir; // Retorna o texto original em caso de falha para não quebrar a aplicação
        }
    }


}