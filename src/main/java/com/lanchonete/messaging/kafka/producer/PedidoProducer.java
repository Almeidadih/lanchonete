package com.lanchonete.messaging.kafka.producer;

import com.lanchonete.messaging.event.PedidoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class PedidoProducer {

    private final KafkaTemplate<String, Object>  kafkaTemplate;

    @Value("${kafka.topics.pedido-criado}")
    private String topicPedidoCriado;

    @Value("${kafka.topics.pedido-atualizado}")
    private String topicPedidoAtualizado;

    @Value("${kafka.topics.pedido-cancelado}")
    private String topicPedidoCancelado;

    @Value("${kafka.topics.notificacao}")
    private String topicNotificacao;

    public void publicarPedidoCriado(PedidoEvent.PedidoCriadoEvent event){

        String key ="pedido-" + event.pedidoId();
        CompletableFuture<SendResult<String , Object>> future =
                kafkaTemplate.send(topicPedidoCriado,key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Erro ao publicar PedidoCriadoEvent para pedido {}: {}" , event.pedidoId(),ex.getMessage());
            }else {
                log.info("PedidoCriadoEvent publicado: pedido={}, offset={}",
                        event.pedidoId(),result.getRecordMetadata().offset());
            }
        });
    }

    public void publicarPedidoAtualizado(PedidoEvent.PedidoAtualizadoEvent event){
        String key = "pedido-" + event.pedidoId();
        CompletableFuture<SendResult<String,Object>> future =
                kafkaTemplate.send(topicPedidoAtualizado,key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Erro ao publicar PedidoAtualizadoEvent para o pedido {}:{}",
                        event.pedidoId(),ex.getMessage());
            }else {
                log.info("PedidoAtualizadEvent publicado; pedido={}, status={}",
                        event.pedidoId(),event.statusNovo());
            }
        });
    }

    public void publicarPedidoCancelado(PedidoEvent.PedidoCanceladoEvent event){
        String key = "pedido-" + event.pedidoId();
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topicPedidoCancelado, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Erro ao publicar PedidoCanceladoEvent para pedido {};{}", event.pedidoId(),ex.getMessage());
            }else {
                log.info("PedidoCanceladoEvent publicado: pedido={}", event.pedidoId());
            }
        });
    }
    public void publicarNotificacao(PedidoEvent.NotificacaoEvent event){
        String key = "usuario-" + event.usuarioId();
        kafkaTemplate.send(topicNotificacao, key, event);
        log.debug("NotificacaoEvent publicado para usuário {}", event.usuarioId());
    }
}
