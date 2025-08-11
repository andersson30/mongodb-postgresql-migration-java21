package com.techtest.processor;

import com.techtest.model.Cliente;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test unitario para ClienteProcessor
 */
class ClienteProcessorTest {

    private ClienteProcessor processor;
    private Exchange exchange;

    @BeforeEach
    void setUp() {
        processor = new ClienteProcessor();
        exchange = new DefaultExchange(new DefaultCamelContext());
    }

    @Test
    void testProcessValidDocument() throws Exception {
        // Preparar documento MongoDB de prueba
        ObjectId mongoId = new ObjectId();
        Document direccionDoc = new Document()
                .append("calle", "Calle Test 123")
                .append("ciudad", "Madrid")
                .append("pais", "España");

        Document clienteDoc = new Document()
                .append("_id", mongoId)
                .append("nombre", "Juan Pérez")
                .append("correo", "juan.perez@email.com")
                .append("direccion", direccionDoc);

        // Configurar exchange
        exchange.getIn().setBody(clienteDoc);

        // Ejecutar procesador
        processor.process(exchange);

        // Verificar resultado
        Cliente cliente = exchange.getIn().getBody(Cliente.class);
        
        assertNotNull(cliente, "Cliente no debe ser null");
        assertEquals(mongoId.toString(), cliente.mongoId());
        assertEquals("Juan Pérez", cliente.nombre());
        assertEquals("juan.perez@email.com", cliente.correo());
        
        assertNotNull(cliente.direccion(), "Dirección no debe ser null");
        assertEquals("Calle Test 123", cliente.direccion().calle());
        assertEquals("Madrid", cliente.direccion().ciudad());
        assertEquals("España", cliente.direccion().pais());
    }

    @Test
    void testProcessDocumentWithMissingFields() {
        // Documento con campos faltantes
        Document clienteDoc = new Document()
                .append("_id", new ObjectId())
                .append("nombre", "Cliente Incompleto");
        // Falta correo y dirección

        exchange.getIn().setBody(clienteDoc);

        // Debe lanzar excepción
        assertThrows(RuntimeException.class, () -> processor.process(exchange));
    }

    @Test
    void testProcessDocumentWithNullDireccion() {
        // Documento con dirección null
        Document clienteDoc = new Document()
                .append("_id", new ObjectId())
                .append("nombre", "Cliente Sin Dirección")
                .append("correo", "cliente@email.com")
                .append("direccion", null);

        exchange.getIn().setBody(clienteDoc);

        // Debe lanzar excepción
        assertThrows(RuntimeException.class, () -> processor.process(exchange));
    }

    @Test
    void testProcessDocumentWithIncompleteDireccion() {
        // Documento con dirección incompleta
        Document direccionDoc = new Document()
                .append("calle", "Calle Test")
                .append("ciudad", "Madrid");
        // Falta país

        Document clienteDoc = new Document()
                .append("_id", new ObjectId())
                .append("nombre", "Cliente Dirección Incompleta")
                .append("correo", "cliente@email.com")
                .append("direccion", direccionDoc);

        exchange.getIn().setBody(clienteDoc);

        // Ejecutar procesador
        assertDoesNotThrow(() -> processor.process(exchange));

        // Verificar que se procesa pero con país null
        Cliente cliente = exchange.getIn().getBody(Cliente.class);
        assertNotNull(cliente);
        assertNull(cliente.direccion().pais());
    }

    @Test
    void testProcessDocumentWithSpecialCharacters() throws Exception {
        // Documento con caracteres especiales
        ObjectId mongoId = new ObjectId();
        Document direccionDoc = new Document()
                .append("calle", "Calle José María Azaña, 123 - 2º")
                .append("ciudad", "São Paulo")
                .append("pais", "Brasil");

        Document clienteDoc = new Document()
                .append("_id", mongoId)
                .append("nombre", "José María Rodríguez-González")
                .append("correo", "josé.maría@email.com")
                .append("direccion", direccionDoc);

        exchange.getIn().setBody(clienteDoc);

        // Ejecutar procesador
        processor.process(exchange);

        // Verificar resultado
        Cliente cliente = exchange.getIn().getBody(Cliente.class);
        
        assertEquals("José María Rodríguez-González", cliente.nombre());
        assertEquals("josé.maría@email.com", cliente.correo());
        assertEquals("Calle José María Azaña, 123 - 2º", cliente.direccion().calle());
        assertEquals("São Paulo", cliente.direccion().ciudad());
        assertEquals("Brasil", cliente.direccion().pais());
    }
}
