package com.duoc.sumativauno.sumativauno.batch.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.listener.SkipListener;

/**
 * Deja constancia en el log de cada registro omitido por la política de
 * skip, para que las omisiones sean auditables y no pasen desapercibidas.
 */
public class LoggingSkipListener<T, S> implements SkipListener<T, S> {

    private static final Logger log = LoggerFactory.getLogger(LoggingSkipListener.class);

    private final String nombreJob;

    public LoggingSkipListener(String nombreJob) {
        this.nombreJob = nombreJob;
    }

    @Override
    public void onSkipInRead(Throwable t) {
        log.warn("[{}] Registro omitido durante la lectura: {}", nombreJob, t.getMessage());
    }

    @Override
    public void onSkipInProcess(T item, Throwable t) {
        log.warn("[{}] Registro omitido durante el procesamiento ({}): {}", nombreJob, item, t.getMessage());
    }

    @Override
    public void onSkipInWrite(S item, Throwable t) {
        log.warn("[{}] Registro omitido durante la escritura ({}): {}", nombreJob, item, t.getMessage());
    }
}
