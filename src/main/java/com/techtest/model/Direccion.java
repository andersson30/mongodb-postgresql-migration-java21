package com.techtest.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Modelo de datos para Dirección usando Java Record
 * Representa el documento embebido en MongoDB y la tabla normalizada en PostgreSQL
 * 
 * @param calle  La calle de la dirección
 * @param ciudad La ciudad de la dirección  
 * @param pais   El país de la dirección
 */
public record Direccion(
    @JsonProperty("calle") String calle,
    @JsonProperty("ciudad") String ciudad, 
    @JsonProperty("pais") String pais
) {
    
    /**
     * Constructor de validación para el record
     * Permite valores null para flexibilidad en casos de datos incompletos
     */
    public Direccion {
        // Validación básica - solo verificamos que no sean strings vacías si no son null
        if (calle != null && calle.trim().isEmpty()) {
            throw new IllegalArgumentException("La calle no puede ser una cadena vacía");
        }
        if (ciudad != null && ciudad.trim().isEmpty()) {
            throw new IllegalArgumentException("La ciudad no puede ser una cadena vacía");
        }
        if (pais != null && pais.trim().isEmpty()) {
            throw new IllegalArgumentException("El país no puede ser una cadena vacía");
        }
    }
    
    /**
     * Constructor para Jackson deserialization
     */
    @JsonCreator
    public static Direccion of(
            @JsonProperty("calle") String calle,
            @JsonProperty("ciudad") String ciudad,
            @JsonProperty("pais") String pais) {
        return new Direccion(calle, ciudad, pais);
    }
}
