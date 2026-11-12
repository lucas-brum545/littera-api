package com.littera.api.service;

import com.littera.api.dto.ItemAcervoRequest;
import com.littera.api.dto.ItemAcervoResponse;
import com.littera.api.exception.RecursoNaoEncontradoException;
import com.littera.api.model.ItemAcervo;
import com.littera.api.model.Livro;
import com.littera.api.model.Revista;
import com.littera.api.model.TipoItemAcervo;
import com.littera.api.repository.ItemAcervoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class ItemAcervoService {

    // Formato esperado da coluna mes_ano_publicacao (length = 7): "05/2024"
    private static final Pattern MES_ANO = Pattern.compile("^(0[1-9]|1[0-2])/\\d{4}$");

    private final ItemAcervoRepository itemAcervoRepository;

    public ItemAcervoService(ItemAcervoRepository itemAcervoRepository) {
        this.itemAcervoRepository = itemAcervoRepository;
    }

    @Transactional
    public ItemAcervoResponse salvar(ItemAcervoRequest request) {
        if (request.tipo() == null) {
            throw new IllegalArgumentException("Tipo de item de acervo inválido.");
        }
        validar(request);

        if (request.tipo() == TipoItemAcervo.LIVRO) {
            Livro livro = new Livro();
            preencherCamposComuns(livro, request);
            preencherLivro(livro, request);
            return new ItemAcervoResponse(itemAcervoRepository.save(livro));

        } else if (request.tipo() == TipoItemAcervo.REVISTA) {
            Revista revista = new Revista();
            preencherCamposComuns(revista, request);
            preencherRevista(revista, request);
            return new ItemAcervoResponse(itemAcervoRepository.save(revista));
        }

        throw new IllegalArgumentException("Tipo de item de acervo inválido.");
    }

    @Transactional
    public ItemAcervoResponse atualizar(Long id, ItemAcervoRequest request) {
        ItemAcervo item = itemAcervoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item de acervo não encontrado com o ID: " + id));

        // 1. Valida ANTES de alterar a entidade
        boolean tipoCompativel =
                (item instanceof Livro && request.tipo() == TipoItemAcervo.LIVRO)
                        || (item instanceof Revista && request.tipo() == TipoItemAcervo.REVISTA);

        if (!tipoCompativel) {
            throw new IllegalArgumentException(
                    "Não é permitido alterar o tipo de um item existente ou os dados não correspondem.");
        }
        validar(request);

        // 2. Só então altera
        preencherCamposComuns(item, request);
        if (item instanceof Livro livro) {
            preencherLivro(livro, request);
        } else if (item instanceof Revista revista) {
            preencherRevista(revista, request);
        }

        return new ItemAcervoResponse(itemAcervoRepository.save(item));
    }

    public List<ItemAcervoResponse> buscarTodos() {
        return itemAcervoRepository.findAll().stream()
                .map(ItemAcervoResponse::new)
                .toList();
    }

    public ItemAcervoResponse buscarPorId(Long id) {
        ItemAcervo item = itemAcervoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item de acervo não encontrado com o ID: " + id));
        return new ItemAcervoResponse(item);
    }

    public void deletar(Long id) {
        if (!itemAcervoRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Item de acervo não encontrado com o ID: " + id);
        }
        itemAcervoRepository.deleteById(id);
    }

    // ------------------------------------------------------------- validação

    /** Valida o request conforme as colunas NOT NULL e os tamanhos das entidades. */
    private void validar(ItemAcervoRequest r) {
        texto(r.titulo(), "título", 200);
        texto(r.localizacao(), "localização", 50);

        if (r.ano() != null && (r.ano() < 0 || r.ano() > LocalDate.now().getYear() + 1)) {
            throw new IllegalArgumentException("O ano informado é inválido.");
        }
        if (r.totalDisponivel() != null && r.totalDisponivel() < 0) {
            throw new IllegalArgumentException("A quantidade disponível não pode ser negativa.");
        }

        if (r.tipo() == TipoItemAcervo.LIVRO) {
            texto(r.autor(), "autor", 100);
            texto(r.isbn(), "ISBN", 20);
            texto(r.genero(), "gênero", 50);
        } else if (r.tipo() == TipoItemAcervo.REVISTA) {
            texto(r.edicao(), "edição", 10);
            texto(r.issn(), "ISSN", 20);
            texto(r.mesAnoPublicacao(), "mês/ano de publicação", 7);
            if (!MES_ANO.matcher(r.mesAnoPublicacao().trim()).matches()) {
                throw new IllegalArgumentException(
                        "O campo mês/ano de publicação deve estar no formato MM/aaaa (ex.: 05/2024).");
            }
        }
    }

    private void texto(String valor, String rotulo, int maximo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("O campo " + rotulo + " é obrigatório.");
        }
        if (valor.trim().length() > maximo) {
            throw new IllegalArgumentException(
                    "O campo " + rotulo + " deve ter no máximo " + maximo + " caracteres.");
        }
    }

    // ------------------------------------------------------------ preenchimento

    private void preencherCamposComuns(ItemAcervo item, ItemAcervoRequest request) {
        item.setTitulo(request.titulo().trim());
        item.setAno(request.ano());
        item.setTipo(request.tipo());
        item.setLocalizacao(request.localizacao().trim());
        item.setIdioma(request.idioma());
        item.setCapa(request.capa());
        item.setDescricao(request.descricao());

        if (request.cadastradoEm() != null) {
            item.setCadastradoEm(request.cadastradoEm());
        } else if (item.getCadastradoEm() == null) {
            item.setCadastradoEm(LocalDate.now());
        }

        if (request.totalDisponivel() != null) {
            item.setQuantidadeDisponivel(request.totalDisponivel());
        }
    }

    private void preencherLivro(Livro livro, ItemAcervoRequest request) {
        livro.setAutor(request.autor().trim());
        livro.setIsbn(request.isbn().trim());
        livro.setGenero(request.genero().trim());
    }

    private void preencherRevista(Revista revista, ItemAcervoRequest request) {
        revista.setEdicao(request.edicao().trim());
        revista.setIssn(request.issn().trim());
        revista.setMesAnoPublicacao(request.mesAnoPublicacao().trim());
    }
}