package com.stockpro.warehouse.controller;

import com.stockpro.warehouse.dto.StockIssueRequest;
import com.stockpro.warehouse.dto.StockWriteOffRequest;
import com.stockpro.warehouse.dto.StockReturnRequest;
import com.stockpro.warehouse.entity.StockLevel;
import com.stockpro.warehouse.entity.StockTransferRequest;
import com.stockpro.warehouse.entity.Warehouse;
import com.stockpro.warehouse.service.WarehouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService service;

    // Create Warehouse
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Warehouse createWarehouse(@RequestBody Warehouse warehouse) {
        return service.createWarehouse(warehouse);
    }

    // Get All Warehouses
    @GetMapping
    public List<Warehouse> getAllWarehouses() {
        return service.getAllWarehouses();
    }

    // Get single warehouse
    @GetMapping("/{id}")
    public Warehouse getById(@PathVariable("id") Long id) {
        return service.getWarehouseById(id);
    }

    // Update warehouse
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public Warehouse updateWarehouse(@PathVariable("id") Long id, @RequestBody Warehouse warehouse) {
        return service.updateWarehouse(id, warehouse);
    }

    // Activate / Deactivate warehouse
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/status")
    public String setStatus(@PathVariable("id") Long id, @RequestParam("active") boolean active) {
        service.setWarehouseActive(id, active);
        return active ? "Warehouse Activated" : "Warehouse Deactivated";
    }

    // Assign Manager
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/manager")
    public String assignManager(@PathVariable("id") Long id, @RequestParam("managerId") Long managerId) {
        service.assignManager(id, managerId);
        return "Manager assigned";
    }

    // Get Stock of a Product in Warehouse
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @GetMapping("/stock")
    public StockLevel getStock(@RequestParam("warehouseId") Long warehouseId,
                               @RequestParam("productId") Long productId) {
        return service.getStock(warehouseId, productId);
    }

    // Manual Stock Update (sets absolute quantity)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @PostMapping("/{warehouseId}/stock")
    public StockLevel updateStock(@PathVariable("warehouseId") Long warehouseId,
                                  @RequestParam("productId") Long productId,
                                  @RequestParam("quantity") int quantity,
                                  @RequestParam("reason") String reason) {
        return service.updateStock(warehouseId, productId, quantity, reason);
    }

    // Incremental Add (used by Purchase Service via Feign)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @PostMapping("/{warehouseId}/stock/add")
    public StockLevel addStock(@PathVariable("warehouseId") Long warehouseId,
                               @RequestParam("productId") Long productId,
                               @RequestParam("delta") int delta,
                               @RequestParam("reason") String reason) {
        return service.addStock(warehouseId, productId, delta, reason);
    }

    // Delete orphan stock entry (Admin cleanup)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{warehouseId}/stock")
    public String deleteStockEntry(@PathVariable("warehouseId") Long warehouseId,
                                   @RequestParam("productId") Long productId) {
        service.deleteStockEntry(warehouseId, productId);
        return "Stock entry deleted";
    }

    // Update Min Threshold
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PutMapping("/{warehouseId}/stock/threshold")
    public String updateThreshold(@PathVariable("warehouseId") Long warehouseId,
                                   @RequestParam("productId") Long productId,
                                   @RequestParam("threshold") int threshold) {
        service.updateStockThreshold(warehouseId, productId, threshold);
        return "Threshold updated";
    }

    // Reserve Stock
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping("/{warehouseId}/reserve")
    public String reserveStock(@PathVariable("warehouseId") Long warehouseId,
                               @RequestParam("productId") Long productId,
                               @RequestParam("qty") int qty) {
        service.reserveStock(warehouseId, productId, qty);
        return "Stock Reserved";
    }

    // Release Stock
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping("/{warehouseId}/release")
    public String releaseStock(@PathVariable("warehouseId") Long warehouseId,
                               @RequestParam("productId") Long productId,
                               @RequestParam("qty") int qty) {
        service.releaseReservation(warehouseId, productId, qty);
        return "Stock Released";
    }

    // Transfer Stock between warehouses
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @PostMapping("/transfer")
    public String transferStock(@RequestParam("productId") Long productId,
                                @RequestParam("fromWarehouse") Long fromWarehouse,
                                @RequestParam("toWarehouse") Long toWarehouse,
                                @RequestParam("qty") int qty,
                                @RequestParam("reason") String reason) {
        service.transferStock(productId, fromWarehouse, toWarehouse, qty, reason);
        return "Stock Transferred";
    }

    // Request stock from all eligible warehouses. The transfer starts only after one accepts.
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @PostMapping("/transfer/request")
    public StockTransferRequest requestStockTransfer(@RequestParam("productId") Long productId,
                                                     @RequestParam("requestingWarehouseId") Long requestingWarehouseId,
                                                     @RequestParam("qty") int qty,
                                                     @RequestParam("reason") String reason) {
        return service.requestStockTransfer(productId, requestingWarehouseId, qty, reason);
    }

    // Accept a pending request from the warehouse that will supply the stock.
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @PostMapping("/transfer/requests/{requestId}/accept")
    public StockTransferRequest acceptTransferRequest(@PathVariable("requestId") Long requestId,
                                                      @RequestParam("fromWarehouseId") Long fromWarehouseId) {
        return service.acceptTransferRequest(requestId, fromWarehouseId);
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @GetMapping("/{warehouseId}/transfer-requests")
    public List<StockTransferRequest> getTransferRequestsForWarehouse(@PathVariable("warehouseId") Long warehouseId) {
        return service.getTransferRequestsForWarehouse(warehouseId);
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @GetMapping("/transfer/requests/pending")
    public List<StockTransferRequest> getPendingTransferRequests() {
        return service.getPendingTransferRequests();
    }

    // Low Stock Report
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @GetMapping("/{warehouseId}/low-stock")
    public List<StockLevel> getLowStock(@PathVariable("warehouseId") Long warehouseId) {
        return service.getLowStockReport(warehouseId);
    }

    // Full Inventory of a Warehouse
    @GetMapping("/{id}/inventory")
    public List<StockLevel> getInventory(@PathVariable("id") Long id) {
        return service.getWarehouseInventory(id);
    }

    // Stock Issue — Sales / Production / Internal Use (PDF §2.2)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @PostMapping("/stock/issue")
    public StockLevel issueStock(@Valid @RequestBody StockIssueRequest request) {
        return service.issueStock(request);
    }

    // Stock Write-Off — Damaged or Expired goods (PDF §2.6)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @PostMapping("/stock/write-off")
    public StockLevel writeOffStock(@Valid @RequestBody StockWriteOffRequest request) {
        return service.writeOffStock(request);
    }

    // Stock Return — Supplier or Customer return (PDF §2.6)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @PostMapping("/stock/return")
    public StockLevel returnStock(@Valid @RequestBody StockReturnRequest request) {
        return service.returnStock(request);
    }
}
