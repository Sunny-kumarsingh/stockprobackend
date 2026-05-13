package com.stockpro.analytics;

import com.stockpro.analytics.client.ProductClient;
import com.stockpro.analytics.client.WarehouseClient;
import com.stockpro.analytics.dto.ProductPerformanceDTO;
import com.stockpro.analytics.dto.SupplierSpendDTO;
import com.stockpro.analytics.entity.ProductPerformance;
import com.stockpro.analytics.entity.SupplierSpend;
import com.stockpro.analytics.repository.ProductPerformanceRepository;
import com.stockpro.analytics.repository.SnapshotRepository;
import com.stockpro.analytics.repository.SupplierSpendRepository;
import com.stockpro.analytics.service.impl.AnalyticsServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * JUnit 5 + Mockito unit tests for AnalyticsServiceImpl.
 * Uses exact repository method names from ProductPerformanceRepository and SupplierSpendRepository.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsServiceImpl - Unit Tests")
class AnalyticsServiceImplTest {

    @Mock private ProductClient productClient;
    @Mock private WarehouseClient warehouseClient;
    @Mock private ProductPerformanceRepository performanceRepository;
    @Mock private SnapshotRepository snapshotRepository;
    @Mock private SupplierSpendRepository supplierSpendRepository;

    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    private ProductPerformance topProduct;
    private ProductPerformance deadProduct;
    private SupplierSpend supplierSpend;

    @BeforeEach
    void setUp() {
        //  Correct entity fields: productId, sku, turnoverRate, movementCategory
        topProduct = new ProductPerformance();
        topProduct.setProductId(1L);
        topProduct.setSku("PROD-001");
        topProduct.setTurnoverRate(4.5);
        topProduct.setMovementCategory("TOP_MOVING");

        deadProduct = new ProductPerformance();
        deadProduct.setProductId(2L);
        deadProduct.setSku("PROD-002");
        deadProduct.setTurnoverRate(0.1);
        deadProduct.setMovementCategory("DEAD");

        //  SupplierSpend ID is supplierId (no separate 'id')
        supplierSpend = new SupplierSpend();
        supplierSpend.setSupplierId(10L);
        supplierSpend.setSupplierName("ABC Supplies");
        supplierSpend.setTotalSpend(250000.0);
        supplierSpend.setTotalOrdersReceived(5);
        supplierSpend.setAvgOrderValue(50000.0);
    }

    // ─────────────────────────────────────────────
    // VALUATION NULL-SAFETY TESTS
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("calculateGlobalValuation() - should return 0.0 when products is null")
    void calculateGlobalValuation_ShouldReturnZero_WhenProductsNull() {
        when(productClient.getAllProducts()).thenReturn(null);

        Double result = analyticsService.calculateGlobalValuation();

        assertThat(result).isEqualTo(0.0);
    }

    @Test
    @DisplayName("calculateGlobalValuation() - should return 0.0 when warehouses is null")
    void calculateGlobalValuation_ShouldReturnZero_WhenWarehousesNull() {
        when(productClient.getAllProducts()).thenReturn(List.of());
        when(warehouseClient.getAllWarehouses()).thenReturn(null);

        Double result = analyticsService.calculateGlobalValuation();

        assertThat(result).isEqualTo(0.0);
    }

    @Test
    @DisplayName("calculateGlobalValuation() - should return 0.0 when both empty")
    void calculateGlobalValuation_ShouldReturnZero_WhenBothEmpty() {
        when(productClient.getAllProducts()).thenReturn(List.of());
        when(warehouseClient.getAllWarehouses()).thenReturn(List.of());

        Double result = analyticsService.calculateGlobalValuation();

        assertThat(result).isEqualTo(0.0);
    }

    // ─────────────────────────────────────────────
    // TOP MOVING PRODUCTS TESTS
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("getTopMovingProducts() - should return top-moving using findTop10ByOrderByTurnoverRateDesc()")
    void getTopMovingProducts_ShouldReturnResults() {
        // ✅ Actual repo method: findTop10ByOrderByTurnoverRateDesc()
        when(performanceRepository.findTop10ByOrderByTurnoverRateDesc())
                .thenReturn(List.of(topProduct));

        List<ProductPerformanceDTO> result = analyticsService.getTopMovingProducts(5);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSku()).isEqualTo("PROD-001");
        assertThat(result.get(0).getMovementCategory()).isEqualTo("TOP_MOVING");
    }

    @Test
    @DisplayName("getTopMovingProducts() - should return empty when no data")
    void getTopMovingProducts_ShouldReturnEmpty_WhenNoData() {
        when(performanceRepository.findTop10ByOrderByTurnoverRateDesc()).thenReturn(List.of());

        List<ProductPerformanceDTO> result = analyticsService.getTopMovingProducts(5);

        assertThat(result).isEmpty();
    }

    // ─────────────────────────────────────────────
    // DEAD STOCK TESTS
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("getDeadStock() - should return DEAD movementCategory products")
    void getDeadStock_ShouldReturnDeadProducts() {
        when(performanceRepository.findDeadStockBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(deadProduct));

        List<ProductPerformanceDTO> result = analyticsService.getDeadStock();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSku()).isEqualTo("PROD-002");
        assertThat(result.get(0).getMovementCategory()).isEqualTo("DEAD");
    }

    @Test
    @DisplayName("getDeadStock() - should return empty when no dead stock")
    void getDeadStock_ShouldReturnEmpty_WhenNone() {
        when(performanceRepository.findDeadStockBefore(any(LocalDateTime.class))).thenReturn(List.of());

        List<ProductPerformanceDTO> result = analyticsService.getDeadStock();

        assertThat(result).isEmpty();
    }

    // ─────────────────────────────────────────────
    // SUPPLIER SPEND TESTS
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("getSupplierSpend() - should return records from findTopSuppliersBySpend()")
    void getSupplierSpend_ShouldReturnAll() {
        //  Actual repo method: findTopSuppliersBySpend() — not findAll()
        when(supplierSpendRepository.findTopSuppliersBySpend())
                .thenReturn(List.of(supplierSpend));

        List<SupplierSpendDTO> result = analyticsService.getSupplierSpend();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSupplierName()).isEqualTo("ABC Supplies");
        assertThat(result.get(0).getTotalSpend()).isEqualTo(250000.0);
    }

    @Test
    @DisplayName("getSupplierSpend() - should return empty when no records")
    void getSupplierSpend_ShouldReturnEmpty_WhenNone() {
        when(supplierSpendRepository.findTopSuppliersBySpend()).thenReturn(List.of());

        List<SupplierSpendDTO> result = analyticsService.getSupplierSpend();

        assertThat(result).isEmpty();
    }
}
