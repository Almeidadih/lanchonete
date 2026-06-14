package com.lanchonete.service;

import com.lanchonete.domain.entity.Pedido;
import com.lanchonete.domain.entity.Produto;
import com.lanchonete.domain.enums.StatusPedido;
import com.lanchonete.dto.PedidoRequestDTO;
import com.lanchonete.dto.PedidoResponseDTO;
import com.lanchonete.exception.OperacaoInvalidaException;
import com.lanchonete.exception.RecursoNaoEncontradoException;
import com.lanchonete.messaging.PedidoProducer;
import com.lanchonete.repository.PedidoRepository;
import com.lanchonete.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProdutoRepository produtoRepository;
    private final PedidoProducer pedidoProducer;

    @Transactional
    public PedidoResponseDTO criar(PedidoRequestDTO dto) {
        log.info("Criando novo pedido com {} produto(s)", dto.getProdutoIds().size());

        // 1. Busca e valida cada produto — lança 404 individualmente se não encontrar
        List<Produto> produtos = dto.getProdutoIds().stream()
                .map(produtoId -> produtoRepository.findById(produtoId)
                        .orElseThrow(() -> new RecursoNaoEncontradoException(
                                "Produto com id " + produtoId + " não encontrado no cardápio")))
                .toList();

        // 2. Verifica disponibilidade de todos os produtos de uma vez
        List<String> indisponiveis = produtos.stream()
                .filter(p -> Boolean.FALSE.equals(p.getDisponivel()))
                .map(Produto::getNome)
                .toList();

        if (!indisponiveis.isEmpty()) {
            throw new OperacaoInvalidaException(
                    "Os seguintes produtos não estão disponíveis no momento: " +
                            String.join(", ", indisponiveis));
        }

        // 3. Cria e persiste o pedido com status inicial RECEBIDO
        Pedido pedido = Pedido.builder()
                .produtos(produtos)
                .status(StatusPedido.RECEBIDO)
                .observacoes(dto.getObservacoes())
                .build();

        pedido = pedidoRepository.save(pedido);
        log.info("✅ Pedido criado | id={} | status=RECEBIDO | itens={}",
                pedido.getId(), produtos.size());

        // 4. Publica evento assíncrono — a cozinha processará via @RabbitListener
        pedidoProducer.publicarNovoPedido(pedido.getId());

        return PedidoResponseDTO.fromEntity(pedido);
    }

    /**
     * Busca um pedido pelo ID para consulta de status e tempo de preparo.
     *
     * @param id ID do pedido
     * @return DTO com status atual e tempo estimado de preparo
     * @throws RecursoNaoEncontradoException se o pedido não existir (HTTP 404)
     */
    public PedidoResponseDTO buscarPorId(UUID id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido", id));
        return PedidoResponseDTO.fromEntity(pedido);
    }

    /**
     * Lista todos os pedidos de um determinado status.
     * Útil para a cozinha visualizar pedidos EM_PREPARO, por exemplo.
     *
     * @param status Status desejado para filtro
     * @return Lista de pedidos no status especificado
     */
    public List<PedidoResponseDTO> listarPorStatus(StatusPedido status) {
        return pedidoRepository.findByStatusOrderByDataHoraCriacaoAsc(status)
                .stream()
                .map(PedidoResponseDTO::fromEntity)
                .toList();
    }

    /**
     * Marca um pedido como PRONTO e notifica via RabbitMQ.
     *
     * Regra de negócio: só pedidos com status EM_PREPARO podem ser marcados como PRONTO.
     * Garante que o fluxo sequencial não seja violado.
     *
     * Após salvar, publica na fila.pedidos.prontos para o atendimento ser notificado.
     *
     * @param id ID do pedido finalizado pela cozinha
     * @return DTO do pedido atualizado
     * @throws RecursoNaoEncontradoException se o pedido não existir (HTTP 404)
     * @throws OperacaoInvalidaException se o status não for EM_PREPARO (HTTP 422)
     */
    @Transactional
    public PedidoResponseDTO marcarComoPronto(UUID id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido", id));

        // Valida transição de status
        if (pedido.getStatus() != StatusPedido.EM_PREPARO) {
            throw new OperacaoInvalidaException(
                    String.format("Transição inválida: pedido está com status '%s'. " +
                                    "Apenas pedidos EM_PREPARO podem ser marcados como PRONTO.",
                            pedido.getStatus()));
        }

        pedido.setStatus(StatusPedido.PRONTO);
        pedido = pedidoRepository.save(pedido);
        log.info("🔔 Pedido {} marcado como PRONTO", id);

        // Notifica o atendimento de forma assíncrona via fila
        pedidoProducer.publicarPedidoPronto(pedido.getId());

        return PedidoResponseDTO.fromEntity(pedido);
    }

    /**
     * Confirma a entrega do pedido ao cliente — status final do ciclo.
     *
     * Regra de negócio: só pedidos PRONTOS podem ser marcados como ENTREGUE.
     *
     * @param id ID do pedido retirado pelo cliente
     * @return DTO do pedido com status ENTREGUE
     * @throws RecursoNaoEncontradoException se o pedido não existir (HTTP 404)
     * @throws OperacaoInvalidaException se o status não for PRONTO (HTTP 422)
     */
    @Transactional
    public PedidoResponseDTO confirmarEntrega(UUID id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido", id));

        if (pedido.getStatus() != StatusPedido.PRONTO) {
            throw new OperacaoInvalidaException(
                    String.format("Transição inválida: pedido está com status '%s'. " +
                                    "Apenas pedidos PRONTOS podem ser marcados como ENTREGUE.",
                            pedido.getStatus()));
        }

        pedido.setStatus(StatusPedido.ENTREGUE);
        pedido = pedidoRepository.save(pedido);
        log.info("🎉 Pedido {} ENTREGUE ao cliente", id);

        return PedidoResponseDTO.fromEntity(pedido);
    }
}
