package com.duoc.sumativauno.sumativauno.batch.policy;

import org.springframework.core.retry.RetryPolicy;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Política de reintento personalizada: solo reintenta fallos transitorios
 * de acceso a datos (timeouts, deadlocks) contra MySQL, hasta 3 intentos
 * con backoff fijo de 500ms. Cualquier otra excepción (por ejemplo un
 * error de validación) no es recuperable con un reintento y se deja
 * fallar el step.
 */
public class EscrituraTransitoriaRetryPolicy implements RetryPolicy {

    private static final int MAX_INTENTOS = 3;
    private static final long BACKOFF_MS = 500L;

    @Override
    public boolean shouldRetry(Throwable throwable) {
        return throwable instanceof TransientDataAccessException
                || throwable instanceof ConcurrencyFailureException;
    }

    @Override
    public BackOff getBackOff() {
        return new FixedBackOff(BACKOFF_MS, MAX_INTENTOS);
    }
}
