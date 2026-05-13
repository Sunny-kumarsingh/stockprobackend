package com.stockpro.warehouseservice;

import org.junit.jupiter.api.Test;

/**
 * Sanity check - verifies the test infrastructure itself works.
 * Full Spring context test is excluded to avoid requiring live DB + RabbitMQ during unit test phase.
 */
class WarehouseServiceApplicationTests {

    @Test
    void sanityCheck() {
        // Basic sanity — if this runs, the test framework is working
        org.junit.jupiter.api.Assertions.assertTrue(true);
    }
}
