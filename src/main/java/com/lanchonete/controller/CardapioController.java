package com.lanchonete.controller;

import com.lanchonete.domain.enums.CategoriaProduto;
import com.lanchonete.dto.ProdutoResponseDTO;
import com.lanchonete.service.ProdutoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/cardapio")
@RequiredArgsConstructor
public class CardapioController {

    private final ProdutoService produtoService;

    @GetMapping
    public ResponseEntity<List<ProdutoResponseDTO>> listarCardapio(){
        return ResponseEntity.ok(produtoService.listarCardapio());
    }

    @GetMapping(params = "categoria")
    public ResponseEntity<List<ProdutoResponseDTO>> listarPorCategoria(@RequestParam CategoriaProduto categoria){
        return ResponseEntity.ok(produtoService.listarCategoria(categoria));
    }
}
