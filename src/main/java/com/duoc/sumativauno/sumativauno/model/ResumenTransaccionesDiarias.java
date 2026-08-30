package com.duoc.sumativauno.sumativauno.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Resumen generado al final del job de transacciones diarias, con los
 * totales y anomalías detectadas en la corrida.
 */
@Entity
@Table(name = "resumen_transacciones_diarias")
@Data
public class ResumenTransaccionesDiarias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime fechaGeneracion;
    private long totalProcesadas;
    private long totalAnomalias;
    private long totalCreditos;
    private long totalDebitos;
    private BigDecimal montoTotalCreditos;
    private BigDecimal montoTotalDebitos;
}
