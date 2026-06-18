package com.lanchonete.controller;

import com.lanchonete.domain.enums.StatusPedido;
import com.lanchonete.dto.PedidoRequestDTO;
import com.lanchonete.dto.PedidoResponseDTO;
import com.lanchonete.service.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;

    @PostMapping
    public ResponseEntity<PedidoResponseDTO> criar(@Valid @RequestBody PedidoRequestDTO dto) {
        PedidoResponseDTO pedido = pedidoService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(pedido);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponseDTO> buscarPorId(@PathVariable UUID id){
        return ResponseEntity.ok(pedidoService.buscarPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<PedidoResponseDTO>> listarPorStatus(
            @RequestParam(required = false)StatusPedido status) {
        if (status == null) {
            // sem filtro: retorna pedidos em aberto (não entregues) - comportamento padrão
            return ResponseEntity.ok(pedidoService.listarPorStatus(StatusPedido.EM_PREPARO));
        }
        return ResponseEntity.ok(pedidoService.listarPorStatus(status));
    }

    @PostMapping("/{id}/pronto")
    public ResponseEntity<PedidoResponseDTO> marcarComoPronto(@PathVariable UUID id) {
        return ResponseEntity.ok(pedidoService.marcarComoPronto(id));

    }

    @PostMapping("/{id}/entregue")
    public ResponseEntity<PedidoResponseDTO> confirmaEntrega(@PathVariable UUID id) {
        return ResponseEntity.ok(pedidoService.confirmarEntrega(id));
    }

}
