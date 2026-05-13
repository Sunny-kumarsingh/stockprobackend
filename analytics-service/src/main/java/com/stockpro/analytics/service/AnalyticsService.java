package com.stockpro.analytics.service;

import com.stockpro.analytics.dto.*;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

public interface AnalyticsService {

    // §2.8 — Inventory Valuation
    Double calculateGlobalValuation();

    // §2.8 — Top Moving / Slow Moving / Dead Stock
    List<ProductPerformanceDTO> getTopMovingProducts(int limit);
    List<ProductPerformanceDTO> getSlowMovingProducts();
    List<ProductPerformanceDTO> getDeadStock();

    // §2.8 — Full Dashboard (single call for React)
    GlobalMetricsDTO getGlobalDashboardMetrics();

    // §2.8 — Warehouse Utilization (dedicated endpoint)
    List<WarehouseUtilizationDTO> getWarehouseUtilization();

    // §2.8 — Supplier Spend Analytics
    List<SupplierSpendDTO> getSupplierSpend();

    // §2.8 — CSV Exports
    void exportValuationCsv(HttpServletResponse response) throws IOException;
    void exportDeadStockCsv(HttpServletResponse response) throws IOException;
    void exportTopMovingCsv(HttpServletResponse response) throws IOException;
    void exportSlowMovingCsv(HttpServletResponse response) throws IOException;
}
