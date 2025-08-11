-- PostgreSQL Functions - Implementar funciones PL/pgSQL
-- Ejecutar con: psql -U postgres -d techtest -f scripts/postgresql/02-functions.sql

\c techtest;

-- Función UPSERT para direcciones
-- Busca si existe una dirección idéntica, si no la crea
CREATE OR REPLACE FUNCTION upsert_direccion(
    p_calle VARCHAR(255),
    p_ciudad VARCHAR(100),
    p_pais VARCHAR(100)
) RETURNS INTEGER AS $$
DECLARE
    v_direccion_id INTEGER;
BEGIN
    -- Buscar dirección existente
    SELECT id INTO v_direccion_id
    FROM direcciones
    WHERE calle = p_calle 
      AND ciudad = p_ciudad 
      AND pais = p_pais;
    
    -- Si no existe, crear nueva dirección
    IF v_direccion_id IS NULL THEN
        INSERT INTO direcciones (calle, ciudad, pais)
        VALUES (p_calle, p_ciudad, p_pais)
        RETURNING id INTO v_direccion_id;
        
        RAISE NOTICE 'Nueva dirección creada con ID: %', v_direccion_id;
    ELSE
        RAISE NOTICE 'Dirección existente encontrada con ID: %', v_direccion_id;
    END IF;
    
    RETURN v_direccion_id;
END;
$$ LANGUAGE plpgsql;

-- Función UPSERT para clientes
-- Inserta nuevo cliente o actualiza existente basado en mongo_id
CREATE OR REPLACE FUNCTION upsert_cliente(
    p_mongo_id VARCHAR(24),
    p_nombre VARCHAR(255),
    p_correo VARCHAR(255),
    p_calle VARCHAR(255),
    p_ciudad VARCHAR(100),
    p_pais VARCHAR(100)
) RETURNS INTEGER AS $$
DECLARE
    v_cliente_id INTEGER;
    v_direccion_id INTEGER;
    v_existing_cliente_id INTEGER;
BEGIN
    -- Primero, obtener o crear la dirección
    v_direccion_id := upsert_direccion(p_calle, p_ciudad, p_pais);
    
    -- Verificar si el cliente ya existe por mongo_id
    SELECT id INTO v_existing_cliente_id
    FROM clientes
    WHERE mongo_id = p_mongo_id;
    
    IF v_existing_cliente_id IS NOT NULL THEN
        -- Cliente existe, actualizar
        UPDATE clientes
        SET nombre = p_nombre,
            correo = p_correo,
            direccion_id = v_direccion_id,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = v_existing_cliente_id;
        
        v_cliente_id := v_existing_cliente_id;
        RAISE NOTICE 'Cliente actualizado con ID: %', v_cliente_id;
    ELSE
        -- Cliente no existe, insertar nuevo
        INSERT INTO clientes (mongo_id, nombre, correo, direccion_id)
        VALUES (p_mongo_id, p_nombre, p_correo, v_direccion_id)
        RETURNING id INTO v_cliente_id;
        
        RAISE NOTICE 'Nuevo cliente creado con ID: %', v_cliente_id;
    END IF;
    
    RETURN v_cliente_id;
EXCEPTION
    WHEN unique_violation THEN
        -- Manejar violación de unicidad en correo
        RAISE EXCEPTION 'Error: El correo % ya existe para otro cliente', p_correo;
    WHEN OTHERS THEN
        -- Manejar otros errores
        RAISE EXCEPTION 'Error al procesar cliente: %', SQLERRM;
END;
$$ LANGUAGE plpgsql;

-- Procedimiento almacenado para obtener clientes por país
CREATE OR REPLACE FUNCTION obtener_clientes_por_pais(p_pais VARCHAR(100))
RETURNS TABLE (
    cliente_id INTEGER,
    mongo_id VARCHAR(24),
    nombre VARCHAR(255),
    correo VARCHAR(255),
    calle VARCHAR(255),
    ciudad VARCHAR(100),
    pais VARCHAR(100),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        c.id as cliente_id,
        c.mongo_id,
        c.nombre,
        c.correo,
        d.calle,
        d.ciudad,
        d.pais,
        c.created_at,
        c.updated_at
    FROM clientes c
    INNER JOIN direcciones d ON c.direccion_id = d.id
    WHERE d.pais = p_pais
    ORDER BY c.nombre;
END;
$$ LANGUAGE plpgsql;

-- Función para obtener estadísticas de migración
CREATE OR REPLACE FUNCTION obtener_estadisticas_migracion()
RETURNS TABLE(
    total_clientes BIGINT,
    total_direcciones BIGINT,
    paises_unicos BIGINT,
    ciudades_unicas BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        (SELECT COUNT(*) FROM clientes)::BIGINT as total_clientes,
        (SELECT COUNT(*) FROM direcciones)::BIGINT as total_direcciones,
        (SELECT COUNT(DISTINCT pais) FROM direcciones)::BIGINT as paises_unicos,
        (SELECT COUNT(DISTINCT ciudad) FROM direcciones)::BIGINT as ciudades_unicas;
END;
$$ LANGUAGE plpgsql;

-- Función auxiliar para limpiar datos de prueba
CREATE OR REPLACE FUNCTION limpiar_datos_prueba()
RETURNS VOID AS $$
BEGIN
    DELETE FROM clientes;
    DELETE FROM direcciones;
    
    -- Reiniciar secuencias
    ALTER SEQUENCE clientes_id_seq RESTART WITH 1;
    ALTER SEQUENCE direcciones_id_seq RESTART WITH 1;
    
    RAISE NOTICE 'Datos de prueba eliminados y secuencias reiniciadas';
END;
$$ LANGUAGE plpgsql;

-- Mostrar funciones creadas
\df+ upsert_direccion
\df+ upsert_cliente
\df+ obtener_clientes_por_pais
\df+ obtener_estadisticas_migracion

SELECT 'Funciones PL/pgSQL creadas exitosamente' as mensaje;
