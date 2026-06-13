package com.lanchonete.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * CacheManager com configurações individuais por cache.
     * Permite TTL e tamanho diferentes para cada necessidade.
     */
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                cardapioCache(),
                produtosCache()
        ));
        return manager;
    }


    /**
     * Cache do cardápio público (GET /cardapio).
     *
     * - TTL curto (5 min): cardápio pode mudar com disponibilidade
     * - Tamanho pequeno: só armazena 1 lista (chave = "todos")
     * - Invalidado explicitamente quando produto é criado/atualizado/removido
     */
    private CaffeineCache cardapioCache() {
        return new CaffeineCache("cardapio",
                Caffeine.newBuilder()
                        .maximumSize(10)                       // poucos registros distintos
                        .expireAfterWrite(5, TimeUnit.MINUTES) // expira 5 min após escrita
                        .recordStats()                         // expõe hit/miss via Actuator
                        .build()
        );
    }

    /**
     * Cache de produtos individuais (GET /produtos/{id}).
     *
     * - TTL maior (10 min): produtos mudam menos que disponibilidade
     * - maximumSize=500: suporta até 500 produtos diferentes em cache
     * - Invalidado quando produto é atualizado ou removido
     */
    private CaffeineCache produtosCache() {
        return new CaffeineCache("produtos",
                Caffeine.newBuilder()
                        .maximumSize(500)
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .expireAfterAccess(5, TimeUnit.MINUTES) // remove se não acessado em 5 min
                        .recordStats()
                        .build()
        );
    }

}
