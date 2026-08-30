package com.duoc.sumativauno.sumativauno.batch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Política de escalamiento elegida: step multi-thread con un pool de
 * ejecución compartido, en lugar de particionamiento.
 *
 * Cada uno de los tres CSV es un único archivo plano de ~1000 filas; el
 * particionamiento tendría sentido si el volumen fuera mucho mayor o si
 * los datos ya vinieran repartidos en múltiples recursos/rangos de clave,
 * pero aquí solo agregaría complejidad (partitioner, agregación de
 * resultados de cada partición) sin beneficio real de throughput. Un step
 * multi-thread con un lector sincronizado (SynchronizedItemStreamReader)
 * y un writer stateless (JdbcBatchItemWriter) es suficiente para paralelizar
 * el procesamiento/validación de cada chunk.
 *
 * Tamaños elegidos: corePoolSize=4 / maxPoolSize=8 para no sobre-suscribir
 * una máquina de desarrollo típica, con una cola de 100 chunks en espera
 * antes de bloquear al step.
 */
@Configuration
public class BatchConcurrencyConfig {

    @Bean
    public ThreadPoolTaskExecutor batchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("batch-worker-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }
}
