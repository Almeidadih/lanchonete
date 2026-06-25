package com.lanchonete.service;

import com.lanchonete.domain.entity.Pedido;
import com.lanchonete.domain.entity.Produto;
import com.lanchonete.domain.enums.CategoriaProduto;
import com.lanchonete.domain.enums.StatusPedido;
import com.lanchonete.dto.PedidoRequestDTO;
import com.lanchonete.dto.PedidoResponseDTO;
import com.lanchonete.exception.OperacaoInvalidaException;
import com.lanchonete.exception.RecursoNaoEncontradoException;
import com.lanchonete.messaging.PedidoProducer;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
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
                .id(1L)
                .nome("X-Burguer")
                .preco(new BigDecimal("18.90"))
                .categoria(CategoriaProduto.LANCHE)
                .disponivel(true)
                .build();

        produtoIndisponivel = Produto.builder()
                .id(2l).nome("X-Burguer")
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
            PedidoRequestDTO dto = new PedidoRequestDTO();
            dto.setProdutoIds(List.of(1L));
            dto.setObservacoes("sem cebola");

            Pedido pedidoSalvo = Pedido.builder()
                    .id(10L)
                    .produtos(List.of(produtoDisponivel))
                    .status(StatusPedido.RECEBIDO)
                    .dataHoraCriacao(LocalDateTime.now())
                    .observacoes("sem cebola")
                    .build();

            when(produtoRepository.findById(1L)).thenReturn(Optional.of(produtoDisponivel));
            when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedidoSalvo);

            // Act
            PedidoResponseDTO resultado = pedidoService.criar(dto);

            // Assert
            assertThat(resultado.getId()).isEqualTo(10L);
            assertThat(resultado.getStatus()).isEqualTo(StatusPedido.RECEBIDO);
            assertThat(resultado.getObservacoes()).isEqualTo("sem cebola");

            // Verifica que o evento foi publicado no RabbitMQ
            verify(pedidoProducer, times(1)).publicarNovoPedido(10L);
            verify(pedidoRepository, times(1)).save(any(Pedido.class));
        }

        @Test
        @DisplayName("deve lançar 404 quando produto não existir no cardápio")
        void deveLancarExcecaoQuandoProdutoNaoExistir(){
            // Arrange
            PedidoRequestDTO dto = new PedidoRequestDTO();
            dto.setProdutoIds(List.of(999L)); // ID inexistente

            when(produtoRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> pedidoService.criar(dto))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessageContaining("999");

            // Garante que NÃO foi salvo nem publicado
            verify(pedidoRepository,never()).save(any());
            verify(pedidoProducer, never()).publicarNovoPedido(anyLong());
        }

        @Test
        @DisplayName("deve lançar 422 quando produto estiver indisponível")
        void deveLancarExcecaoQuandoProdutoIndisponivel(){
            // Arrange
            PedidoRequestDTO dto = new PedidoRequestDTO();
            dto.setProdutoIds(List.of(2L));

            when(produtoRepository.findById(2L)).thenReturn(Optional.of(produtoIndisponivel));

            // Act & Assert
            assertThatThrownBy(() -> pedidoService.criar(dto))
                    .isInstanceOf(OperacaoInvalidaException.class)
                    .hasMessageContaining("Produto Esgotado");
            verify(pedidoRepository,never()).save(any());
            verify(pedidoProducer, never()).publicarNovoPedido(anyLong());
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
                Pedido pedido = Pedido.builder()
                        .id(1L)
                        .produtos(List.of(produtoDisponivel))
                        .status(StatusPedido.EM_PREPARO)
                        .dataHoraCriacao(LocalDateTime.now())
                        .build();

                when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
                when(pedidoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                // Act
                PedidoResponseDTO resultado = pedidoService.marcarComoPronto(1L);

                // Assert
                assertThat(resultado.getStatus()).isEqualTo(StatusPedido.PRONTO);
                verify(pedidoProducer, times(1)).publicarPedidoPronto(1L);
            }

            @Test
            @DisplayName("deve lançar 422 se pedido não estiver EM_PREPARO")
            void deveLancarExcecaoSeStatusNaoForEmPreparo() {
                // Arrange — pedido ainda RECEBIDO
                Pedido pedido = Pedido.builder()
                        .id(1L)
                        .status(StatusPedido.RECEBIDO)
                        .dataHoraCriacao(LocalDateTime.now())
                        .build();

                when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

                // Act & Assert
                assertThatThrownBy(() -> pedidoService.marcarComoPronto(1L))
                        .isInstanceOf(OperacaoInvalidaException.class)
                        .hasMessageContaining("RECEBIDO");

                verify(pedidoProducer, never()).publicarPedidoPronto(anyLong());
            }
        }

        @Nested
        @DisplayName("confirmarEntrega()")
        class ConfirmarEntrega {

            @Test
            @DisplayName("deve transicionar de PRONTO para ENTREGUE")
            void deveConfirmarEntregaQuandoPronto() {
                // Arrange
                Pedido pedido = Pedido.builder()
                        .id(1L)
                        .produtos(List.of(produtoDisponivel))
                        .status(StatusPedido.PRONTO)
                        .dataHoraCriacao(LocalDateTime.now())
                        .build();

                when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
                when(pedidoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                // Act
                PedidoResponseDTO resultado = pedidoService.confirmarEntrega(1L);

                // Assert
                assertThat(resultado.getStatus()).isEqualTo(StatusPedido.ENTREGUE);
            }

            @Test
            @DisplayName("deve lançar 422 ao tentar entregar pedido não PRONTO")
            void deveLancarExcecaoSePedidoNaoPronto() {
                Pedido pedido = Pedido.builder()
                        .id(1L)
                        .status(StatusPedido.EM_PREPARO)
                        .dataHoraCriacao(LocalDateTime.now())
                        .build();

                when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

                assertThatThrownBy(() -> pedidoService.confirmarEntrega(1L))
                        .isInstanceOf(OperacaoInvalidaException.class)
                        .hasMessageContaining("EM_PREPARO");
            }

            @Test
            @DisplayName("deve lançar 404 para pedido inexistente")
            void deveLancar404ParaPedidoInexistente() {
                when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> pedidoService.confirmarEntrega(99L))
                        .isInstanceOf(RecursoNaoEncontradoException.class);
            }
        }

    }


}