package com.duoc.sumativauno.sumativauno.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Cuenta de ahorro/préstamo/hipoteca con el interés mensual ya aplicado
 * y el saldo final actualizado.
 */
@Entity
@Table(name = "cuentas_intereses")
@Data
public class CuentaInteres {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long cuentaId;
    private String nombre;
    private BigDecimal saldo;
    private Integer edad;
    private String tipo;
    private BigDecimal tasaAplicada;
    private BigDecimal saldoFinal;
    private boolean anomalia;

    @Column(length = 500)
    private String observacion;
}
