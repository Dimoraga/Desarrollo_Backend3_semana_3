package com.duoc.sumativauno.sumativauno.batch.cuentaanual;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Compila, por cada cuenta con movimientos en movimientos_cuenta_anual, un
 * estado de cuenta anual (totales por tipo de movimiento, saldo neto y
 * cantidad de anomalías detectadas) para auditorías.
 */
public class EstadoCuentaAnualTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(EstadoCuentaAnualTasklet.class);

    private final JdbcTemplate jdbcTemplate;

    public EstadoCuentaAnualTasklet(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        List<Long> cuentas = jdbcTemplate.queryForList(
                "SELECT DISTINCT cuenta_id FROM movimientos_cuenta_anual", Long.class);

        for (Long cuentaId : cuentas) {
            BigDecimal depositos = sumarPorTipo(cuentaId, "deposito");
            BigDecimal retiros = sumarPorTipo(cuentaId, "retiro");
            BigDecimal compras = sumarPorTipo(cuentaId, "compra");
            BigDecimal pagos = sumarPorTipo(cuentaId, "pago");
            BigDecimal saldoNeto = depositos.subtract(retiros).subtract(compras).subtract(pagos);
            long cantidadMovimientos = contar(cuentaId,
                    "SELECT COUNT(*) FROM movimientos_cuenta_anual WHERE cuenta_id = ?");
            long cantidadAnomalias = contar(cuentaId,
                    "SELECT COUNT(*) FROM movimientos_cuenta_anual WHERE cuenta_id = ? AND anomalia = true");

            jdbcTemplate.update(
                    "INSERT INTO estados_cuenta_anual "
                            + "(cuenta_id, total_depositos, total_retiros, total_compras, total_pagos, saldo_neto, cantidad_movimientos, cantidad_anomalias, fecha_generacion) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    cuentaId, depositos, retiros, compras, pagos, saldoNeto, cantidadMovimientos,
                    cantidadAnomalias, LocalDateTime.now());
        }

        log.info("Generados {} estados de cuenta anuales para auditoría", cuentas.size());
        return RepeatStatus.FINISHED;
    }

    private BigDecimal sumarPorTipo(Long cuentaId, String tipo) {
        BigDecimal valor = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(monto),0) FROM movimientos_cuenta_anual WHERE cuenta_id = ? AND tipo_movimiento = ?",
                BigDecimal.class, cuentaId, tipo);
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private long contar(Long cuentaId, String sql) {
        Long valor = jdbcTemplate.queryForObject(sql, Long.class, cuentaId);
        return valor == null ? 0L : valor;
    }
}
