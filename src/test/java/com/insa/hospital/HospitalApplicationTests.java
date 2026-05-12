package com.insa.hospital;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test — verifies the Spring application context loads successfully.
 * Requires a running MySQL database with the nhospital1 schema imported.
 */
@SpringBootTest
@ActiveProfiles("test")
class HospitalApplicationTests {

    @Test
    void contextLoads() {
        // If the context loads without throwing, the test passes.
        // This validates: datasource, JPA, security config, and beans.
    }
}
