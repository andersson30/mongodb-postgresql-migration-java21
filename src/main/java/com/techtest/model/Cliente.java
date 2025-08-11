package com.techtest.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Modelo de datos para Cliente usando Java Record
 * Representa tanto el documento MongoDB como la entidad PostgreSQL
 * 
 * @param mongoId   ID único del documento en MongoDB
 * @param nombre    Nombre del cliente
 * @param correo    Email del cliente
 * @param direccion Dirección del cliente
 */
public record Cliente(
    @JsonProperty("_id") String mongoId,
    @JsonProperty("nombre") String nombre,
    @JsonProperty("correo") String correo,
    @JsonProperty("direccion") Direccion direccion
) {
    
    /**
     * Constructor de validación para el record
     */
    public Cliente {
        if (mongoId == null || mongoId.trim().isEmpty()) {
            throw new IllegalArgumentException("El mongoId no puede ser null o vacío");
        }
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede ser null o vacío");
        }
        if (correo == null || correo.trim().isEmpty()) {
            throw new IllegalArgumentException("El correo no puede ser null o vacío");
        }
        if (direccion == null) {
            throw new IllegalArgumentException("La dirección no puede ser null");
        }
    }
    
    /**
     * Constructor para Jackson deserialization
     */
    @JsonCreator
    public static Cliente of(
            @JsonProperty("_id") String mongoId,
            @JsonProperty("nombre") String nombre,
            @JsonProperty("correo") String correo,
            @JsonProperty("direccion") Direccion direccion) {
        return new Cliente(mongoId, nombre, correo, direccion);
    }
}
