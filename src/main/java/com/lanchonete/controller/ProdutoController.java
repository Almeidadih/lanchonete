package com.lanchonete.controller;

import com.lanchonete.dto.ProdutoRequestDTO;
import com.lanchonete.dto.ProdutoResponseDTO;
import com.lanchonete.service.ProdutoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/produtos")
@RequiredArgsConstructor
public class ProdutoController {

    private final ProdutoService produtoService;

    @GetMapping
    public ResponseEntity<List<ProdutoResponseDTO>> listar(){
        return ResponseEntity.ok(produtoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoResponseDTO> buscarPorId(@PathVariable UUID id){
        return ResponseEntity.ok(produtoService.buscarPorId(id));
    }
    @PostMapping
    public ResponseEntity<ProdutoResponseDTO> criar( @Valid  @RequestBody ProdutoRequestDTO dto){
        ProdutoResponseDTO criado = produtoService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProdutoResponseDTO> atualizar(
            @PathVariable UUID id,
            @Valid  @RequestBody ProdutoRequestDTO dto) {
        return ResponseEntity.ok(produtoService.atualizar(id, dto));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        produtoService.remover(id);
        return ResponseEntity.noContent().build(); // HTTP 204
    }
    @PatchMapping("/{id}/disponibilidade")
    public ResponseEntity<ProdutoResponseDTO> alterarDisponibilidade(
            @PathVariable UUID id,
            @RequestParam boolean ativo) {
        return ResponseEntity.ok(produtoService.alterarDisponibilidade(id, ativo));
    }


}
