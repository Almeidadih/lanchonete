package com.lanchonete.messaging;


import com.lanchonete.domain.entity.Pedido;
import com.lanchonete.domain.enums.StatusPedido;
import com.lanchonete.repository.PedidoRepository;
import com.lanchonete.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PedidoConsumer {

    private final PedidoRepository pedidoRepository;
    private final WebSocketNotificationService wsNotificationService;

    @RabbitListener(queues = "${app.rabbitmq.queues.pedidos-novos}")
    public void processarNovoPedido(UUID pedidoId) {
        log.info("🍳 [COZINHA] Nova mensagem recebida | pedidoId={}", pedidoId);

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> {
                    log.error("❌ Pedido {} não encontrado. Mensagem descartada.", pedidoId);
                    return new RuntimeException("Pedido " + pedidoId + " não encontrado");
                });

        // Idempotência: ignora se já foi processado
        if (pedido.getStatus() != StatusPedido.RECEBIDO) {
            log.warn("⚠️ Pedido {} ignorado — status atual: {}. Esperado: RECEBIDO.",
                    pedidoId, pedido.getStatus());
            return;
        }

        // Lógica da cozinha: 5 min por item
        int quantidadeItens = pedido.getProdutos().size();
        int tempoEstimado = quantidadeItens * 5;

        pedido.setStatus(StatusPedido.EM_PREPARO);
        pedido.setTempoPreparoEstimado(tempoEstimado);
        pedidoRepository.save(pedido);

        log.info("✅ [COZINHA] Pedido {} em preparo | {} itens | {} min",
                pedidoId, quantidadeItens, tempoEstimado);

        // Notificação WebSocket assíncrona — não bloqueia este thread
        // O cliente vê o status mudar em tempo real no browser
        wsNotificationService.notificarPedidoEmPreparo(pedidoId, tempoEstimado);
    }

    /**
     * LISTENER: fila.pedidos.prontos
     *
     * Notifica via WebSocket que o pedido está pronto para retirada.
     * O atendente e o cliente recebem a notificação em tempo real.
     */
    @RabbitListener(queues = "${app.rabbitmq.queues.pedidos-prontos}")
    public void notificarPedidoPronto(UUID pedidoId) {
        log.info("🔔 [ATENDIMENTO] Pedido {} PRONTO PARA RETIRADA!", pedidoId);

        pedidoRepository.findById(pedidoId).ifPresentOrElse(
                pedido -> {
                    log.info("📢 Pedido #{} — {} item(ns) — chamar cliente",
                            pedidoId, pedido.getProdutos().size());

                    // Broadcast WebSocket assíncrono para o cliente e balcão
                    wsNotificationService.notificarPedidoPronto(pedidoId);
                },
                () -> log.warn("⚠️ Pedido {} não encontrado ao notificar pronto", pedidoId)
        );
    }

}
