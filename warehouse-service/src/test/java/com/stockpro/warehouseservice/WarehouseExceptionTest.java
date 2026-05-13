package com.stockpro.warehouseservice;

import com.stockpro.warehouse.exception.CapacityExceededException;
import com.stockpro.warehouse.exception.ProductNotFoundException;
import com.stockpro.warehouse.exception.WarehouseNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for warehouse custom exception classes to satisfy SonarQube coverage gate.
 */
class WarehouseExceptionTest {

    @Test
    @DisplayName("CapacityExceededException - message constructor")
    void capacityExceededException() {
        CapacityExceededException ex = new CapacityExceededException("Capacity exceeded");
        assertThat(ex.getMessage()).isEqualTo("Capacity exceeded");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("ProductNotFoundException - message constructor")
    void productNotFoundException() {
        ProductNotFoundException ex = new ProductNotFoundException("Product 99 not found");
        assertThat(ex.getMessage()).isEqualTo("Product 99 not found");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("WarehouseNotFoundException - message constructor")
    void warehouseNotFoundException() {
        WarehouseNotFoundException ex = new WarehouseNotFoundException("Warehouse 5 not found");
        assertThat(ex.getMessage()).isEqualTo("Warehouse 5 not found");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }
}
