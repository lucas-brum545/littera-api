package com.littera.api.service;

import com.littera.api.dto.ItemAcervoRequest;
import com.littera.api.dto.ItemAcervoResponse;
import com.littera.api.exception.RecursoNaoEncontradoException;
import com.littera.api.model.ItemAcervo;
import com.littera.api.model.Livro;
import com.littera.api.model.Revista;
import com.littera.api.model.TipoItemAcervo;
import com.littera.api.repository.ItemAcervoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ItemAcervoService {

    @Autowired
    private ItemAcervoRepository itemAcervoRepository;

    public ItemAcervoResponse salvar(ItemAcervoRequest request) {
        if (request.tipo() == TipoItemAcervo.LIVRO) {
            Livro livro = new Livro();
            preencherCamposComuns(livro, request);

            // Atributos específicos de Livro
            livro.setAutor(request.autor());
            livro.setIsbn(request.isbn());
            livro.setGenero(request.genero());

            Livro livroSalvo = itemAcervoRepository.save(livro);
            return new ItemAcervoResponse(livroSalvo);

        } else if (request.tipo() == TipoItemAcervo.REVISTA) {
            Revista revista = new Revista();
            preencherCamposComuns(revista, request);

            // Atributos específicos de Revista
            revista.setEdicao(request.edicao());
            revista.setIssn(request.issn());
            revista.setMesAnoPublicacao(request.mesAnoPublicacao());

            Revista revistaSalva = itemAcervoRepository.save(revista);
            return new ItemAcervoResponse(revistaSalva);
        }

        throw new IllegalArgumentException("Tipo de item de acervo inválido.");
    }

    public ItemAcervoResponse atualizar(Long id, ItemAcervoRequest request) {
        ItemAcervo itemExistente = itemAcervoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item de acervo não encontrado com o ID: " + id));

        preencherCamposComuns(itemExistente, request);

        if (itemExistente instanceof Livro livro && request.tipo() == TipoItemAcervo.LIVRO) {
            livro.setAutor(request.autor());
            livro.setIsbn(request.isbn());
            livro.setGenero(request.genero());

            ItemAcervo atualizado = itemAcervoRepository.save(livro);
            return new ItemAcervoResponse(atualizado);

        } else if (itemExistente instanceof Revista revista && request.tipo() == TipoItemAcervo.REVISTA) {
            revista.setEdicao(request.edicao());
            revista.setIssn(request.issn());
            revista.setMesAnoPublicacao(request.mesAnoPublicacao());

            ItemAcervo atualizado = itemAcervoRepository.save(revista);
            return new ItemAcervoResponse(atualizado);
        }

        throw new IllegalArgumentException("Não é permitido alterar o tipo de um item existente ou os dados não correspondem.");
    }

    public List<ItemAcervoResponse> buscarTodos() {
        List<ItemAcervo> itens = itemAcervoRepository.findAll();
        return itens.stream()
                .map(ItemAcervoResponse::new)
                .toList();
    }

    public ItemAcervoResponse buscarPorId(Long id) {
        ItemAcervo item = itemAcervoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item de acervo não encontrado com o ID: " + id));
        return new ItemAcervoResponse(item);
    }

    public void deletar(Long id) {
        if(!itemAcervoRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Item de acervo não encontrado com o ID: " + id);
        }
        itemAcervoRepository.deleteById(id);
    }

    // Método auxiliar para evitar repetição de código nos campos comuns
    private void preencherCamposComuns(ItemAcervo item, ItemAcervoRequest request) {
        item.setTitulo(request.titulo());
        item.setAno(request.ano());
        item.setTipo(request.tipo());
        item.setLocalizacao(request.localizacao());
        item.setIdioma(request.idioma());

        // Novos campos TEXT adicionados
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
}