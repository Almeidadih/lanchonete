package com.lanchonete.dto;

import com.lanchonete.domain.entity.Pedido;
import com.lanchonete.domain.enums.StatusPedido;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class PedidoResponseDTO {

    private UUID id;
    private List<ProdutoResponseDTO> produtos;
    private StatusPedido status;
    private Integer tempoPreparoEstimado;
    private LocalDateTime dataHoraCriacao;
    private String observacoes;
    private BigDecimal valorTotal;

    /**
     * Converte uma entidade Pedido para o DTO de resposta.
     * Calcula o valor total somando os preços dos produtos.
     */
    public static PedidoResponseDTO fromEntity(Pedido pedido) {
        PedidoResponseDTO dto = new PedidoResponseDTO();
        dto.setId(pedido.getId());
        dto.setStatus(pedido.getStatus());
        dto.setTempoPreparoEstimado(pedido.getTempoPreparoEstimado());
        dto.setDataHoraCriacao(pedido.getDataHoraCriacao());
        dto.setObservacoes(pedido.getObservacoes());

        // Converte lista de produtos para DTOs
        dto.setProdutos(
                pedido.getProdutos().stream()
                        .map(ProdutoResponseDTO::fromEntity)
                        .toList()
        );

        // Calcula valor total do pedido
        dto.setValorTotal(
                pedido.getProdutos().stream()
                        .map(p -> p.getPreco())
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        );

        return dto;
    }



}
