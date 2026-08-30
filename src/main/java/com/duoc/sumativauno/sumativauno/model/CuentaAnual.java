package com.duoc.sumativauno.sumativauno.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Estado de cuenta anual compilado por cuenta a partir de sus movimientos,
 * usado como informe detallado para auditorías.
 */
@Entity
@Table(name = "estados_cuenta_anual")
@Data
public class CuentaAnual {

    @Id
    private Long cuentaId;

    private BigDecimal totalDepositos;
    private BigDecimal totalRetiros;
    private BigDecimal totalCompras;
    private BigDecimal totalPagos;
    private BigDecimal saldoNeto;
    private long cantidadMovimientos;
    private long cantidadAnomalias;
    private LocalDateTime fechaGeneracion;
}
