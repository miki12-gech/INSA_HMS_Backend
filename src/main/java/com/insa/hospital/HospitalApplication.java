package com.insa.hospital;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * HMS Backend — Spring Boot Entry Point
 *
 * MIGRATION NOTE:
 * This application is a strict 1:1 migration of the legacy PHP/CodeIgniter
 * Hospital Management System. The database schema is FIXED; Hibernate is
 * configured with ddl-auto=none and must NOT alter or create any tables.
 *
 * Legacy DB: nhospital1 (MySQL)
 * Reference: nhospital1 (6).sql dump in the project root.
 */
@SpringBootApplication
public class HospitalApplication {

    public static void main(String[] args) {
        try {
            SpringApplication.run(HospitalApplication.class, args);
        } catch (Exception e) {
            System.err.println("CRITICAL FAILURE DURING STARTUP:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
