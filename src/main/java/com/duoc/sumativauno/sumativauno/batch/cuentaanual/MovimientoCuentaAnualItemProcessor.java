package com.duoc.sumativauno.sumativauno.batch.cuentaanual;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.batch.infrastructure.item.ItemProcessor;

import com.duoc.sumativauno.sumativauno.batch.util.FechaUtils;
import com.duoc.sumativauno.sumativauno.batch.util.NumeroUtils;
import com.duoc.sumativauno.sumativauno.dto.CuentaAnualCsv;
import com.duoc.sumativauno.sumativauno.model.MovimientoCuentaAnual;

/**
 * Valida y normaliza cada fila de cuentas_anuales.csv. El origen mezcla
 * variantes con y sin tilde para el mismo tipo de movimiento (ej.
 * "depósito" / "deposito"), por lo que el tipo se normaliza quitando
 * diacríticos antes de compararlo contra el catálogo válido
 * {deposito, retiro, compra, pago}.
 */
public class MovimientoCuentaAnualItemProcessor implements ItemProcessor<CuentaAnualCsv, MovimientoCuentaAnual> {

    private static final Set<String> TIPOS_VALIDOS = Set.of("deposito", "retiro", "compra", "pago");

    @Override
    public MovimientoCuentaAnual process(CuentaAnualCsv item) {
        Long cuentaId = NumeroUtils.parseLong(item.getCuentaId());
        if (cuentaId == null) {
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

        String tipo = normalizarTipo(item.getTransaccion());
        if (!TIPOS_VALIDOS.contains(tipo)) {
            problemas.add("Tipo de movimiento no reconocido: '" + item.getTransaccion() + "'");
            tipo = tipo.isEmpty() ? "desconocido" : tipo;
        }

        String descripcion = (item.getDescripcion() == null || item.getDescripcion().isBlank())
                ? "Sin descripción" : item.getDescripcion().trim();

        MovimientoCuentaAnual movimiento = new MovimientoCuentaAnual();
        movimiento.setCuentaId(cuentaId);
        movimiento.setFecha(fecha);
        movimiento.setFechaOriginal(item.getFecha());
        movimiento.setTipoMovimiento(tipo);
        movimiento.setMonto(monto);
        movimiento.setDescripcion(descripcion);
        movimiento.setAnomalia(!problemas.isEmpty());
        movimiento.setObservacion(String.join("; ", problemas));
        return movimiento;
    }

    private String normalizarTipo(String valor) {
        if (valor == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(valor.trim().toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildes;
    }
}
