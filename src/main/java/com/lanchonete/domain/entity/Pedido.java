package com.lanchonete.domain.entity;

import com.lanchonete.domain.enums.StatusPedido;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pedidos")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@ToString(exclude = "itens") // evita loop em relacionamentos
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Lista de itens (produtos) do pedido.
     * Relacionamento ManyToMany com tabela de junção "pedido_produtos".
     */

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "pedido_produtos",
            joinColumns = @JoinColumn(name = "pedido_id"),
            inverseJoinColumns = @JoinColumn(name = "produto_id")
    )
    @Builder.Default
    private List<Produto> produtos = new ArrayList<>();


    /**
     * Status atual do pedido no fluxo de preparação.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusPedido status = StatusPedido.RECEBIDO;

    /**
     * Tempo estimado de preparo em minutos.
     * Calculado pelo listener da cozinha ao iniciar o preparo.
     */
    @Column(name = "tempo_preparo_estimado")
    private Integer tempoPreparoEstimado;

    /**
     * Data e hora em que o pedido foi criado.
     * Preenchido automaticamente pelo Hibernate.
     */
    @CreationTimestamp
    @Column(name = "data_hora_criacao" , nullable = false , updatable = false)
    private LocalDateTime dataHoraCriacao;

    /**
     * Observações do cliente (ex: "sem cebola", "ponto da carne").
     */
    @Column(length = 500)
    private String observacoes;
}
