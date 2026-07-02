package com.lanchonete.messaging.kafka.consumer;

import com.lanchonete.messaging.event.PedidoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PedidoConsumer {

    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(topics = "${kafka.topics.pedido-criado}", groupId = "lanchonete-group")
    public void consumirPedidoCriado(PedidoEvent.PedidoCriadoEvent event) {
        log.info("Pedido Criado recebido: id={}, usuario={}, total={}",
                event.pedidoId(), event.nomeUsuario(),event.valorTotal());

        //Notifica painel do balconista via WebSocket
        messagingTemplate.convertAndSend("/topic/pedidos/novos", event);

        //Notifica o cliente
        messagingTemplate.convertAndSendToUser(
                event.emailUsuario(),
                "/queue/pedidos",
                new NotificacaoWS("Pedido recebido!",
                        "Seu pedido #" + "foi recebido com sucesso.", "Sucesso")
        );
    }

    @KafkaListener(topics = "${kafka.topics.pedido-atualizado}", groupId = "lanchonete-group")
    public void consumirPedidoAtualizado(PedidoEvent.PedidoAtualizadoEvent event) {
        log.info("Pedido atualizado: id={}, {} -> {}", event.pedidoId(),event.statusAnterior(),event.statusNovo());

        // Notifica todos os balconistas sobre mudanças de status
        messagingTemplate.convertAndSend("topic/pedidos/atualizados");

        //Notifica o cliente especifico
        String mensagem = switch (event.statusNovo()) {
            case CONFIRMADO -> "Seu pedido foi confirmado! 🎉";
            case EM_PREPARO -> "Seu pedido está sendo preparado! 👨‍🍳";
            case PRONTO -> "Seu pedido está pronto! 🚀";
            case ENTREGUE -> "Pedido entregue. Bom apetite! 😋";
            case CANCELADO -> "Seu pedido foi cancelado.";
            default -> "Status do pedido atualizado.";
        };

        messagingTemplate.convertAndSendToUser(
                event.emailUsuario(),
                "/queue/pedidos",
                new NotificacaoWS("Pedido #" + event.pedidoId(),mensagem,"INFO")
        );
    }

    @KafkaListener(topics = "${kafka.topics.pedido-cancelado}", groupId = "lanchonete-group")
    public void consumirPedidoCancelado(PedidoEvent.PedidoCanceladoEvent event) {
        log.info("Pedido cancelado: id={}", event.pedidoId());

        messagingTemplate.convertAndSend("/topic/pedidos/cancelados", event);

        messagingTemplate.convertAndSendToUser(
                event.emailUsuario(),
                "/queue/pedidos",
                new NotificacaoWS("Pedido cancelado",
                        "Seu pedido #" + event.pedidoId() + " foi cancelado.", "AVISO")
        );
    }

    @KafkaListener(topics = "${kafka.topics.notificacao}", groupId = "lanchonete-group")
    public void consumirNotificacao(PedidoEvent.NotificacaoEvent event) {
        log.debug("Notificação recebida para usuário {}: {}", event.usuarioId(), event.titulo());

        messagingTemplate.convertAndSendToUser(
                event.emailUsuario(),
                "/queue/notificacoes",
                new NotificacaoWS(event.titulo(), event.mensagem(), event.tipo())
        );
    }




    public record NotificacaoWS(String titulo, String mensagem, String tipo) {}
}
