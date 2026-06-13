package com.lanchonete.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configura o message broker em memória.
     *
     * /topic → broadcast (1 servidor → N clientes inscritos)
     * /queue → ponto a ponto (1 servidor → 1 cliente específico)
     * /app   → prefixo para mensagens que passam pelo @MessageMapping
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Broker simples em memória — suficiente para este caso de uso.
        // Em produção com múltiplas instâncias, substituir por RabbitMQ STOMP broker relay.
        registry.enableSimpleBroker("/topic", "/queue");

        // Prefixo para endpoints @MessageMapping nos controllers
        registry.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Registra o endpoint WebSocket.
     * SockJS fornece fallback HTTP polling para ambientes sem suporte a WS.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // em produção, restringir ao domínio da aplicação
                .withSockJS();                 // habilita fallback SockJS
    }

    /**
     * Limites de transporte — evita consumo excessivo de memória por mensagens grandes.
     *
     * messageSizeLimit: 64KB por mensagem (suficiente para payloads JSON de pedidos)
     * sendBufferSizeLimit: 512KB no buffer de envio por sessão
     * sendTimeLimit: 10s de timeout no envio (desconecta cliente lento)
     */
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration
                .setMessageSizeLimit(64 * 1024)       // 64 KB
                .setSendBufferSizeLimit(512 * 1024)    // 512 KB
                .setSendTimeLimit(10 * 1000);          // 10 segundos
    }
}
