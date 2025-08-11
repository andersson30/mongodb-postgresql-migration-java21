-- PostgreSQL Test Data - Insertar datos de prueba para validar funciones
-- Ejecutar con: psql -U postgres -d techtest -f scripts/postgresql/03-test-data.sql

\c techtest;

-- Limpiar datos existentes
SELECT limpiar_datos_prueba();

-- Insertar datos de prueba usando las funciones upsert
SELECT 'Insertando datos de prueba...' as mensaje;

-- Datos de prueba que simulan la migración desde MongoDB
SELECT upsert_cliente(
    '507f1f77bcf86cd799439011',
    'Juan Pérez Test',
    'juan.test@email.com',
    'Calle Mayor 123',
    'Madrid',
    'España'
);

SELECT upsert_cliente(
    '507f1f77bcf86cd799439012',
    'María García Test',
    'maria.test@email.com',
    'Av. Libertador 456',
    'Buenos Aires',
    'Argentina'
);

SELECT upsert_cliente(
    '507f1f77bcf86cd799439013',
    'Carlos Rodríguez Test',
    'carlos.test@email.com',
    'Rua das Flores 789',
    'São Paulo',
    'Brasil'
);

-- Insertar cliente con dirección duplicada para probar la función upsert_direccion
SELECT upsert_cliente(
    '507f1f77bcf86cd799439014',
    'Ana Martínez Test',
    'ana.test@email.com',
    'Calle Mayor 123',  -- Misma dirección que Juan
    'Madrid',
    'España'
);

-- Probar actualización de cliente existente
SELECT upsert_cliente(
    '507f1f77bcf86cd799439011',  -- Mismo mongo_id que Juan
    'Juan Pérez Actualizado',
    'juan.actualizado@email.com',
    'Nueva Calle 999',
    'Barcelona',
    'España'
);

-- Mostrar resultados
SELECT 'Datos insertados. Verificando resultados...' as mensaje;

-- Mostrar todos los clientes
SELECT 'CLIENTES:' as tabla;
SELECT c.id, c.mongo_id, c.nombre, c.correo, 
       d.calle, d.ciudad, d.pais
FROM clientes c
JOIN direcciones d ON c.direccion_id = d.id
ORDER BY c.id;

-- Mostrar todas las direcciones
SELECT 'DIRECCIONES:' as tabla;
SELECT * FROM direcciones ORDER BY id;

-- Probar procedimiento almacenado - clientes por país
SELECT 'CLIENTES DE ESPAÑA:' as consulta;
SELECT * FROM obtener_clientes_por_pais('España');

SELECT 'CLIENTES DE ARGENTINA:' as consulta;
SELECT * FROM obtener_clientes_por_pais('Argentina');

-- Mostrar estadísticas
SELECT 'ESTADÍSTICAS:' as consulta;
SELECT * FROM obtener_estadisticas_migracion();

-- Verificar integridad referencial
SELECT 'VERIFICACIÓN DE INTEGRIDAD:' as consulta;
SELECT 
    'Clientes sin dirección' as tipo,
    COUNT(*) as cantidad
FROM clientes c
LEFT JOIN direcciones d ON c.direccion_id = d.id
WHERE d.id IS NULL

UNION ALL

SELECT 
    'Direcciones no utilizadas' as tipo,
    COUNT(*) as cantidad
FROM direcciones d
LEFT JOIN clientes c ON d.id = c.direccion_id
WHERE c.id IS NULL;

SELECT 'Datos de prueba insertados y validados exitosamente' as resultado;
