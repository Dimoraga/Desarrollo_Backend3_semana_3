package com.duoc.sumativauno.sumativauno.batch.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

/**
 * Los tres CSV de origen mezclan formatos de fecha (yyyy-MM-dd, dd-MM-yyyy,
 * dd/MM/yyyy, yyyy/MM/dd) e incluyen valores directamente inválidos
 * (ej. "2024-13-01"). Se intenta cada formato conocido antes de declarar
 * el valor como no parseable.
 */
public final class FechaUtils {

    private static final List<DateTimeFormatter> FORMATOS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"));

    private FechaUtils() {
    }

    public static Optional<LocalDate> parsear(String valor) {
        if (valor == null || valor.isBlank()) {
            return Optional.empty();
        }
        String limpio = valor.trim();
        for (DateTimeFormatter formato : FORMATOS) {
            try {
                return Optional.of(LocalDate.parse(limpio, formato));
            } catch (DateTimeParseException ignored) {
                // se prueba con el siguiente formato soportado
            }
        }
        return Optional.empty();
    }
}
