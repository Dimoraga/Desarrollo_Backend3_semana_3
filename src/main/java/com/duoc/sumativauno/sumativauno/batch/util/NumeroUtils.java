package com.duoc.sumativauno.sumativauno.batch.util;

/**
 * Conversión tolerante a fallos de campos numéricos de los CSV: en vez de
 * lanzar una excepción que detendría el chunk, devuelve null y deja que el
 * ItemProcessor decida cómo corregir o marcar el registro como anómalo.
 */
public final class NumeroUtils {

    private NumeroUtils() {
    }

    public static Long parseLong(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(valor.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static Integer parseInt(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
