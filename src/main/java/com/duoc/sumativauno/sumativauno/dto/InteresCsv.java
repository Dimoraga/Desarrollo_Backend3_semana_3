package com.duoc.sumativauno.sumativauno.dto;

import lombok.Data;

/**
 * Representa una fila cruda del archivo intereses.csv.
 */
@Data
public class InteresCsv {

    private String cuentaId;
    private String nombre;
    private String saldo;
    private String edad;
    private String tipo;
}
