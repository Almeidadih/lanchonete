package com.lanchonete.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class PedidoRequestDTO {

    @NotEmpty(message = "O pedido deve conter pelo menos um produto")
    private List<UUID> produtoIds;

    private String observacoes;
}
