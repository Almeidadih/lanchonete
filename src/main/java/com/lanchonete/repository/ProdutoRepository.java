package com.lanchonete.repository;

import com.lanchonete.domain.entity.Produto;
import com.lanchonete.domain.enums.CategoriaProduto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, UUID> {

    List<Produto> findByDisponivelTrue();

    List<Produto> findByCategoriaAndDisponivelTrue(CategoriaProduto categoria);
}
