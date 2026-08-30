package com.duoc.sumativauno.sumativauno.batch.intereses;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.infrastructure.item.ItemProcessor;

import com.duoc.sumativauno.sumativauno.batch.util.NumeroUtils;
import com.duoc.sumativauno.sumativauno.dto.InteresCsv;
import com.duoc.sumativauno.sumativauno.model.CuentaInteres;

/**
 * Valida cada fila de intereses.csv y calcula el saldo final aplicando la
 * tasa mensual que corresponde al tipo de cuenta:
 * - ahorro: 0.4% mensual (0.5% si el titular tiene 65 años o más).
 * - préstamo: 1.8% mensual.
 * - hipoteca: 1.1% mensual.
 * Un tipo no reconocido (valores "-1", "unknown", vacíos) no genera interés
 * y se marca como anomalía; el saldo ausente/negativo se corrige a 0 para
 * el cálculo pero también queda marcado como anomalía.
 */
public class CuentaInteresItemProcessor implements ItemProcessor<InteresCsv, CuentaInteres> {

    private static final BigDecimal TASA_AHORRO = new BigDecimal("0.004");
    private static final BigDecimal BONUS_ADULTO_MAYOR = new BigDecimal("0.001");
    private static final BigDecimal TASA_PRESTAMO = new BigDecimal("0.018");
    private static final BigDecimal TASA_HIPOTECA = new BigDecimal("0.011");
    private static final int EDAD_ADULTO_MAYOR = 65;
    private static final int EDAD_MAXIMA_RAZONABLE = 110;

    @Override
    public CuentaInteres process(InteresCsv item) {
        Long cuentaId = NumeroUtils.parseLong(item.getCuentaId());
        if (cuentaId == null) {
            return null;
        }

        List<String> problemas = new ArrayList<>();

        String nombre = (item.getNombre() == null || item.getNombre().isBlank())
                ? "Desconocido" : item.getNombre().trim();
        if (nombre.equalsIgnoreCase("unknown")) {
            problemas.add("Nombre de titular no disponible");
        }

        BigDecimal saldo;
        if (item.getSaldo() == null || item.getSaldo().isBlank()) {
            saldo = BigDecimal.ZERO;
            problemas.add("Saldo ausente, se asume 0");
        } else {
            try {
                saldo = new BigDecimal(item.getSaldo().trim());
            } catch (NumberFormatException ex) {
                saldo = BigDecimal.ZERO;
                problemas.add("Saldo no numérico: '" + item.getSaldo() + "'");
            }
        }
        boolean saldoInvalido = saldo.signum() < 0;
        if (saldoInvalido) {
            problemas.add("Saldo negativo, se usa 0 como base de cálculo");
        }
        BigDecimal saldoBase = saldoInvalido ? BigDecimal.ZERO : saldo;

        Integer edad = NumeroUtils.parseInt(item.getEdad());
        if (edad == null) {
            problemas.add("Edad ausente");
        } else if (edad < 0 || edad > EDAD_MAXIMA_RAZONABLE) {
            problemas.add("Edad fuera de rango: " + edad);
            edad = null;
        }

        String tipo = item.getTipo() == null ? "" : item.getTipo().trim().toLowerCase();
        BigDecimal tasa;
        switch (tipo) {
            case "ahorro" -> tasa = (edad != null && edad >= EDAD_ADULTO_MAYOR)
                    ? TASA_AHORRO.add(BONUS_ADULTO_MAYOR) : TASA_AHORRO;
            case "prestamo" -> tasa = TASA_PRESTAMO;
            case "hipoteca" -> tasa = TASA_HIPOTECA;
            default -> {
                tasa = BigDecimal.ZERO;
                problemas.add("Tipo de cuenta no reconocido: '" + item.getTipo() + "'");
                tipo = tipo.isEmpty() ? "desconocido" : tipo;
            }
        }

        BigDecimal saldoFinal = saldoBase.add(saldoBase.multiply(tasa)).setScale(2, RoundingMode.HALF_UP);

        CuentaInteres cuenta = new CuentaInteres();
        cuenta.setCuentaId(cuentaId);
        cuenta.setNombre(nombre);
        cuenta.setSaldo(saldo);
        cuenta.setEdad(edad);
        cuenta.setTipo(tipo);
        cuenta.setTasaAplicada(tasa);
        cuenta.setSaldoFinal(saldoFinal);
        cuenta.setAnomalia(!problemas.isEmpty());
        cuenta.setObservacion(String.join("; ", problemas));
        return cuenta;
    }
}
