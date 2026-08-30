package com.duoc.sumativauno.sumativauno.batch.transacciones;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.batch.infrastructure.item.ItemProcessor;

import com.duoc.sumativauno.sumativauno.batch.util.FechaUtils;
import com.duoc.sumativauno.sumativauno.batch.util.NumeroUtils;
import com.duoc.sumativauno.sumativauno.dto.TransaccionCsv;
import com.duoc.sumativauno.sumativauno.model.Transaccion;

/**
 * Valida y corrige cada fila de transacciones.csv:
 * - id ausente/no numérico: registro descartado (no se puede auditar sin id).
 * - fecha en cualquiera de los formatos soportados; si no calza con ninguno
 *   (ej. "2024-13-01") se marca como anomalía y se conserva el valor original.
 * - monto ausente se corrige a 0; monto negativo se marca como anomalía pero
 *   se conserva (podría representar un ajuste/reverso real).
 * - tipo fuera de {credito, debito} (valores "invalid", "desconocido", vacíos)
 *   se marca como anomalía.
 */
public class TransaccionItemProcessor implements ItemProcessor<TransaccionCsv, Transaccion> {

    private static final Set<String> TIPOS_VALIDOS = Set.of("credito", "debito");

    @Override
    public Transaccion process(TransaccionCsv item) {
        Long id = NumeroUtils.parseLong(item.getId());
        if (id == null) {
            return null;
        }

        List<String> problemas = new ArrayList<>();

        LocalDate fecha = FechaUtils.parsear(item.getFecha()).orElse(null);
        if (fecha == null) {
            problemas.add("Fecha inválida: '" + item.getFecha() + "'");
        }

        BigDecimal monto;
        if (item.getMonto() == null || item.getMonto().isBlank()) {
            monto = BigDecimal.ZERO;
            problemas.add("Monto ausente, se asume 0");
        } else {
            try {
                monto = new BigDecimal(item.getMonto().trim());
            } catch (NumberFormatException ex) {
                monto = BigDecimal.ZERO;
                problemas.add("Monto no numérico: '" + item.getMonto() + "'");
            }
        }
        if (monto.signum() < 0) {
            problemas.add("Monto negativo: " + monto);
        }

        String tipo = item.getTipo() == null ? "" : item.getTipo().trim().toLowerCase();
        if (!TIPOS_VALIDOS.contains(tipo)) {
            problemas.add("Tipo de transacción no reconocido: '" + item.getTipo() + "'");
            tipo = tipo.isEmpty() ? "desconocido" : tipo;
        }

        Transaccion transaccion = new Transaccion();
        transaccion.setTransaccionId(id);
        transaccion.setFecha(fecha);
        transaccion.setFechaOriginal(item.getFecha());
        transaccion.setMonto(monto);
        transaccion.setTipo(tipo);
        transaccion.setAnomalia(!problemas.isEmpty());
        transaccion.setObservacion(String.join("; ", problemas));
        return transaccion;
    }
}
