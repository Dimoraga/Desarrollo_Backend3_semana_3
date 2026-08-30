package com.duoc.sumativauno.sumativauno.batch.transacciones;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Segundo paso del job de transacciones diarias: agrega lo que se acaba
 * de cargar en la tabla transacciones (ya limpiada al inicio del job) y
 * deja un resumen persistido para el reporte de anomalías.
 */
public class ResumenTransaccionesDiariasTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(ResumenTransaccionesDiariasTasklet.class);

    private final JdbcTemplate jdbcTemplate;

    public ResumenTransaccionesDiariasTasklet(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        long total = contarLong("SELECT COUNT(*) FROM transacciones");
        long anomalias = contarLong("SELECT COUNT(*) FROM transacciones WHERE anomalia = true");
        long creditos = contarLong("SELECT COUNT(*) FROM transacciones WHERE tipo = 'credito'");
        long debitos = contarLong("SELECT COUNT(*) FROM transacciones WHERE tipo = 'debito'");
        BigDecimal montoCreditos = sumar("SELECT COALESCE(SUM(monto),0) FROM transacciones WHERE tipo = 'credito'");
        BigDecimal montoDebitos = sumar("SELECT COALESCE(SUM(monto),0) FROM transacciones WHERE tipo = 'debito'");

        jdbcTemplate.update(
                "INSERT INTO resumen_transacciones_diarias "
                        + "(fecha_generacion, total_procesadas, total_anomalias, total_creditos, total_debitos, monto_total_creditos, monto_total_debitos) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                LocalDateTime.now(), total, anomalias, creditos, debitos, montoCreditos, montoDebitos);

        log.info("Resumen diario de transacciones -> procesadas: {}, anomalías: {}, créditos: {}, débitos: {}",
                total, anomalias, creditos, debitos);
        return RepeatStatus.FINISHED;
    }

    private long contarLong(String sql) {
        Long valor = jdbcTemplate.queryForObject(sql, Long.class);
        return valor == null ? 0L : valor;
    }

    private BigDecimal sumar(String sql) {
        BigDecimal valor = jdbcTemplate.queryForObject(sql, BigDecimal.class);
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
