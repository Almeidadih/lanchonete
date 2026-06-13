package com.lanchonete.repository;

import com.lanchonete.domain.entity.Pedido;
import com.lanchonete.domain.enums.StatusPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido , UUID> {

    List<Pedido>findByStatusOrderByDataHoraCriacaoAsc(StatusPedido status);
}
