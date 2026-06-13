package com.lanchonete.dto;

import com.lanchonete.domain.entity.Produto;
import com.lanchonete.domain.enums.StatusPedido;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PedidoNotificacaoDTO {

    private UUID pedidoId;
    private StatusPedido status;
    private Integer tempoPreparoEstimado;
    private String mensagem;
    private LocalDateTime timestamp;

    /**
     * Factory para evento de início de preparo.
     */
    public static PedidoNotificacaoDTO emPreparo(UUID pedidoId, int tempoEstimado) {
        return PedidoNotificacaoDTO.builder()
                .pedidoId(pedidoId)
                .status(StatusPedido.EM_PREPARO)
                .tempoPreparoEstimado(tempoEstimado)
                .mensagem(String.format(
                        "Seu pedido #%d foi recebido pela cozinha! Tempo estimado: %d minutos.",
                        pedidoId, tempoEstimado))
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Factory para evento de pedido pronto.
     */
    public static PedidoNotificacaoDTO pronto(UUID pedidoId) {
        return PedidoNotificacaoDTO.builder()
                .pedidoId(pedidoId)
                .status(StatusPedido.PRONTO)
                .tempoPreparoEstimado(0)
                .mensagem(String.format(
                        "🔔 Pedido #%d PRONTO! Por favor, retire no balcão.", pedidoId))
                .timestamp(LocalDateTime.now())
                .build();
    }
}
