package com.lanchonete.service;


import com.lanchonete.domain.entity.Produto;
import com.lanchonete.domain.enums.CategoriaProduto;
import com.lanchonete.dto.ProdutoRequestDTO;
import com.lanchonete.dto.ProdutoResponseDTO;
import com.lanchonete.exception.RecursoNaoEncontradoException;
import com.lanchonete.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProdutoService {

    private final ProdutoRepository produtoRepository;

    /**
     * Lista produtos disponíveis com cache.
     *
     * Cache "cardapio", chave "todos":
     * - 1ª chamada: busca no banco, armazena no cache
     * - Chamadas seguintes: retorna do cache sem tocar o banco
     * - TTL: 5 minutos (configurado no CacheConfig)
     */
    @Cacheable(value = "cardapio" , key = "'todos'")
    public List<ProdutoResponseDTO> listarCardapio(){
        log.debug("🗄️ [CACHE MISS] Buscando cardápio no banco de dados");
        return produtoRepository.findByDisponivelTrue()
                .stream()
                .map(ProdutoResponseDTO::fromEntity)
                .toList();
    }

    /**
     * Filtra por categoria com cache separado por categoria.
     * Cache key inclui o nome da categoria para evitar colisão entre caches.
     */
    @Cacheable(value = "cardapio" , key = "'cardapio-' + #categoria.name()")
    public List<ProdutoResponseDTO> listarCategoria(CategoriaProduto categoria){
        log.debug("🗄️ [CACHE MISS] Buscando categoria {} no banco", categoria);
        return produtoRepository.findByCategoriaAndDisponivelTrue(categoria)
                .stream()
                .map(ProdutoResponseDTO::fromEntity)
                .toList();
    }

    /**
     * Lista todos os produtos (admin) — sem cache intencional.
     * Dado administrativo deve ser sempre fresco.
     */
    public List<ProdutoResponseDTO> listarTodos() {
        return produtoRepository.findAll()
                .stream()
                .map(ProdutoResponseDTO::fromEntity)
                .toList();
    }

    /**
     * Busca por ID com cache individual por produto.
     * Cache key = ID do produto.
     */
    @Cacheable(value = "produtos", key = "#id")
    public ProdutoResponseDTO buscarPorId(UUID id) {
        log.debug("🗄️ [CACHE MISS] Buscando produto id={} no banco", id);
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", id));
        return ProdutoResponseDTO.fromEntity(produto);
    }

    /**
     * Cria produto e invalida todo o cache do cardápio.
     *
     * @CacheEvict(allEntries=true) remove todas as entradas do cache "cardapio"
     * porque um novo produto afeta a lista inteira (independente de categoria).
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "cardapio" , allEntries = true),
    })
    public ProdutoResponseDTO criar(ProdutoRequestDTO dto){
        Produto produto = Produto.builder()
                .nome(dto.getNome())
                .descricao(dto.getDescricao())
                .preco(dto.getPreco())
                .categoria(dto.getCategoria())
                .disponivel(dto.getDisponivel() != null ? dto.getDisponivel() : true)
                .build();

            Produto salvo = produtoRepository.save(produto);
            log.info("✅ Produto criado: id={}, nome={} | Cache cardápio invalidado",
                    salvo.getId(), salvo.getNome());
            return ProdutoResponseDTO.fromEntity(salvo);
    }

    /**
     * Atualiza produto e invalida os caches afetados:
     * - "produtos" para o ID específico (entrada individual)
     * - "cardapio" inteiro (lista pode ter mudado)
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "produtos", key = "#id"),
            @CacheEvict(value = "cardapio", allEntries = true),
    })
    public ProdutoResponseDTO atualizar(UUID id, ProdutoRequestDTO dto){
        Produto produto = produtoRepository
                .findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", id));

        produto.setNome(dto.getNome());
        produto.setDescricao(dto.getDescricao());
        produto.setPreco(dto.getPreco());
        produto.setCategoria(dto.getCategoria());
        produto.setDisponivel(dto.getDisponivel() != null ? dto.getDisponivel() : produto.getDisponivel());

        log.info("✅ Produto id={} atualizado | Cache invalidado" , id);
        return ProdutoResponseDTO.fromEntity(produtoRepository.save(produto));
    }
    /**
     * Remove produto e invalida todos os caches relacionados.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "produtos", key = "#id"),
            @CacheEvict(value = "cardapio", allEntries = true),
    })
    public void remover(UUID id){
        if (!produtoRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Produto", id);
        }
        produtoRepository.deleteById(id);
        log.info("🗑️ Produto id={} removido | Cache invalidado" , id);
    }

    /**
     * Altera disponibilidade e invalida caches afetados.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "produtos", key = "#id"),
            @CacheEvict(value = "cardapio", allEntries = true),
    })
    public ProdutoResponseDTO alterarDisponibilidade(UUID id, boolean disponivel) {
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", id));

        produto.setDisponivel(disponivel);
        log.info("✅ Produto id={} disponibilidade={} | Cache invalidado", id, disponivel);
        return ProdutoResponseDTO.fromEntity(produtoRepository.save(produto));
    }

}
