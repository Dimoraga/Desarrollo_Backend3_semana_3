package com.duoc.sumativauno.sumativauno.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Transacción diaria ya validada/corregida, lista para el reporte de anomalías.
 */
@Entity
@Table(name = "transacciones")
@Data
public class Transaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long transaccionId;

    private LocalDate fecha;

    private String fechaOriginal;

    private BigDecimal monto;

    private String tipo;

    private boolean anomalia;

    @Column(length = 500)
    private String observacion;
}
