package com.lanchonete.config;

import org.apache.catalina.Executor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Value("${async.thread-pool.core-size:2}")
    private int coreSize;

    @Value("${async.thread-pool.mac-size:6}")
    private int maxSize;

    @Value("${async.thread-pool.queue-capacity:50}")
    private int queueCapacity;

    @Value("${async.thread-pool.thread-name-prefix:lanchonete-async-}")
    private String threadNamePrefix;

    /**
     * Pool de threads para @Async — usado no WebSocketNotificationService.
     *
     * Nome "taskExecutor" é o bean padrão buscado pelo Spring para @Async.
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);

        // Politica ap atingir limite: a thread chamadora executa a tarefa
        // (evita rejeitar notificações sob carga externa)
        executor.setRejectedExecutionHandler(
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        // Aguarda tarefas em andamento ao desligar a applicação (graceful shutdown)
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();
        return (Executor) executor;
    }

}
