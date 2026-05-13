package com.stockpro.analytics;

import com.stockpro.analytics.controller.AnalyticsController;
import com.stockpro.analytics.dto.GlobalMetricsDTO;
import com.stockpro.analytics.dto.ProductPerformanceDTO;
import com.stockpro.analytics.dto.SupplierSpendDTO;
import com.stockpro.analytics.dto.WarehouseUtilizationDTO;
import com.stockpro.analytics.service.AnalyticsService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    @Mock
    private AnalyticsService service;

    @Mock
    private HttpServletResponse servletResponse;

    private AnalyticsController controller;
    private ProductPerformanceDTO product;

    @BeforeEach
    void setUp() {
        controller = new AnalyticsController(service);
        product = new ProductPerformanceDTO(1L, "SKU-1", 3.5, "TOP_MOVING", LocalDateTime.now());
    }

    @Test
    void metricEndpointsDelegateToService() {
        GlobalMetricsDTO metrics = new GlobalMetricsDTO();
        metrics.setTotalInventoryValue(1000.0);
        when(service.calculateGlobalValuation()).thenReturn(1000.0);
        when(service.getTopMovingProducts(5)).thenReturn(List.of(product));
        when(service.getDeadStock()).thenReturn(List.of(product));
        when(service.getGlobalDashboardMetrics()).thenReturn(metrics);

        assertThat(controller.getGlobalValuation().getBody()).isEqualTo(1000.0);
        assertThat(controller.getTopMoving(5).getBody()).hasSize(1);
        assertThat(controller.getDeadStock().getBody()).hasSize(1);
        assertThat(controller.getDashboard().getBody()).isSameAs(metrics);
    }

    @Test
    void dashboardReturns500MessageWhenServiceFails() {
        when(service.getGlobalDashboardMetrics()).thenThrow(new RuntimeException("downstream"));

        var response = controller.getDashboard();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo("Analytics Error: downstream");
    }

    @Test
    void warehouseAndSupplierEndpointsDelegate() {
        WarehouseUtilizationDTO warehouse =
                new WarehouseUtilizationDTO(1L, "Main", 20, 100, 20.0, "NORMAL");
        SupplierSpendDTO spend =
                new SupplierSpendDTO(1L, "ABC", 500.0, 2, 250.0, "2026-05-04");
        when(service.getWarehouseUtilization()).thenReturn(List.of(warehouse));
        when(service.getSupplierSpend()).thenReturn(List.of(spend));

        assertThat(controller.getWarehouseUtilization().getBody()).hasSize(1);
        assertThat(controller.getSupplierSpend().getBody()).hasSize(1);
    }

    @Test
    void exportEndpointsDelegate() throws IOException {
        controller.exportValuationCsv(servletResponse);
        controller.exportDeadStockCsv(servletResponse);
        controller.exportTopMovingCsv(servletResponse);

        verify(service).exportValuationCsv(servletResponse);
        verify(service).exportDeadStockCsv(servletResponse);
        verify(service).exportTopMovingCsv(servletResponse);
    }

    @Test
    void exportEndpointPropagatesIOException() throws IOException {
        doThrow(new IOException("disk")).when(service).exportValuationCsv(servletResponse);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> controller.exportValuationCsv(servletResponse))
                .isInstanceOf(IOException.class)
                .hasMessage("disk");
    }
}
