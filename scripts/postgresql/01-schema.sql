-- PostgreSQL Schema - Crear tablas con integridad referencial
-- Ejecutar con: psql -U postgres -d techtest -f scripts/postgresql/01-schema.sql

-- Crear base de datos si no existe (ejecutar como superusuario)
CREATE DATABASE techtest;

-- Conectar a la base de datos techtest


-- Eliminar tablas si existen (para re-ejecución del script)
DROP TABLE IF EXISTS clientes CASCADE;
DROP TABLE IF EXISTS direcciones CASCADE;

-- Crear tabla direcciones primero (referenciada por clientes)
CREATE TABLE direcciones (
    id SERIAL PRIMARY KEY,
    calle VARCHAR(255) NOT NULL,
    ciudad VARCHAR(100) NOT NULL,
    pais VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Crear tabla clientes con referencia a direcciones
CREATE TABLE clientes (
    id SERIAL PRIMARY KEY,
    mongo_id VARCHAR(24) UNIQUE NOT NULL, -- Para mapear con MongoDB ObjectId
    nombre VARCHAR(255) NOT NULL,
    correo VARCHAR(255) UNIQUE NOT NULL,
    direccion_id INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Clave foránea con integridad referencial
    CONSTRAINT fk_cliente_direccion 
        FOREIGN KEY (direccion_id) 
        REFERENCES direcciones(id) 
        ON DELETE RESTRICT 
        ON UPDATE CASCADE
);

-- Crear índices para optimizar consultas
CREATE INDEX idx_clientes_mongo_id ON clientes(mongo_id);
CREATE INDEX idx_clientes_correo ON clientes(correo);
CREATE INDEX idx_clientes_nombre ON clientes(nombre);
CREATE INDEX idx_direcciones_pais ON direcciones(pais);
CREATE INDEX idx_direcciones_ciudad ON direcciones(ciudad);

-- Crear trigger para actualizar updated_at automáticamente
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Aplicar trigger a ambas tablas
CREATE TRIGGER update_direcciones_updated_at 
    BEFORE UPDATE ON direcciones 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_clientes_updated_at 
    BEFORE UPDATE ON clientes 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Comentarios para documentar el esquema
COMMENT ON TABLE clientes IS 'Tabla de clientes migrados desde MongoDB';
COMMENT ON TABLE direcciones IS 'Tabla de direcciones normalizadas';
COMMENT ON COLUMN clientes.mongo_id IS 'ID original del documento en MongoDB';
COMMENT ON COLUMN clientes.direccion_id IS 'Referencia a la tabla direcciones';



