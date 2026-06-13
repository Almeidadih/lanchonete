package com.lanchonete.domain.entity;

import com.lanchonete.domain.enums.CategoriaProduto;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "produtos")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@ToString
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "O nome do produto é obrigatório")
    @Column(nullable = false)
    private String nome;

    @Column(length = 500)
    private String descricao;

    @NotNull(message = "O preço é obrigatório")
    @Positive(message = "O preço deve ser maior que zero")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @NotNull(message = "A categoria é obrigatória")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoriaProduto categoria;

    @Column(nullable = false)
    @Builder.Default
    private Boolean disponivel = true ;
}
