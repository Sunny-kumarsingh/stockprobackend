package com.stockpro.warehouse.service;

import com.stockpro.warehouse.dto.StockIssueRequest;
import com.stockpro.warehouse.dto.StockWriteOffRequest;
import com.stockpro.warehouse.dto.StockReturnRequest;
import com.stockpro.warehouse.entity.Warehouse;
import com.stockpro.warehouse.entity.StockLevel;
import com.stockpro.warehouse.entity.StockTransferRequest;

import java.util.List;

public interface WarehouseService {

    // Warehouse
    Warehouse createWarehouse(Warehouse warehouse);
    List<Warehouse> getAllWarehouses();
    Warehouse getWarehouseById(Long id);
    Warehouse updateWarehouse(Long id, Warehouse updated);
    void setWarehouseActive(Long id, boolean active);
    void assignManager(Long warehouseId, Long managerId);

    // Stock
    StockLevel getStock(Long warehouseId, Long productId);
    StockLevel updateStock(Long warehouseId, Long productId, int quantity, String reason);
    StockLevel addStock(Long warehouseId, Long productId, int delta, String reason);
    void deleteStockEntry(Long warehouseId, Long productId);
    void updateStockThreshold(Long warehouseId, Long productId, int threshold);

    // Stock Issue — Consumption (Sales, Production, Internal Use)
    StockLevel issueStock(StockIssueRequest request);

    // Stock Write-Off — Damaged or Expired goods (PDF §2.6)
    StockLevel writeOffStock(StockWriteOffRequest request);

    // Stock Return — From Supplier or Customer (PDF §2.6)
    StockLevel returnStock(StockReturnRequest request);

    List<StockLevel> getLowStockReport(Long warehouseId);
    List<StockLevel> getWarehouseInventory(Long warehouseId);

    // Reservation
    void reserveStock(Long warehouseId, Long productId, int qty);
    void releaseReservation(Long warehouseId, Long productId, int qty);

    // Transfer
    void transferStock(Long productId, Long fromWarehouse, Long toWarehouse, int qty, String reason);
    StockTransferRequest requestStockTransfer(Long productId, Long requestingWarehouseId, int qty, String reason);
    StockTransferRequest acceptTransferRequest(Long requestId, Long fromWarehouseId);
    List<StockTransferRequest> getTransferRequestsForWarehouse(Long warehouseId);
    List<StockTransferRequest> getPendingTransferRequests();
}
