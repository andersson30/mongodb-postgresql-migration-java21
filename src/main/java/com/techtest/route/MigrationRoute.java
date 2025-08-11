package com.techtest.route;

import com.techtest.processor.ClienteProcessor;
import com.techtest.service.PostgreSQLService;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mongodb.MongoDbConstants;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Ruta Apache Camel para migrar datos de MongoDB a PostgreSQL
 */
@Component
public class MigrationRoute extends RouteBuilder {

    @Autowired
    private ClienteProcessor clienteProcessor;

    @Autowired
    private PostgreSQLService postgreSQLService;

    @Override
    public void configure() throws Exception {
        
        // Configuración global de manejo de errores
        onException(Exception.class)
            .handled(true)
            .maximumRedeliveries(3)
            .redeliveryDelay(2000)
            .backOffMultiplier(2)
            .log("Error procesando mensaje: ${exception.message}")
            .to("direct:error-handler");

        // Ruta principal de migración
        from("timer://migration?period=30000&repeatCount=1")
            .routeId("mongodb-to-postgresql-migration")
            .log("=== Iniciando migración de MongoDB a PostgreSQL ===")
            .bean(postgreSQLService, "verificarConexion")
            .choice()
                .when(body().isEqualTo(false))
                    .log("ERROR: No se puede conectar a PostgreSQL")
                    .stop()
                .otherwise()
                    .log("Conexión PostgreSQL verificada")
            .end()
            .to("direct:read-from-mongodb");

        // Leer documentos de MongoDB
        from("direct:read-from-mongodb")
            .routeId("read-mongodb-clients")
            .log("Leyendo clientes desde MongoDB...")
            .setHeader(MongoDbConstants.COLLECTION, constant("clientes"))
            .to("mongodb:mongoClient?database=techtest&collection=clientes&operation=findAll")
            .split(body())
                .streaming()
                .to("direct:process-client")
            .end()
            .log("=== Migración completada ===")
            .bean(postgreSQLService, "obtenerEstadisticas")
            .log("${body}");

        // Procesar cada cliente individualmente
        from("direct:process-client")
            .routeId("process-individual-client")
            .log("Procesando cliente: ${body}")
            .process(clienteProcessor)
            .to("direct:save-to-postgresql");

        // Guardar en PostgreSQL
        from("direct:save-to-postgresql")
            .routeId("save-to-postgresql")
            .log("Guardando cliente en PostgreSQL: ${body.nombre}")
            .bean(postgreSQLService, "upsertCliente")
            .log("Cliente guardado con ID: ${body}");

        // Manejo de errores
        from("direct:error-handler")
            .routeId("error-handler")
            .log("=== MANEJANDO ERROR ===")
            .log("Error: ${exception.message}")
            .log("Causa: ${exception.stacktrace}")
            .choice()
                .when(header("CamelRedeliveryCounter").isLessThan(3))
                    .log("Reintentando... Intento ${header.CamelRedeliveryCounter}")
                .otherwise()
                    .log("ERROR: Máximo número de reintentos alcanzado")
                    .to("direct:dead-letter")
            .end();

        // Dead letter queue (simulado con log)
        from("direct:dead-letter")
            .routeId("dead-letter-queue")
            .log("=== MENSAJE ENVIADO A DEAD LETTER QUEUE ===")
            .log("Mensaje que falló: ${body}")
            .log("Error: ${exception.message}");

        // Ruta para migración manual (útil para testing)
        from("direct:migrate-single-client")
            .routeId("migrate-single-client")
            .log("Migrando cliente individual: ${body}")
            .process(clienteProcessor)
            .bean(postgreSQLService, "upsertCliente")
            .log("Cliente migrado con ID: ${body}");
    }
}
