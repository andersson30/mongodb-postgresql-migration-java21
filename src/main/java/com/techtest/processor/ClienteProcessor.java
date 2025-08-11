package com.techtest.processor;

import com.techtest.model.Cliente;
import com.techtest.model.Direccion;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Procesador para transformar documentos MongoDB a objetos Cliente
 */
@Component
public class ClienteProcessor implements Processor {

    private static final Logger logger = LoggerFactory.getLogger(ClienteProcessor.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        Document mongoDoc = exchange.getIn().getBody(Document.class);
        
        logger.info("Procesando documento MongoDB: {}", mongoDoc.getObjectId("_id"));
        
        try {
            // Extraer datos del documento MongoDB
            String mongoId = mongoDoc.getObjectId("_id").toString();
            String nombre = mongoDoc.getString("nombre");
            String correo = mongoDoc.getString("correo");
            
            // Extraer dirección embebida
            Document direccionDoc = mongoDoc.get("direccion", Document.class);
            Direccion direccion = new Direccion(
                direccionDoc.getString("calle"),
                direccionDoc.getString("ciudad"),
                direccionDoc.getString("pais")
            );
            
            // Crear objeto Cliente
            Cliente cliente = new Cliente(mongoId, nombre, correo, direccion);
            
            // Establecer el cliente transformado en el exchange
            exchange.getIn().setBody(cliente);
            
            logger.info("Cliente transformado exitosamente: {}", cliente.nombre());
            
        } catch (Exception e) {
            logger.error("Error al procesar documento MongoDB: {}", e.getMessage(), e);
            throw new RuntimeException("Error en transformación de cliente", e);
        }
    }
}
