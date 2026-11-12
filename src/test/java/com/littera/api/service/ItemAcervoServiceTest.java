package com.littera.api.service;

import com.littera.api.dto.ItemAcervoRequest;
import com.littera.api.dto.ItemAcervoResponse;
import com.littera.api.exception.RecursoNaoEncontradoException;
import com.littera.api.model.ItemAcervo;
import com.littera.api.model.Livro;
import com.littera.api.model.Revista;
import com.littera.api.model.TipoItemAcervo;
import com.littera.api.repository.ItemAcervoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemAcervoServiceTest {

    @Mock
    private ItemAcervoRepository itemAcervoRepository;

    // O service usa @Autowired em campo; o Mockito injeta o mock por campo.
    @InjectMocks
    private ItemAcervoService itemAcervoService;

    private Livro livroExistente;
    private Revista revistaExistente;

    @BeforeEach
    void setUp() {
        livroExistente = new Livro();
        livroExistente.setCodigo(1L);
        livroExistente.setTitulo("Clean Code");
        livroExistente.setTipo(TipoItemAcervo.LIVRO);
        livroExistente.setAutor("Robert C. Martin");

        revistaExistente = new Revista();
        revistaExistente.setCodigo(2L);
        revistaExistente.setTitulo("Superinteressante");
        revistaExistente.setTipo(TipoItemAcervo.REVISTA);
        revistaExistente.setEdicao("400");
    }

    /**
     * Cria um request "falso" (mock do record) com valores padrão. Assim os testes
     * dependem só dos nomes dos métodos de acesso, e não da ordem dos componentes
     * do record, que muda com frequência.
     * Atenção: wrappers sem stub retornariam 0 no Mockito, por isso null é explícito.
     */
    private ItemAcervoRequest request(TipoItemAcervo tipo) {
        ItemAcervoRequest r = mock(ItemAcervoRequest.class);
        lenient().when(r.tipo()).thenReturn(tipo);
        lenient().when(r.titulo()).thenReturn("Novo título");
        lenient().when(r.ano()).thenReturn(2020);
        lenient().when(r.localizacao()).thenReturn("A1");
        lenient().when(r.idioma()).thenReturn("Português");
        lenient().when(r.capa()).thenReturn(null);
        lenient().when(r.descricao()).thenReturn("Descrição");
        lenient().when(r.cadastradoEm()).thenReturn(null);
        lenient().when(r.totalDisponivel()).thenReturn(null);
        lenient().when(r.autor()).thenReturn("Autor X");
        lenient().when(r.isbn()).thenReturn("978-0000000000");
        lenient().when(r.genero()).thenReturn("Tecnologia");
        lenient().when(r.edicao()).thenReturn("12");
        lenient().when(r.issn()).thenReturn("1234-5678");
        lenient().when(r.mesAnoPublicacao()).thenReturn("05/2024");
        return r;
    }

    // ---------------------------------------------------------------- salvar

    @Test
    @DisplayName("salvar: cria Livro com campos comuns e específicos")
    void salvar_livro() {
        when(itemAcervoRepository.save(any(Livro.class))).thenAnswer(inv -> inv.getArgument(0));

        ItemAcervoResponse resposta = itemAcervoService.salvar(request(TipoItemAcervo.LIVRO));

        assertThat(resposta).isNotNull();
        ArgumentCaptor<Livro> captor = ArgumentCaptor.forClass(Livro.class);
        verify(itemAcervoRepository).save(captor.capture());
        Livro salvo = captor.getValue();
        assertThat(salvo.getTitulo()).isEqualTo("Novo título");
        assertThat(salvo.getAno()).isEqualTo(2020);
        assertThat(salvo.getLocalizacao()).isEqualTo("A1");
        assertThat(salvo.getTipo()).isEqualTo(TipoItemAcervo.LIVRO);
        assertThat(salvo.getAutor()).isEqualTo("Autor X");
        assertThat(salvo.getIsbn()).isEqualTo("978-0000000000");
        assertThat(salvo.getGenero()).isEqualTo("Tecnologia");
    }

    @Test
    @DisplayName("salvar: cria Revista com campos comuns e específicos")
    void salvar_revista() {
        when(itemAcervoRepository.save(any(Revista.class))).thenAnswer(inv -> inv.getArgument(0));

        ItemAcervoResponse resposta = itemAcervoService.salvar(request(TipoItemAcervo.REVISTA));

        assertThat(resposta).isNotNull();
        ArgumentCaptor<Revista> captor = ArgumentCaptor.forClass(Revista.class);
        verify(itemAcervoRepository).save(captor.capture());
        Revista salva = captor.getValue();
        assertThat(salva.getTipo()).isEqualTo(TipoItemAcervo.REVISTA);
        assertThat(salva.getEdicao()).isEqualTo("12");
        assertThat(salva.getIssn()).isEqualTo("1234-5678");
        assertThat(salva.getMesAnoPublicacao()).isEqualTo("05/2024");
    }

    @Test
    @DisplayName("salvar: sem cadastradoEm no request, usa a data de hoje")
    void salvar_semCadastradoEm_usaHoje() {
        when(itemAcervoRepository.save(any(Livro.class))).thenAnswer(inv -> inv.getArgument(0));

        itemAcervoService.salvar(request(TipoItemAcervo.LIVRO));

        ArgumentCaptor<Livro> captor = ArgumentCaptor.forClass(Livro.class);
        verify(itemAcervoRepository).save(captor.capture());
        assertThat(captor.getValue().getCadastradoEm()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("salvar: cadastradoEm informado no request é respeitado")
    void salvar_comCadastradoEm() {
        LocalDate data = LocalDate.of(2024, 1, 15);
        ItemAcervoRequest req = request(TipoItemAcervo.LIVRO);
        lenient().when(req.cadastradoEm()).thenReturn(data);
        when(itemAcervoRepository.save(any(Livro.class))).thenAnswer(inv -> inv.getArgument(0));

        itemAcervoService.salvar(req);

        ArgumentCaptor<Livro> captor = ArgumentCaptor.forClass(Livro.class);
        verify(itemAcervoRepository).save(captor.capture());
        assertThat(captor.getValue().getCadastradoEm()).isEqualTo(data);
    }

    @Test
    @DisplayName("salvar: totalDisponivel informado vira quantidade disponível")
    void salvar_comTotalDisponivel() {
        ItemAcervoRequest req = request(TipoItemAcervo.LIVRO);
        lenient().when(req.totalDisponivel()).thenReturn(5);
        when(itemAcervoRepository.save(any(Livro.class))).thenAnswer(inv -> inv.getArgument(0));

        itemAcervoService.salvar(req);

        ArgumentCaptor<Livro> captor = ArgumentCaptor.forClass(Livro.class);
        verify(itemAcervoRepository).save(captor.capture());
        assertThat(captor.getValue().getQuantidadeDisponivel()).isEqualTo(5);
    }

    @Test
    @DisplayName("salvar: tipo nulo lança IllegalArgumentException e não grava")
    void salvar_tipoNulo() {
        assertThatThrownBy(() -> itemAcervoService.salvar(request(null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tipo de item de acervo inválido.");

        verify(itemAcervoRepository, never()).save(any());
    }

    // -------------------------------------------------------------- atualizar

    @Test
    @DisplayName("atualizar: altera um Livro existente")
    void atualizar_livro() {
        when(itemAcervoRepository.findById(1L)).thenReturn(Optional.of(livroExistente));
        when(itemAcervoRepository.save(any(ItemAcervo.class))).thenAnswer(inv -> inv.getArgument(0));

        ItemAcervoResponse resposta = itemAcervoService.atualizar(1L, request(TipoItemAcervo.LIVRO));

        assertThat(resposta).isNotNull();
        assertThat(livroExistente.getTitulo()).isEqualTo("Novo título");
        assertThat(livroExistente.getAutor()).isEqualTo("Autor X");
        assertThat(livroExistente.getIsbn()).isEqualTo("978-0000000000");
        verify(itemAcervoRepository).save(livroExistente);
    }

    @Test
    @DisplayName("atualizar: altera uma Revista existente")
    void atualizar_revista() {
        when(itemAcervoRepository.findById(2L)).thenReturn(Optional.of(revistaExistente));
        when(itemAcervoRepository.save(any(ItemAcervo.class))).thenAnswer(inv -> inv.getArgument(0));

        itemAcervoService.atualizar(2L, request(TipoItemAcervo.REVISTA));

        assertThat(revistaExistente.getEdicao()).isEqualTo("12");
        assertThat(revistaExistente.getIssn()).isEqualTo("1234-5678");
        verify(itemAcervoRepository).save(revistaExistente);
    }

    @Test
    @DisplayName("atualizar: item inexistente lança RecursoNaoEncontradoException")
    void atualizar_inexistente() {
        when(itemAcervoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemAcervoService.atualizar(99L, request(TipoItemAcervo.LIVRO)))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("99");

        verify(itemAcervoRepository, never()).save(any());
    }

    @Test
    @DisplayName("atualizar: trocar o tipo de um item existente é recusado e não grava")
    void atualizar_tipoDiferente_recusado() {
        when(itemAcervoRepository.findById(1L)).thenReturn(Optional.of(livroExistente));

        assertThatThrownBy(() -> itemAcervoService.atualizar(1L, request(TipoItemAcervo.REVISTA)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(itemAcervoRepository, never()).save(any());
    }

    /**
     * ATENÇÃO: este teste FALHA com o service atual. O service chama
     * preencherCamposComuns() ANTES de validar o tipo, então a entidade já foi
     * alterada (título, tipo etc.) quando a exceção é lançada. Dentro de uma
     * transação isso poderia ser gravado. Corrija movendo a validação do tipo
     * para antes do preencherCamposComuns().
     */
    @Test
    @DisplayName("atualizar: request rejeitado não deve alterar a entidade")
    void atualizar_tipoDiferente_naoAlteraEntidade() {
        when(itemAcervoRepository.findById(1L)).thenReturn(Optional.of(livroExistente));

        assertThatThrownBy(() -> itemAcervoService.atualizar(1L, request(TipoItemAcervo.REVISTA)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(livroExistente.getTitulo()).isEqualTo("Clean Code");
        assertThat(livroExistente.getTipo()).isEqualTo(TipoItemAcervo.LIVRO);
    }

    // ------------------------------------------------------------ buscarTodos

    @Test
    @DisplayName("buscarTodos: converte todos os itens, livros e revistas")
    void buscarTodos() {
        when(itemAcervoRepository.findAll()).thenReturn(List.of(livroExistente, revistaExistente));

        List<ItemAcervoResponse> resultado = itemAcervoService.buscarTodos();

        assertThat(resultado).hasSize(2);
    }

    @Test
    @DisplayName("buscarTodos: acervo vazio devolve lista vazia")
    void buscarTodos_vazio() {
        when(itemAcervoRepository.findAll()).thenReturn(List.of());

        assertThat(itemAcervoService.buscarTodos()).isEmpty();
    }

    // ----------------------------------------------------------- buscarPorId

    @Test
    @DisplayName("buscarPorId: devolve o item quando existe")
    void buscarPorId_sucesso() {
        when(itemAcervoRepository.findById(1L)).thenReturn(Optional.of(livroExistente));

        assertThat(itemAcervoService.buscarPorId(1L)).isNotNull();
    }

    @Test
    @DisplayName("buscarPorId: lança exceção quando não existe")
    void buscarPorId_inexistente() {
        when(itemAcervoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemAcervoService.buscarPorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Item de acervo não encontrado com o ID: 99");
    }

    // --------------------------------------------------------------- deletar

    // ------------------------------------------------------------- validação

    private void definirCampo(ItemAcervoRequest r, String campo, String valor) {
        switch (campo) {
            case "titulo" -> lenient().when(r.titulo()).thenReturn(valor);
            case "localizacao" -> lenient().when(r.localizacao()).thenReturn(valor);
            case "autor" -> lenient().when(r.autor()).thenReturn(valor);
            case "isbn" -> lenient().when(r.isbn()).thenReturn(valor);
            case "genero" -> lenient().when(r.genero()).thenReturn(valor);
            case "edicao" -> lenient().when(r.edicao()).thenReturn(valor);
            case "issn" -> lenient().when(r.issn()).thenReturn(valor);
            case "mesAnoPublicacao" -> lenient().when(r.mesAnoPublicacao()).thenReturn(valor);
            default -> throw new IllegalArgumentException("Campo desconhecido: " + campo);
        }
    }

    @ParameterizedTest(name = "{0}: campo {1} ausente é recusado")
    @CsvSource({
            "LIVRO,titulo", "LIVRO,localizacao", "LIVRO,autor", "LIVRO,isbn", "LIVRO,genero",
            "REVISTA,titulo", "REVISTA,localizacao", "REVISTA,edicao", "REVISTA,issn", "REVISTA,mesAnoPublicacao"
    })
    void salvar_campoObrigatorioAusente(TipoItemAcervo tipo, String campo) {
        for (String invalido : new String[]{null, "", "   "}) {
            ItemAcervoRequest req = request(tipo);
            definirCampo(req, campo, invalido);

            assertThatThrownBy(() -> itemAcervoService.salvar(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("obrigatório");
        }
        verify(itemAcervoRepository, never()).save(any());
    }

    @Test
    @DisplayName("salvar: mensagem de campo obrigatório cita o campo")
    void salvar_mensagemCitaOCampo() {
        ItemAcervoRequest req = request(TipoItemAcervo.LIVRO);
        lenient().when(req.autor()).thenReturn(null);

        assertThatThrownBy(() -> itemAcervoService.salvar(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O campo autor é obrigatório.");
    }

    @Test
    @DisplayName("salvar: título acima de 200 caracteres é recusado")
    void salvar_tituloMuitoLongo() {
        ItemAcervoRequest req = request(TipoItemAcervo.LIVRO);
        lenient().when(req.titulo()).thenReturn("x".repeat(201));

        assertThatThrownBy(() -> itemAcervoService.salvar(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no máximo 200");
        verify(itemAcervoRepository, never()).save(any());
    }

    @Test
    @DisplayName("salvar: mês/ano de publicação fora do formato MM/aaaa é recusado")
    void salvar_mesAnoFormatoInvalido() {
        for (String invalido : new String[]{"2024-05", "13/2024", "5/2024", "05/24"}) {
            ItemAcervoRequest req = request(TipoItemAcervo.REVISTA);
            lenient().when(req.mesAnoPublicacao()).thenReturn(invalido);

            assertThatThrownBy(() -> itemAcervoService.salvar(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("MM/aaaa");
        }
        verify(itemAcervoRepository, never()).save(any());
    }

    @Test
    @DisplayName("salvar: ano negativo ou muito no futuro é recusado")
    void salvar_anoInvalido() {
        for (Integer invalido : new Integer[]{-1, LocalDate.now().getYear() + 2}) {
            ItemAcervoRequest req = request(TipoItemAcervo.LIVRO);
            lenient().when(req.ano()).thenReturn(invalido);

            assertThatThrownBy(() -> itemAcervoService.salvar(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("O ano informado é inválido.");
        }
    }

    @Test
    @DisplayName("salvar: ano ausente é aceito (campo opcional)")
    void salvar_semAno_aceito() {
        ItemAcervoRequest req = request(TipoItemAcervo.LIVRO);
        lenient().when(req.ano()).thenReturn(null);
        when(itemAcervoRepository.save(any(Livro.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(itemAcervoService.salvar(req)).isNotNull();
    }

    @Test
    @DisplayName("salvar: totalDisponivel negativo é recusado")
    void salvar_totalDisponivelNegativo() {
        ItemAcervoRequest req = request(TipoItemAcervo.LIVRO);
        lenient().when(req.totalDisponivel()).thenReturn(-1);

        assertThatThrownBy(() -> itemAcervoService.salvar(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negativa");
    }

    @Test
    @DisplayName("salvar: espaços nas pontas dos textos são removidos")
    void salvar_removeEspacosDasPontas() {
        ItemAcervoRequest req = request(TipoItemAcervo.LIVRO);
        lenient().when(req.titulo()).thenReturn("  Dom Casmurro  ");
        lenient().when(req.autor()).thenReturn(" Machado de Assis ");
        when(itemAcervoRepository.save(any(Livro.class))).thenAnswer(inv -> inv.getArgument(0));

        itemAcervoService.salvar(req);

        ArgumentCaptor<Livro> captor = ArgumentCaptor.forClass(Livro.class);
        verify(itemAcervoRepository).save(captor.capture());
        assertThat(captor.getValue().getTitulo()).isEqualTo("Dom Casmurro");
        assertThat(captor.getValue().getAutor()).isEqualTo("Machado de Assis");
    }

    @Test
    @DisplayName("atualizar: campo obrigatório ausente é recusado sem alterar a entidade")
    void atualizar_campoObrigatorioAusente() {
        when(itemAcervoRepository.findById(1L)).thenReturn(Optional.of(livroExistente));
        ItemAcervoRequest req = request(TipoItemAcervo.LIVRO);
        lenient().when(req.autor()).thenReturn("   ");

        assertThatThrownBy(() -> itemAcervoService.atualizar(1L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O campo autor é obrigatório.");

        assertThat(livroExistente.getTitulo()).isEqualTo("Clean Code");
        assertThat(livroExistente.getAutor()).isEqualTo("Robert C. Martin");
        verify(itemAcervoRepository, never()).save(any());
    }

    // --------------------------------------------------------------- deletar

    @Test
    @DisplayName("deletar: remove quando o item existe")
    void deletar_sucesso() {
        when(itemAcervoRepository.existsById(1L)).thenReturn(true);

        itemAcervoService.deletar(1L);

        verify(itemAcervoRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deletar: item inexistente lança exceção e não remove nada")
    void deletar_inexistente() {
        when(itemAcervoRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> itemAcervoService.deletar(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Item de acervo não encontrado com o ID: 99");

        verify(itemAcervoRepository, never()).deleteById(any());
    }
}