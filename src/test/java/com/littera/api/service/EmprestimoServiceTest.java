package com.littera.api.service;

import com.littera.api.dto.EmprestimoRequest;
import com.littera.api.dto.EmprestimoResponse;
import com.littera.api.exception.RecursoNaoEncontradoException;
import com.littera.api.model.Emprestimo;
import com.littera.api.model.Livro;
import com.littera.api.model.Revista;
import com.littera.api.model.Usuario;
import com.littera.api.repository.EmprestimoRepository;
import com.littera.api.repository.ItemAcervoRepository;
import com.littera.api.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmprestimoServiceTest {

    @Mock
    private EmprestimoRepository emprestimoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ItemAcervoRepository itemAcervoRepository;

    @InjectMocks
    private EmprestimoService emprestimoService;

    private Usuario usuario;
    private Livro livro;
    private Revista revista;
    private Emprestimo emprestimo;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("Lucas Brum");
        usuario.setEmail("lucas@teste.com");
        usuario.setBloqueado(false);
        usuario.setQuantidadeEmprestada(0);

        livro = new Livro();
        livro.setCodigo(10L);
        livro.setTitulo("Clean Code");
        livro.setQuantidadeDisponivel(2);

        revista = new Revista();
        revista.setCodigo(20L);
        revista.setTitulo("Superinteressante");
        revista.setQuantidadeDisponivel(1);

        emprestimo = new Emprestimo();
        emprestimo.setCodigo(100L);
        emprestimo.setUsuario(usuario);
        emprestimo.setItemAcervo(livro);
        emprestimo.setDataEmprestimo(LocalDate.now());
        emprestimo.setDataDevolucaoPrevista(LocalDate.now().plusDays(14));
        emprestimo.setDataDevolucaoReal(null);
    }

    /**
     * Único ponto que monta o request. Se a ordem/quantidade de componentes do
     * record EmprestimoRequest for diferente, só este método precisa ser ajustado.
     * Ordem assumida: codigoUsuario, codigoItemAcervo, dataEmprestimo, dataDevolucaoPrevista.
     */
    private EmprestimoRequest request(Long codigoUsuario, Long codigoItem,
                                      LocalDate dataEmprestimo, LocalDate dataDevolucaoPrevista, LocalDate dataDevolucaoReal) {
        return new EmprestimoRequest(codigoUsuario, codigoItem, dataEmprestimo, dataDevolucaoPrevista, dataDevolucaoReal);
    }

    // ---------------------------------------------------------------- salvar

    @Test
    @DisplayName("salvar: livro tem prazo de 14 dias e atualiza estoque e contador do usuário")
    void salvar_livro_sucesso() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(itemAcervoRepository.findById(10L)).thenReturn(Optional.of(livro));
        when(emprestimoRepository.save(any(Emprestimo.class))).thenAnswer(inv -> inv.getArgument(0));

        EmprestimoResponse resposta = emprestimoService.salvar(request(1L, 10L, LocalDate.now(), null,null));

        assertThat(resposta).isNotNull();
        assertThat(livro.getQuantidadeDisponivel()).isEqualTo(1);
        assertThat(usuario.getQuantidadeEmprestada()).isEqualTo(1);

        ArgumentCaptor<Emprestimo> captor = ArgumentCaptor.forClass(Emprestimo.class);
        verify(emprestimoRepository).save(captor.capture());
        Emprestimo salvo = captor.getValue();
        assertThat(salvo.getUsuario()).isSameAs(usuario);
        assertThat(salvo.getItemAcervo()).isSameAs(livro);
        assertThat(salvo.getDataDevolucaoPrevista()).isEqualTo(LocalDate.now().plusDays(14));
        assertThat(salvo.getDataDevolucaoReal()).isNull();

        verify(usuarioRepository).save(usuario);
        verify(itemAcervoRepository).save(livro);
    }

    @Test
    @DisplayName("salvar: revista tem prazo de 7 dias")
    void salvar_revista_prazoDeSeteDias() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(itemAcervoRepository.findById(20L)).thenReturn(Optional.of(revista));
        when(emprestimoRepository.save(any(Emprestimo.class))).thenAnswer(inv -> inv.getArgument(0));

        emprestimoService.salvar(request(1L, 20L, LocalDate.now(), null, null));

        ArgumentCaptor<Emprestimo> captor = ArgumentCaptor.forClass(Emprestimo.class);
        verify(emprestimoRepository).save(captor.capture());
        assertThat(captor.getValue().getDataDevolucaoPrevista()).isEqualTo(LocalDate.now().plusDays(7));
        assertThat(revista.getQuantidadeDisponivel()).isZero();
    }

    @Test
    @DisplayName("salvar: sem data de empréstimo no request, usa a data de hoje")
    void salvar_semDataEmprestimo_usaHoje() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(itemAcervoRepository.findById(10L)).thenReturn(Optional.of(livro));
        when(emprestimoRepository.save(any(Emprestimo.class))).thenAnswer(inv -> inv.getArgument(0));

        emprestimoService.salvar(request(1L, 10L, null, null, null));

        ArgumentCaptor<Emprestimo> captor = ArgumentCaptor.forClass(Emprestimo.class);
        verify(emprestimoRepository).save(captor.capture());
        assertThat(captor.getValue().getDataEmprestimo()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("salvar: usuário inexistente lança RecursoNaoEncontradoException")
    void salvar_usuarioInexistente() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emprestimoService.salvar(request(99L, 10L, LocalDate.now(), null, null)))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("99");

        verify(emprestimoRepository, never()).save(any());
    }

    @Test
    @DisplayName("salvar: item inexistente lança RecursoNaoEncontradoException")
    void salvar_itemInexistente() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(itemAcervoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emprestimoService.salvar(request(1L, 99L, LocalDate.now(), null, null)))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("99");

        verify(emprestimoRepository, never()).save(any());
    }

    @Test
    @DisplayName("salvar: usuário bloqueado dentro do prazo não pode emprestar")
    void salvar_usuarioBloqueado() {
        usuario.setBloqueado(true);
        usuario.setDataFimBloqueio(LocalDate.now().plusDays(5));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(itemAcervoRepository.findById(10L)).thenReturn(Optional.of(livro));

        assertThatThrownBy(() -> emprestimoService.salvar(request(1L, 10L, LocalDate.now(), null, null)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Usuário bloqueado até");

        verify(emprestimoRepository, never()).save(any());
        assertThat(livro.getQuantidadeDisponivel()).isEqualTo(2); // estoque intacto
    }

    @Test
    @DisplayName("salvar: bloqueio já expirado é removido e o empréstimo é permitido")
    void salvar_bloqueioExpirado_liberaUsuario() {
        usuario.setBloqueado(true);
        usuario.setDataFimBloqueio(LocalDate.now().minusDays(1));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(itemAcervoRepository.findById(10L)).thenReturn(Optional.of(livro));
        when(emprestimoRepository.save(any(Emprestimo.class))).thenAnswer(inv -> inv.getArgument(0));

        emprestimoService.salvar(request(1L, 10L, LocalDate.now(), null, null));

        assertThat(usuario.getBloqueado()).isFalse();
        assertThat(usuario.getDataFimBloqueio()).isNull();
        verify(emprestimoRepository).save(any(Emprestimo.class));
    }

    @Test
    @DisplayName("salvar: item sem exemplares disponíveis não pode ser emprestado")
    void salvar_itemIndisponivel() {
        livro.setQuantidadeDisponivel(0);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(itemAcervoRepository.findById(10L)).thenReturn(Optional.of(livro));

        assertThatThrownBy(() -> emprestimoService.salvar(request(1L, 10L, LocalDate.now(), null, null)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Item não está disponível para empréstimo.");

        verify(emprestimoRepository, never()).save(any());
        assertThat(usuario.getQuantidadeEmprestada()).isZero();
    }

    // ------------------------------------------------------------- atualizar

    @Test
    @DisplayName("atualizar: devolução no prazo repõe estoque, reduz contador e não bloqueia")
    void atualizar_devolucaoNoPrazo() {
        usuario.setQuantidadeEmprestada(1);
        livro.setQuantidadeDisponivel(1);
        LocalDate devolucao = emprestimo.getDataDevolucaoPrevista(); // exatamente no prazo
        when(emprestimoRepository.findById(100L)).thenReturn(Optional.of(emprestimo));
        when(emprestimoRepository.save(any(Emprestimo.class))).thenAnswer(inv -> inv.getArgument(0));

        emprestimoService.atualizar(100L, request(1L, 10L, null, devolucao, null));

        assertThat(emprestimo.getDataDevolucaoReal()).isEqualTo(devolucao);
        assertThat(usuario.getBloqueado()).isFalse();
        assertThat(usuario.getQuantidadeEmprestada()).isZero();
        assertThat(livro.getQuantidadeDisponivel()).isEqualTo(2);
        verify(usuarioRepository).save(usuario);
        verify(itemAcervoRepository).save(livro);
    }

    @Test
    @DisplayName("atualizar: atraso de 3 dias bloqueia o usuário por 6 dias (dobro)")
    void atualizar_devolucaoComAtraso_aplicaPenalidade() {
        usuario.setQuantidadeEmprestada(1);
        LocalDate devolucao = emprestimo.getDataDevolucaoPrevista().plusDays(3);
        when(emprestimoRepository.findById(100L)).thenReturn(Optional.of(emprestimo));
        when(emprestimoRepository.save(any(Emprestimo.class))).thenAnswer(inv -> inv.getArgument(0));

        emprestimoService.atualizar(100L, request(1L, 10L, null, devolucao, null));

        assertThat(usuario.getBloqueado()).isTrue();
        assertThat(usuario.getDataFimBloqueio()).isEqualTo(devolucao.plusDays(6));
    }

    @Test
    @DisplayName("atualizar: empréstimo já devolvido não é processado de novo")
    void atualizar_jaDevolvido_naoAlteraNada() {
        emprestimo.setDataDevolucaoReal(LocalDate.now().minusDays(1));
        usuario.setQuantidadeEmprestada(3);
        livro.setQuantidadeDisponivel(5);
        when(emprestimoRepository.findById(100L)).thenReturn(Optional.of(emprestimo));
        when(emprestimoRepository.save(any(Emprestimo.class))).thenAnswer(inv -> inv.getArgument(0));

        emprestimoService.atualizar(100L, request(1L, 10L, null, null, null));

        assertThat(usuario.getQuantidadeEmprestada()).isEqualTo(3);
        assertThat(livro.getQuantidadeDisponivel()).isEqualTo(5);
        verify(usuarioRepository, never()).save(any());
        verify(itemAcervoRepository, never()).save(any());
    }

    @Test
    @DisplayName("atualizar: request sem data de devolução não dá baixa")
    void atualizar_semDataDevolucao_naoDaBaixa() {
        when(emprestimoRepository.findById(100L)).thenReturn(Optional.of(emprestimo));
        when(emprestimoRepository.save(any(Emprestimo.class))).thenAnswer(inv -> inv.getArgument(0));

        emprestimoService.atualizar(100L, request(1L, 10L, null, null, null));

        assertThat(emprestimo.getDataDevolucaoReal()).isNull();
        verify(usuarioRepository, never()).save(any());
        verify(itemAcervoRepository, never()).save(any());
    }

    @Test
    @DisplayName("atualizar: empréstimo inexistente lança RecursoNaoEncontradoException")
    void atualizar_inexistente() {
        when(emprestimoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emprestimoService.atualizar(999L, request(1L, 10L, null, LocalDate.now(), null)))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("999");
    }

    // --------------------------------------------------------------- deletar

    @Test
    @DisplayName("deletar: remove quando o código existe")
    void deletar_sucesso() {
        when(emprestimoRepository.existsById(100L)).thenReturn(true);

        emprestimoService.deletar(100L);

        verify(emprestimoRepository, times(1)).deleteById(100L);
    }

    @Test
    @DisplayName("deletar: código inexistente lança exceção e não remove nada")
    void deletar_inexistente() {
        when(emprestimoRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> emprestimoService.deletar(999L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Empréstimo não encontrado com o código: 999");

        verify(emprestimoRepository, never()).deleteById(any());
    }

    // ---------------------------------------------------------------- buscas

    @Test
    @DisplayName("buscarId: retorna o empréstimo quando existe")
    void buscarId_sucesso() {
        when(emprestimoRepository.findById(100L)).thenReturn(Optional.of(emprestimo));

        assertThat(emprestimoService.buscarId(100L)).isNotNull();
    }

    @Test
    @DisplayName("buscarId: lança exceção quando não existe")
    void buscarId_inexistente() {
        when(emprestimoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emprestimoService.buscarId(999L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("listarTodos / listarTodosResumo: convertem todos os empréstimos")
    void listagens() {
        when(emprestimoRepository.findAll()).thenReturn(List.of(emprestimo));

        assertThat(emprestimoService.listarTodos()).hasSize(1);
        assertThat(emprestimoService.listarTodosResumo()).hasSize(1);
    }

    @Test
    @DisplayName("buscarPorIdResumo: Optional presente quando existe e vazio quando não")
    void buscarPorIdResumo() {
        when(emprestimoRepository.findById(100L)).thenReturn(Optional.of(emprestimo));
        when(emprestimoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThat(emprestimoService.buscarPorIdResumo(100L)).isPresent();
        assertThat(emprestimoService.buscarPorIdResumo(999L)).isEmpty();
    }
}