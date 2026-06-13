package com.lanchonete.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class PedidoRequestDTO {

    @NotEmpty(message = "O pedido deve conter pelo menos um produto")
    private List<Long> produtoIds;

    private String observacoes;
}
