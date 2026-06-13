package com.lanchonete.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.queues.pedidos-novos}")
    private String filaPedidosNovos;

    @Value("${app.rabbitmq.queues.pedidos-prontos}")
    private String filaPedidosProntos;

    @Value("${app.rabbitmq.routing-keys.pedido-novo}")
    private String rkPedidoNovo;

    @Value("${app.rabbitmq.routing-keys.pedido-pronto}")
    private String rkPedidoPronto;

    @Bean
    @Primary
    public CachingConnectionFactory connectionFactory(
            @Value("${spring.rabbitmq.host:localhost}") String host,
            @Value("${spring.rabbitmq.port:5672}") int port,
            @Value("${spring.rabbitmq.username:guest}") String username,
            @Value("${spring.rabbitmq.password:guest}") String password
    )
    {

        CachingConnectionFactory factory = new CachingConnectionFactory(host, port);
        factory.setUsername(username);
        factory.setPassword(password);
        factory.setChannelCacheSize(5);
        factory.setCacheMode(CachingConnectionFactory.CacheMode.CHANNEL);
        factory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        factory.setPublisherReturns(true);
        return factory;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {

        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();

        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(5);
        factory.setPrefetchCount(1);

        return factory;
    }
    @Bean
    public TopicExchange lanchoneteExchange() {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public Queue filaPedidosNovos() {
        return QueueBuilder.durable(filaPedidosNovos).build();
    }

    @Bean
    public Queue filaPedidosProntos() {
        return QueueBuilder.durable(filaPedidosProntos).build();
    }

    @Bean
    public Binding bindingPedidosNovos(Queue filaPedidosNovos,
                                       TopicExchange lanchoneteExchange) {
        return BindingBuilder.bind(filaPedidosNovos)
                .to(lanchoneteExchange).with(rkPedidoNovo);
    }

    @Bean
    public Binding bindingPedidosProntos(Queue filaPedidosProntos,
                                         TopicExchange lanchoneteExchange) {
        return BindingBuilder.bind(filaPedidosProntos)
                .to(lanchoneteExchange).with(rkPedidoPronto);
    }

    // =====================
    // Serialização JSON
    // =====================

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        template.setMandatory(true);
        return template;
    }

}
