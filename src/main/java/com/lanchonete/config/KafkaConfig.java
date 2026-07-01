package com.lanchonete.config;



import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${kafka.topics.pedido-criado}")
    private String topicPedidoCriado;

    @Value("${kafka.topics.pedido-atualizado}")
    private String topicPedidoAtualizado;

    @Value("${kafka.topics.pedido-cancelado}")
    private String topicPedidoCancelado;

    @Value("${kafka.topics.notificacao}")
    private String topicNotificacao;

    @Bean
    public NewTopic topicPedidoCriado() {
        return TopicBuilder.name(topicPedidoCriado)
                .partitions(3)
                .replicas(1)
                .build();
    }
    @Bean
    public NewTopic topicPedidoAtualizado() {
        return TopicBuilder.name(topicPedidoAtualizado)
                .partitions(3)
                .replicas(1)
                .build();
    }
    @Bean
    public NewTopic topicPedidoCancelado() {
        return TopicBuilder.name(topicPedidoCancelado)
                .partitions(1)
                .replicas(1)
                .build();
    }
    @Bean
    public NewTopic topicNotificacao() {
        return TopicBuilder.name(topicNotificacao)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
