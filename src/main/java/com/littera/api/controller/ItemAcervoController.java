
package com.littera.api.controller;

import com.littera.api.dto.ItemAcervoRequest;
import com.littera.api.dto.ItemAcervoResponse;
import com.littera.api.service.ItemAcervoService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/acervo")
public class ItemAcervoController {

    private final ItemAcervoService itemAcervoService;

    public ItemAcervoController(ItemAcervoService itemAcervoService) {
        this.itemAcervoService = itemAcervoService;
    }

    @GetMapping
    public ResponseEntity<List<ItemAcervoResponse>> listar() {
        return ResponseEntity.ok(itemAcervoService.buscarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemAcervoResponse> buscarPorId(
            @PathVariable Long id) {
        return ResponseEntity.ok(itemAcervoService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<ItemAcervoResponse> criar(
            @RequestBody ItemAcervoRequest itemAcervoRequest) {

        ItemAcervoResponse novoItem =
                itemAcervoService.salvar(itemAcervoRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(novoItem);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ItemAcervoResponse> atualizar(
            @PathVariable Long id,
            @RequestBody ItemAcervoRequest itemAcervoRequest) {

        ItemAcervoResponse itemAtualizado =
                itemAcervoService.atualizar(id, itemAcervoRequest);

        return ResponseEntity.ok(itemAtualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        itemAcervoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
