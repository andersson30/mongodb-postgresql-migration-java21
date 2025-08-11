package com.techtest.integration;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.techtest.MigrationApplication;
import com.techtest.model.Cliente;
import com.techtest.model.Direccion;
import com.techtest.service.PostgreSQLService;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test de integración para la migración completa de MongoDB a PostgreSQL
 * Utiliza Testcontainers para levantar instancias reales de las bases de datos
 */
@SpringBootTest(classes = MigrationApplication.class)
@CamelSpringBootTest
@Testcontainers
public class MigrationIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:5.0")
            .withExposedPorts(27017);

    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:13")
            .withDatabaseName("techtest")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("mongodb.connection.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("postgresql.url", postgresContainer::getJdbcUrl);
        registry.add("postgresql.username", postgresContainer::getUsername);
        registry.add("postgresql.password", postgresContainer::getPassword);
    }

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private ProducerTemplate producerTemplate;

    @Autowired
    private MongoClient mongoClient;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PostgreSQLService postgreSQLService;

    private MongoDatabase mongoDatabase;
    private MongoCollection<Document> clientesCollection;

    @BeforeEach
    void setUp() throws Exception {
        // Configurar MongoDB
        mongoDatabase = mongoClient.getDatabase("techtest");
        clientesCollection = mongoDatabase.getCollection("clientes");
        clientesCollection.drop(); // Limpiar colección

        // Configurar PostgreSQL - crear schema
        setupPostgreSQLSchema();
    }

    private void setupPostgreSQLSchema() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            // Crear tablas
            String createDirecciones = """
                CREATE TABLE IF NOT EXISTS direcciones (
                    id SERIAL PRIMARY KEY,
                    calle VARCHAR(255) NOT NULL,
                    ciudad VARCHAR(100) NOT NULL,
                    pais VARCHAR(100) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;

            String createClientes = """
                CREATE TABLE IF NOT EXISTS clientes (
                    id SERIAL PRIMARY KEY,
                    mongo_id VARCHAR(24) UNIQUE NOT NULL,
                    nombre VARCHAR(255) NOT NULL,
                    correo VARCHAR(255) UNIQUE NOT NULL,
                    direccion_id INTEGER NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_cliente_direccion 
                        FOREIGN KEY (direccion_id) 
                        REFERENCES direcciones(id) 
                        ON DELETE RESTRICT 
                        ON UPDATE CASCADE
                )
                """;

            // Crear función upsert_direccion
            String createUpsertDireccion = """
                CREATE OR REPLACE FUNCTION upsert_direccion(
                    p_calle VARCHAR(255),
                    p_ciudad VARCHAR(100),
                    p_pais VARCHAR(100)
                ) RETURNS INTEGER AS $$
                DECLARE
                    direccion_id INTEGER;
                BEGIN
                    SELECT id INTO direccion_id
                    FROM direcciones
                    WHERE calle = p_calle AND ciudad = p_ciudad AND pais = p_pais;
                    
                    IF direccion_id IS NULL THEN
                        INSERT INTO direcciones (calle, ciudad, pais)
                        VALUES (p_calle, p_ciudad, p_pais)
                        RETURNING id INTO direccion_id;
                    END IF;
                    
                    RETURN direccion_id;
                END;
                $$ LANGUAGE plpgsql;
                """;

            // Crear función upsert_cliente
            String createUpsertCliente = """
                CREATE OR REPLACE FUNCTION upsert_cliente(
                    p_mongo_id VARCHAR(24),
                    p_nombre VARCHAR(255),
                    p_correo VARCHAR(255),
                    p_calle VARCHAR(255),
                    p_ciudad VARCHAR(100),
                    p_pais VARCHAR(100)
                ) RETURNS INTEGER AS $$
                DECLARE
                    cliente_id INTEGER;
                    direccion_id INTEGER;
                    existing_cliente_id INTEGER;
                BEGIN
                    direccion_id := upsert_direccion(p_calle, p_ciudad, p_pais);
                    
                    SELECT id INTO existing_cliente_id
                    FROM clientes
                    WHERE mongo_id = p_mongo_id;
                    
                    IF existing_cliente_id IS NOT NULL THEN
                        UPDATE clientes
                        SET nombre = p_nombre,
                            correo = p_correo,
                            direccion_id = direccion_id,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = existing_cliente_id;
                        
                        cliente_id := existing_cliente_id;
                    ELSE
                        INSERT INTO clientes (mongo_id, nombre, correo, direccion_id)
                        VALUES (p_mongo_id, p_nombre, p_correo, direccion_id)
                        RETURNING id INTO cliente_id;
                    END IF;
                    
                    RETURN cliente_id;
                END;
                $$ LANGUAGE plpgsql;
                """;

            conn.prepareStatement(createDirecciones).execute();
            conn.prepareStatement(createClientes).execute();
            conn.prepareStatement(createUpsertDireccion).execute();
            conn.prepareStatement(createUpsertCliente).execute();
        }
    }

    @Test
    void testMigrationSingleClient() throws Exception {
        // Preparar datos de prueba en MongoDB
        ObjectId mongoId = new ObjectId();
        Document clienteDoc = new Document()
                .append("_id", mongoId)
                .append("nombre", "Juan Pérez Test")
                .append("correo", "juan.test@email.com")
                .append("direccion", new Document()
                        .append("calle", "Calle Test 123")
                        .append("ciudad", "Madrid")
                        .append("pais", "España"));

        clientesCollection.insertOne(clienteDoc);

        // Ejecutar migración usando Camel
        producerTemplate.sendBody("direct:migrate-single-client", clienteDoc);

        // Verificar que el cliente fue migrado a PostgreSQL
        try (Connection conn = dataSource.getConnection()) {
            String sql = """
                SELECT c.mongo_id, c.nombre, c.correo, d.calle, d.ciudad, d.pais
                FROM clientes c
                JOIN direcciones d ON c.direccion_id = d.id
                WHERE c.mongo_id = ?
                """;

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, mongoId.toString());
            ResultSet rs = stmt.executeQuery();

            assertTrue(rs.next(), "Cliente debe existir en PostgreSQL");
            assertEquals(mongoId.toString(), rs.getString("mongo_id"));
            assertEquals("Juan Pérez Test", rs.getString("nombre"));
            assertEquals("juan.test@email.com", rs.getString("correo"));
            assertEquals("Calle Test 123", rs.getString("calle"));
            assertEquals("Madrid", rs.getString("ciudad"));
            assertEquals("España", rs.getString("pais"));
        }
    }

    @Test
    void testUpsertClienteService() throws Exception {
        // Crear cliente de prueba
        Direccion direccion = new Direccion("Av. Test 456", "Buenos Aires", "Argentina");
        Cliente cliente = new Cliente("507f1f77bcf86cd799439011", "María García Test", "maria.test@email.com", direccion);

        // Ejecutar upsert
        Integer clienteId = postgreSQLService.upsertCliente(cliente);

        assertNotNull(clienteId, "ID del cliente no debe ser null");
        assertTrue(clienteId > 0, "ID del cliente debe ser positivo");

        // Verificar que el cliente existe
        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT COUNT(*) FROM clientes WHERE mongo_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, cliente.mongoId());
            ResultSet rs = stmt.executeQuery();

            rs.next();
            assertEquals(1, rs.getInt(1), "Debe existir exactamente un cliente");
        }

        // Probar actualización (mismo mongo_id)
        Cliente clienteActualizado = new Cliente(cliente.mongoId(), "María García Actualizada", "maria.actualizada@email.com", cliente.direccion());

        Integer clienteIdActualizado = postgreSQLService.upsertCliente(clienteActualizado);
        assertEquals(clienteId, clienteIdActualizado, "El ID debe ser el mismo en actualización");
    }

    @Test
    void testMigrationWithDuplicateAddress() throws Exception {
        // Crear dos clientes con la misma dirección
        Direccion direccionCompartida = new Direccion("Calle Compartida 123", "Ciudad Test", "País Test");
        Cliente cliente1 = new Cliente("507f1f77bcf86cd799439021", "Cliente Uno", "cliente1@email.com", direccionCompartida);
        Cliente cliente2 = new Cliente("507f1f77bcf86cd799439022", "Cliente Dos", "cliente2@email.com", direccionCompartida);

        // Migrar ambos clientes
        postgreSQLService.upsertCliente(cliente1);
        postgreSQLService.upsertCliente(cliente2);

        // Verificar que solo hay una dirección pero dos clientes
        try (Connection conn = dataSource.getConnection()) {
            // Contar direcciones
            String sqlDirecciones = "SELECT COUNT(*) FROM direcciones WHERE calle = 'Calle Compartida 123'";
            PreparedStatement stmtDir = conn.prepareStatement(sqlDirecciones);
            ResultSet rsDir = stmtDir.executeQuery();
            rsDir.next();
            assertEquals(1, rsDir.getInt(1), "Debe haber solo una dirección");

            // Contar clientes
            String sqlClientes = """
                SELECT COUNT(*) FROM clientes c
                JOIN direcciones d ON c.direccion_id = d.id
                WHERE d.calle = 'Calle Compartida 123'
                """;
            PreparedStatement stmtCli = conn.prepareStatement(sqlClientes);
            ResultSet rsCli = stmtCli.executeQuery();
            rsCli.next();
            assertEquals(2, rsCli.getInt(1), "Debe haber dos clientes con la misma dirección");
        }
    }

    @Test
    void testPostgreSQLConnectionVerification() {
        boolean isConnected = postgreSQLService.verificarConexion();
        assertTrue(isConnected, "La conexión a PostgreSQL debe ser exitosa");
    }

    @Test
    void testStatistics() {
        String estadisticas = postgreSQLService.obtenerEstadisticas();
        assertNotNull(estadisticas, "Las estadísticas no deben ser null");
        assertTrue(estadisticas.contains("Estadísticas de migración"), 
                   "Las estadísticas deben contener el texto esperado");
    }
}
