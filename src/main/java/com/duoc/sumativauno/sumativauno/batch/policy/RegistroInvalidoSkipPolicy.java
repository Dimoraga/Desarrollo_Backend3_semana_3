package com.duoc.sumativauno.sumativauno.batch.policy;

import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;

/**
 * Política de skip personalizada para los tres jobs: una línea de CSV
 * mal formada (número de columnas incorrecto, etc.) se omite hasta un
 * límite configurable en lugar de abortar el step completo, ya que los
 * problemas de contenido (fechas, montos, tipos) se corrigen o marcan
 * como anomalía dentro del ItemProcessor y no deberían llegar aquí como
 * excepción. Cualquier otra excepción (por ejemplo de acceso a datos)
 * no se salta: la maneja la política de reintentos o hace fallar el step.
 */
public class RegistroInvalidoSkipPolicy implements SkipPolicy {

    private final int limiteSkips;

    public RegistroInvalidoSkipPolicy(int limiteSkips) {
        this.limiteSkips = limiteSkips;
    }

    @Override
    public boolean shouldSkip(Throwable t, long skipCount) throws SkipLimitExceededException {
        if (t instanceof FlatFileParseException) {
            if (skipCount < limiteSkips) {
                return true;
            }
            throw new SkipLimitExceededException(limiteSkips, t);
        }
        return false;
    }
}
