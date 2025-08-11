# MongoDB - Módulo 1

## Documentos Embebidos vs Referencias

### Documentos Embebidos (Usado en este proyecto)
**Ventajas:**
- **Rendimiento**: Una sola consulta obtiene toda la información del cliente y su dirección
- **Atomicidad**: Las operaciones de escritura son atómicas a nivel de documento
- **Simplicidad**: Modelo de datos más simple, fácil de entender y mantener
- **Consistencia**: No hay riesgo de inconsistencias entre documentos relacionados

**Desventajas:**
- **Duplicación**: Si una dirección es compartida por múltiples clientes, se duplica la información
- **Límite de tamaño**: Los documentos tienen un límite de 16MB en MongoDB
- **Actualizaciones complejas**: Actualizar direcciones requiere modificar múltiples documentos
- **Consultas limitadas**: Más difícil hacer consultas complejas solo sobre direcciones

### Referencias (Alternativa)
**Ventajas:**
- **Normalización**: Evita duplicación de datos
- **Flexibilidad**: Permite relaciones más complejas (muchos a muchos)
- **Consultas independientes**: Se pueden consultar direcciones independientemente
- **Actualizaciones eficientes**: Cambiar una dirección afecta solo un documento

**Desventajas:**
- **Múltiples consultas**: Requiere joins manuales o múltiples consultas
- **Complejidad**: Modelo de datos más complejo
- **Consistencia**: Riesgo de referencias rotas o inconsistencias
- **Rendimiento**: Potencialmente más lento para consultas que necesitan datos relacionados

## Recomendación para este caso
Para el modelo cliente-dirección, los **documentos embebidos** son la mejor opción porque:
1. Cada cliente típicamente tiene una dirección principal
2. Las consultas frecuentes necesitan ambos datos
3. Las direcciones no se comparten entre clientes
4. El modelo es más simple para el caso de uso de migración

## Scripts Disponibles

### 01-create-collection.js
- Crea la colección `clientes`
- Configura índices para optimizar consultas
- Inserta 10 documentos de prueba con datos de diferentes países

### 02-queries.js
- Consultas por país
- Actualización de correo electrónico
- Consultas agregadas para estadísticas
- Ejemplos de operaciones CRUD

## Ejecución
```bash
# Ejecutar creación de datos
mongo mongodb://localhost:27017/techtest scripts/mongodb/01-create-collection.js

# Ejecutar consultas de ejemplo
mongo mongodb://localhost:27017/techtest scripts/mongodb/02-queries.js
```
