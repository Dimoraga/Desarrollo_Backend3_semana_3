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
 * Movimiento individual (depósito, retiro, compra o pago) ya validado,
 * usado como detalle para compilar el estado de cuenta anual.
 */
@Entity
@Table(name = "movimientos_cuenta_anual")
@Data
public class MovimientoCuentaAnual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long cuentaId;
    private LocalDate fecha;
    private String fechaOriginal;
    private String tipoMovimiento;
    private BigDecimal monto;

    @Column(length = 300)
    private String descripcion;

    private boolean anomalia;

    @Column(length = 500)
    private String observacion;
}
