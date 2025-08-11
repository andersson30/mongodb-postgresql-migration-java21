package com.techtest.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Configuración de las conexiones a las bases de datos
 * MongoDB y PostgreSQL
 */
@Configuration
public class DatabaseConfig {

    @Value("${mongodb.connection.uri:mongodb://localhost:27017}")
    private String mongoUri;

    @Value("${mongodb.database.name:techtest}")
    private String mongoDatabase;

    @Value("${postgresql.url:jdbc:postgresql://localhost:5432/techtest}")
    private String postgresqlUrl;

    @Value("${postgresql.username:postgres}")
    private String postgresqlUsername;

    @Value("${postgresql.password:postgres}")
    private String postgresqlPassword;

    /**
     * Configuración del cliente MongoDB
     */
    @Bean
    public MongoClient mongoClient() {
        return MongoClients.create(mongoUri);
    }

    /**
     * Base de datos MongoDB
     */
    @Bean
    public MongoDatabase mongoDatabase(MongoClient mongoClient) {
        return mongoClient.getDatabase(mongoDatabase);
    }

    /**
     * DataSource PostgreSQL con pool de conexiones HikariCP
     */
    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(postgresqlUrl);
        config.setUsername(postgresqlUsername);
        config.setPassword(postgresqlPassword);
        config.setDriverClassName("org.postgresql.Driver");
        
        // Configuración del pool
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        
        // Configuraciones adicionales para PostgreSQL
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        
        return new HikariDataSource(config);
    }
}
