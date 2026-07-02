package com.lanchonete.messaging.event;

import com.lanchonete.domain.enums.StatusPedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PedidoEvent {

    public record PedidoCriadoEvent(
            Long pedidoId,
            Long usuarioId,
            String nomeUsuario,
            String emailUsuario,
            BigDecimal valorTotal,
            int quantidadeItens,
            LocalDateTime criadoEm
    ) {}
    public record PedidoAtualizadoEvent(
            Long pedidoId,
            Long usuarioId,
            String emailUsuario,
            StatusPedido statusAnterior,
            StatusPedido statusNovo,
            LocalDateTime atualizadoEm
    ) {}

    public record PedidoCanceladoEvent(
            Long pedidoId,
            Long usuarioId,
            String emailUsuario,
            String motivo,
            LocalDateTime canceladoEm
    ) {}

    public record NotificacaoEvent(
            Long usuarioId,
            String emailUsuario,
            String titulo,
            String mensagem,
            String tipo,
            LocalDateTime criadoEm
    ) {}
}
