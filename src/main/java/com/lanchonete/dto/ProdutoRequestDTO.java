package com.lanchonete.dto;


import com.lanchonete.domain.enums.CategoriaProduto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO para criação e atualização de Produto.
 * Separa a camada de transporte (HTTP) da entidade de domínio.
 */
@Data
public class ProdutoRequestDTO {

    @NotBlank(message = "O nome do produto é obrigatório")
    private String nome;

    private String descricao;

    @NotNull(message = "O preço é obrigatório")
    @Positive(message = "O preço deve ser positivo")
    private BigDecimal preco;

    @NotNull(message = "A categoria é obrigatória")
    private CategoriaProduto categoria;

    private Boolean disponivel = true;
}
