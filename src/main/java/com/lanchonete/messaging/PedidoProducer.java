package com.lanchonete.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PedidoProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-keys.pedido-novo}")
    private String rkPedidoNovo;

    @Value("${app.rabbitmq.routing-keys.pedido-pronto}")
    private String rkPedidoPronto;

    /**
     * Publica um evento de "novo pedido" na fila.pedidos.novos.
     * O listener da cozinha (PedidoConsumer) consumirá esta mensagem.
     *
     * @param pedidoId ID do pedido recém-criado
     */
    public void publicarNovoPedido(UUID pedidoId) {
        log.info("📤 Publicando novo pedido na fila | pedidoId={}", pedidoId);

        rabbitTemplate.convertAndSend(exchange, rkPedidoNovo, pedidoId);

        log.info("✅ Pedido {} publicado com sucesso na fila de novos pedidos", pedidoId);
    }

    /**
     * Publica um evento de "pedido pronto" na fila fila.pedidos.prontos.
     * Notifica que o pedido está disponível para retirada pelo cliente.
     *
     * @param pedidoId ID do pedido finalizado
     */
    public void publicarPedidoPronto(UUID pedidoId) {
        log.info("📤 Publicando pedido pronto na fila | pedidoId={}", pedidoId);

        rabbitTemplate.convertAndSend(exchange, rkPedidoPronto, pedidoId);

        log.info("✅ Pedido {} publicado com sucesso na fila de pedidos prontos", pedidoId);
    }


}
