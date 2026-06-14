package com.lanchonete.messaging;


import com.lanchonete.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PedidoConsumer {

    private final PedidoRepository pedidoRepository;
    private final WebsocketNoticationService websocketNoticationService;


}
