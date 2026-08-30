package com.duoc.sumativauno.sumativauno.batch.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.retry.RetryListener;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.Retryable;

/**
 * Deja constancia en el log de cada fallo que dispara un reintento bajo
 * EscrituraTransitoriaRetryPolicy, útil para diagnosticar problemas
 * intermitentes de conexión con MySQL.
 */
public class LoggingRetryListener implements RetryListener {

    private static final Logger log = LoggerFactory.getLogger(LoggingRetryListener.class);

    private final String nombreJob;

    public LoggingRetryListener(String nombreJob) {
        this.nombreJob = nombreJob;
    }

    @Override
    public void onRetryFailure(RetryPolicy retryPolicy, Retryable<?> retryable, Throwable throwable) {
        log.warn("[{}] Intento fallido, se reintentará según la política configurada: {}", nombreJob,
                throwable.getMessage());
    }
}
