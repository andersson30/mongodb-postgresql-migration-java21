package com.techtest.service;

import com.techtest.model.Cliente;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Servicio para operaciones con PostgreSQL
 * Utiliza las funciones PL/pgSQL para insertar/actualizar datos
 */
@Service
public class PostgreSQLService {

    private static final Logger logger = LoggerFactory.getLogger(PostgreSQLService.class);

    @Autowired
    private DataSource dataSource;

    /**
     * Inserta o actualiza un cliente usando la función PL/pgSQL upsert_cliente
     * 
     * @param cliente Cliente a insertar/actualizar
     * @return ID del cliente en PostgreSQL
     * @throws SQLException Si hay error en la operación
     */
    public Integer upsertCliente(Cliente cliente) throws SQLException {
        logger.info("Iniciando upsert para cliente: {}", cliente.nombre());
        
        String sql = "{ ? = call upsert_cliente(?, ?, ?, ?, ?, ?) }";
        
        try (Connection conn = dataSource.getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {
            
            // Configurar parámetros de salida
            stmt.registerOutParameter(1, Types.INTEGER);
            
            // Configurar parámetros de entrada
            stmt.setString(2, cliente.mongoId());
            stmt.setString(3, cliente.nombre());
            stmt.setString(4, cliente.correo());
            stmt.setString(5, cliente.direccion().calle());
            stmt.setString(6, cliente.direccion().ciudad());
            stmt.setString(7, cliente.direccion().pais());
            
            // Ejecutar función
            stmt.execute();
            
            // Obtener ID del cliente
            Integer clienteId = stmt.getInt(1);
            
            logger.info("Cliente procesado exitosamente. ID PostgreSQL: {}", clienteId);
            
            return clienteId;
            
        } catch (SQLException e) {
            logger.error("Error al ejecutar upsert_cliente para {}: {}", 
                        cliente.nombre(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Obtiene estadísticas de la migración
     * 
     * @return String con las estadísticas
     */
    public String obtenerEstadisticas() {
        logger.info("Obteniendo estadísticas de migración");
        
        String sql = "SELECT * FROM obtener_estadisticas_migracion()";
        
        try (Connection conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql);
             var rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                long totalClientes = rs.getLong("total_clientes");
                long totalDirecciones = rs.getLong("total_direcciones");
                long paisesUnicos = rs.getLong("paises_unicos");
                long ciudadesUnicas = rs.getLong("ciudades_unicas");
                
                String estadisticas = String.format(
                    "Estadísticas de migración: %d clientes, %d direcciones, %d países, %d ciudades",
                    totalClientes, totalDirecciones, paisesUnicos, ciudadesUnicas
                );
                
                logger.info(estadisticas);
                return estadisticas;
            }
            
        } catch (SQLException e) {
            logger.error("Error al obtener estadísticas: {}", e.getMessage(), e);
        }
        
        return "Error al obtener estadísticas";
    }

    /**
     * Verifica la conectividad con PostgreSQL
     * 
     * @return true si la conexión es exitosa
     */
    public boolean verificarConexion() {
        try (Connection conn = dataSource.getConnection()) {
            boolean isValid = conn.isValid(5);
            logger.info("Verificación de conexión PostgreSQL: {}", isValid ? "OK" : "FAILED");
            return isValid;
        } catch (SQLException e) {
            logger.error("Error al verificar conexión PostgreSQL: {}", e.getMessage());
            return false;
        }
    }
}
