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

    

    public record NotificacaoWS(String titulo, String mensagem, String tipo) {}
}
