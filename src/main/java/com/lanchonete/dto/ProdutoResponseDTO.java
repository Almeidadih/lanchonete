package com.lanchonete.dto;

import com.lanchonete.domain.entity.Produto;
import com.lanchonete.domain.enums.CategoriaProduto;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ProdutoResponseDTO {

    private UUID id;
    private String nome;
    private String descricao;
    private BigDecimal preco;
    private CategoriaProduto categoria;
    private Boolean disponivel;

    public static ProdutoResponseDTO fromEntity(Produto produto) {
        ProdutoResponseDTO dto = new ProdutoResponseDTO();
        dto.setId(produto.getId());
        dto.setNome(produto.getNome());
        dto.setDescricao(produto.getDescricao());
        dto.setPreco(produto.getPreco());
        dto.setCategoria(produto.getCategoria());
        dto.setDisponivel(produto.getDisponivel());
        return dto;
    }
}
