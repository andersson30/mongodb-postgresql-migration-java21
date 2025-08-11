package com.techtest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Aplicación principal para la migración de datos MongoDB a PostgreSQL
 * usando Apache Camel
 * 
 * @author Technical Test
 * @version 1.0.0
 */
@SpringBootApplication
public class MigrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(MigrationApplication.class, args);
    }
}
