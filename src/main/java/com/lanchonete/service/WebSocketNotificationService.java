package com.lanchonete.service;

import com.lanchonete.dto.PedidoNotificacaoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;


    @Async
    public void notificarPedidoEmPreparo(UUID pedidoId, int tempoEstimado){
        PedidoNotificacaoDTO notificacao =
                PedidoNotificacaoDTO.emPreparo(pedidoId, tempoEstimado);

        // Notifica o cliente que está acompanhando este pedido especifico
        String topicoPedido ="/topic/pedidos" + pedidoId;
        messagingTemplate.convertAndSend(topicoPedido, notificacao);
        log.info("📡 [WS] Notificação EM_PREPARO enviada para {} | pedidoId={}" ,
                topicoPedido, pedidoId);

        // Notifica o painel da cozinha com o mesmo evento
        messagingTemplate.convertAndSend("📡 [WS] Notificação EM_PREPARO enviada para /topic/cozinha");
    }
    @Async
    public void notificarPedidoEmPronto(UUID pedidoId){
        PedidoNotificacaoDTO notificacao = PedidoNotificacaoDTO.pronto(pedidoId);

        // Notifica o cliente diretamente
        String topicoPedido = "/topic/pedidos/" + pedidoId;
        messagingTemplate.convertAndSend(topicoPedido, notificacao);
        log.info("📡 [WS] Notificação PRONTO enviada para {} | pedidoId={}",
                topicoPedido, pedidoId);

        // Notifica o balcão de atendimento
        messagingTemplate.convertAndSend("/topic/atendimento", notificacao);
        log.info("📡 [WS] Notificação PRONTO enviada para /topic/atendimento");
    }
}
