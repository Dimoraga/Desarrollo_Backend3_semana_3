package com.duoc.sumativauno.sumativauno.dto;

import lombok.Data;

/**
 * Representa una fila cruda del archivo transacciones.csv.
 * Todos los campos se mantienen como String para que las inconsistencias
 * del origen (fechas mal escritas, montos vacíos, etc.) se validen y
 * corrijan explícitamente en el ItemProcessor, en vez de fallar en la lectura.
 */
@Data
public class TransaccionCsv {

    private String id;
    private String fecha;
    private String monto;
    private String tipo;
}
