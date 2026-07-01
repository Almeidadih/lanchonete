package com.lanchonete.service;

import com.lanchonete.domain.entity.Pedido;
import com.lanchonete.domain.entity.Produto;
import com.lanchonete.domain.enums.CategoriaProduto;
import com.lanchonete.domain.enums.StatusPedido;
import com.lanchonete.dto.PedidoRequestDTO;
import com.lanchonete.dto.PedidoResponseDTO;
import com.lanchonete.exception.OperacaoInvalidaException;
import com.lanchonete.exception.RecursoNaoEncontradoException;
import com.lanchonete.messaging.rabbitmq.producer.PedidoProducer;
import com.lanchonete.repository.PedidoRepository;
import com.lanchonete.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PedidoService - Testes Unitários")
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private PedidoProducer pedidoProducer;

    @InjectMocks
    private PedidoService pedidoService;

    // Produto de teste reutilizado no cenários
    private Produto produtoDisponivel;
    private Produto produtoIndisponivel;

    @BeforeEach
    void setUp(){
        produtoDisponivel = Produto.builder()
                .id(UUID.randomUUID())
                .nome("X-Burguer")
                .preco(new BigDecimal("18.90"))
                .categoria(CategoriaProduto.LANCHE)
                .disponivel(true)
                .build();

        produtoIndisponivel = Produto.builder()
                .id(UUID.randomUUID()).nome("X-Burguer")
                .preco(new BigDecimal("18.90"))
                .categoria(CategoriaProduto.LANCHE)
                .disponivel(false)
                .build();
    }
    // =========================================================
    // Cenários: criação de pedido
    // =========================================================


    @Nested
    @DisplayName("criar()")
    class CriarPedido {
        void deveCriarPedidoComSucessoEPublicarMensagem(){

            // Arrange
            UUID idProduto = UUID.randomUUID();
            UUID idPedido = UUID.randomUUID();

            PedidoRequestDTO dto = new PedidoRequestDTO();
            dto.setProdutoIds(List.of(idProduto));
            dto.setObservacoes("sem cebola");

            Pedido pedidoSalvo = Pedido.builder()
                    .id(idPedido)
                    .produtos(List.of(produtoDisponivel))
                    .status(StatusPedido.RECEBIDO)
                    .dataHoraCriacao(LocalDateTime.now())
                    .observacoes("sem cebola")
                    .build();

            when(produtoRepository.findById(idProduto)).thenReturn(Optional.of(produtoDisponivel));
            when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedidoSalvo);

            // Act
            PedidoResponseDTO resultado = pedidoService.criar(dto);

            // Assert
            assertThat(resultado.getId()).isEqualTo(idPedido);
            assertThat(resultado.getStatus()).isEqualTo(StatusPedido.RECEBIDO);
            assertThat(resultado.getObservacoes()).isEqualTo("sem cebola");

            // Verifica que o evento foi publicado no RabbitMQ
            verify(pedidoProducer, times(1)).publicarNovoPedido(any());
            verify(pedidoRepository, times(1)).save(any(Pedido.class));
        }

        @Test
        @DisplayName("deve lançar 404 quando produto não existir no cardápio")
        void deveLancarExcecaoQuandoProdutoNaoExistir(){
            // Arrange
            UUID idProduto = UUID.randomUUID();
            PedidoRequestDTO dto = new PedidoRequestDTO();
            dto.setProdutoIds(List.of(idProduto)); // ID inexistente

            when(produtoRepository.findById(idProduto)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> pedidoService.criar(dto))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessageContaining("não encontrado no cardápio")
                    .hasMessageContaining(idProduto.toString());

            // Garante que NÃO foi salvo nem publicado
            verify(pedidoRepository,never()).save(any());
            verify(pedidoProducer, never()).publicarNovoPedido(any());
        }

        @Test
        @DisplayName("deve lançar 422 quando produto estiver indisponível")
        void deveLancarExcecaoQuandoProdutoIndisponivel(){
            // Arrange

            UUID meuIdDeTeste = UUID.randomUUID();

            PedidoRequestDTO dto = new PedidoRequestDTO();
            dto.setProdutoIds(List.of(meuIdDeTeste));

            when(produtoRepository.findById(meuIdDeTeste)).thenReturn(Optional.of(produtoIndisponivel));

            // Act & Assert
            assertThatThrownBy(() -> pedidoService.criar(dto))
                    .isInstanceOf(OperacaoInvalidaException.class)
                    .hasMessageContaining("não estão disponíveis no momento");
            verify(pedidoRepository,never()).save(any());
            verify(pedidoProducer, never()).publicarNovoPedido(any());
        }

// =========================================================
        // Cenários: transição de status
        // =========================================================

        @Nested
        @DisplayName("marcarComoPronto()")
        class MarcarComoPronto {

            @Test
            @DisplayName("deve transicionar de EM_PREPARO para PRONTO e publicar no RabbitMQ")
            void deveMarcarComoProntoQuandoEmPreparo() {
                // Arrange
                UUID idPedido = UUID.randomUUID();
                Pedido pedido = Pedido.builder()
                        .id(idPedido)
                        .produtos(List.of(produtoDisponivel))
                        .status(StatusPedido.EM_PREPARO)
                        .dataHoraCriacao(LocalDateTime.now())
                        .build();

                when(pedidoRepository.findById(idPedido)).thenReturn(Optional.of(pedido));
                when(pedidoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                // Act
                PedidoResponseDTO resultado = pedidoService.marcarComoPronto(idPedido);

                // Assert
                assertThat(resultado.getStatus()).isEqualTo(StatusPedido.PRONTO);
                verify(pedidoProducer, times(1)).publicarPedidoPronto(idPedido);
            }

            @Test
            @DisplayName("deve lançar 422 se pedido não estiver EM_PREPARO")
            void deveLancarExcecaoSeStatusNaoForEmPreparo() {
                // Arrange — pedido ainda RECEBIDO
                UUID idPedido = UUID.randomUUID();
                Pedido pedido = Pedido.builder()
                        .id(idPedido)
                        .status(StatusPedido.RECEBIDO)
                        .dataHoraCriacao(LocalDateTime.now())
                        .build();

                when(pedidoRepository.findById(idPedido)).thenReturn(Optional.of(pedido));

                // Act & Assert
                assertThatThrownBy(() -> pedidoService.marcarComoPronto(idPedido))
                        .isInstanceOf(OperacaoInvalidaException.class)
                        .hasMessageContaining("RECEBIDO");

                verify(pedidoProducer, never()).publicarPedidoPronto(any());
            }
        }

        @Nested
        @DisplayName("confirmarEntrega()")
        class ConfirmarEntrega {

            @Test
            @DisplayName("deve transicionar de PRONTO para ENTREGUE")
            void deveConfirmarEntregaQuandoPronto() {
                // Arrange
                UUID idPedido = UUID.randomUUID();
                Pedido pedido = Pedido.builder()
                        .id(idPedido)
                        .produtos(List.of(produtoDisponivel))
                        .status(StatusPedido.PRONTO)
                        .dataHoraCriacao(LocalDateTime.now())
                        .build();

                when(pedidoRepository.findById(idPedido)).thenReturn(Optional.of(pedido));
                when(pedidoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                // Act
                PedidoResponseDTO resultado = pedidoService.confirmarEntrega(idPedido);

                // Assert
                assertThat(resultado.getStatus()).isEqualTo(StatusPedido.ENTREGUE);
            }

            @Test
            @DisplayName("deve lançar 422 ao tentar entregar pedido não PRONTO")
            void deveLancarExcecaoSePedidoNaoPronto() {

                UUID idPedido = UUID.randomUUID();

                Pedido pedido = Pedido.builder()
                        .id(idPedido)
                        .status(StatusPedido.EM_PREPARO)
                        .dataHoraCriacao(LocalDateTime.now())
                        .build();

                when(pedidoRepository.findById(idPedido)).thenReturn(Optional.of(pedido));

                assertThatThrownBy(() -> pedidoService.confirmarEntrega(idPedido))
                        .isInstanceOf(OperacaoInvalidaException.class)
                        .hasMessageContaining("EM_PREPARO");
            }

            @Test
            @DisplayName("deve lançar 404 para pedido inexistente")
            void deveLancar404ParaPedidoInexistente() {
                // Arrange
                UUID idPedido = UUID.randomUUID();

                // Ensinar o mockito a retornar vazio para este ID
                when(pedidoRepository.findById(idPedido)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> pedidoService.confirmarEntrega(idPedido))
                        .isInstanceOf(RecursoNaoEncontradoException.class);
            }
        }

    }


}