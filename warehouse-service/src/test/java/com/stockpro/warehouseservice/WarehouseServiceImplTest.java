package com.stockpro.warehouseservice;

import com.stockpro.warehouse.client.MovementClient;
import com.stockpro.warehouse.client.ProductClient;
import com.stockpro.warehouse.dto.StockIssueRequest;
import com.stockpro.warehouse.dto.StockWriteOffRequest;
import com.stockpro.warehouse.dto.StockReturnRequest;
import com.stockpro.warehouse.entity.StockLevel;
import com.stockpro.warehouse.entity.StockTransferRequest;
import com.stockpro.warehouse.entity.TransferRequestStatus;
import com.stockpro.warehouse.entity.Warehouse;
import com.stockpro.warehouse.publisher.StockEventPublisher;
import com.stockpro.warehouse.repository.StockLevelRepository;
import com.stockpro.warehouse.repository.StockTransferRequestRepository;
import com.stockpro.warehouse.repository.WarehouseRepository;
import com.stockpro.warehouse.service.impl.WarehouseServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * JUnit 5 + Mockito unit tests for WarehouseServiceImpl.
 * Tests: createWarehouse, getById, getAllWarehouses, updateWarehouse, setWarehouseActive,
 *        assignManager, getStock, getFullInventory, getLowStockReport, getWarehouseInventory,
 *        updateStockThreshold, reserveStock, releaseReservation, deleteStockEntry,
 *        issueStock, writeOffStock, returnStock.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WarehouseServiceImpl - Unit Tests")
class WarehouseServiceImplTest {

    @Mock private WarehouseRepository warehouseRepo;
    @Mock private StockLevelRepository stockRepo;
    @Mock private StockTransferRequestRepository transferRequestRepo;
    @Mock private MovementClient movementClient;
    @Mock private ProductClient productClient;
    @Mock private StockEventPublisher stockEventPublisher;

    @InjectMocks
    private WarehouseServiceImpl warehouseService;

    private Warehouse testWarehouse;
    private StockLevel testStock;

    @BeforeEach
    void setUp() {
        testWarehouse = new Warehouse();
        testWarehouse.setWarehouseId(1L);
        testWarehouse.setName("Main Warehouse");
        testWarehouse.setLocation("Mumbai");
        testWarehouse.setAddress("123 Industrial Area");
        testWarehouse.setPhone("9876543210");
        testWarehouse.setCapacity(1000);
        testWarehouse.setUsedCapacity(200);
        testWarehouse.setIsActive(true);

        testStock = new StockLevel();
        testStock.setStockId(1L);
        testStock.setWarehouseId(1L);
        testStock.setProductId(10L);
        testStock.setQuantity(50);
        testStock.setReservedQuantity(0);
        testStock.setMinThreshold(5);
        testStock.setMaxStockLevel(1000);
    }

    // ─────────────────────────────────────────────
    // CREATE WAREHOUSE TESTS
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("createWarehouse() - should save warehouse with isActive=true and usedCapacity=0")
    void createWarehouse_ShouldSaveWithDefaults() {
        Warehouse input = new Warehouse();
        input.setName("New Warehouse");
        input.setCapacity(500);
        when(warehouseRepo.save(any(Warehouse.class))).thenReturn(testWarehouse);

        warehouseService.createWarehouse(input);

        assertThat(input.getIsActive()).isTrue();
        assertThat(input.getUsedCapacity()).isZero();
        verify(warehouseRepo).save(input);
    }

    // ─────────────────────────────────────────────
    // GET BY ID TESTS
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("getWarehouseById() - should return warehouse when found")
    void getWarehouseById_ShouldReturn_WhenFound() {
        when(warehouseRepo.findById(1L)).thenReturn(Optional.of(testWarehouse));

        Warehouse result = warehouseService.getWarehouseById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Main Warehouse");
    }

    @Test
    @DisplayName("getWarehouseById() - should throw RuntimeException when not found")
    void getWarehouseById_ShouldThrow_WhenNotFound() {
        when(warehouseRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseService.getWarehouseById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Warehouse not found");
    }

    // ─────────────────────────────────────────────
    // GET ALL WAREHOUSES
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("getAllWarehouses() - should return all warehouses")
    void getAllWarehouses_ShouldReturnAll() {
        when(warehouseRepo.findAll()).thenReturn(List.of(testWarehouse));

        List<Warehouse> result = warehouseService.getAllWarehouses();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Main Warehouse");
    }

    @Test
    @DisplayName("getAllWarehouses() - should return empty list when none exist")
    void getAllWarehouses_ShouldReturnEmpty_WhenNone() {
        when(warehouseRepo.findAll()).thenReturn(List.of());

        List<Warehouse> result = warehouseService.getAllWarehouses();

        assertThat(result).isEmpty();
    }

    // ─────────────────────────────────────────────
    // UPDATE WAREHOUSE TESTS
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("updateWarehouse() - should update fields and save")
    void updateWarehouse_ShouldUpdateFields_WhenFound() {
        Warehouse updated = new Warehouse();
        updated.setName("Updated Warehouse");
        updated.setLocation("Delhi");
        updated.setAddress("456 New Area");
        updated.setPhone("1111111111");
        updated.setCapacity(2000);

        when(warehouseRepo.findById(1L)).thenReturn(Optional.of(testWarehouse));
        when(warehouseRepo.save(any(Warehouse.class))).thenReturn(testWarehouse);

        warehouseService.updateWarehouse(1L, updated);

        assertThat(testWarehouse.getName()).isEqualTo("Updated Warehouse");
        assertThat(testWarehouse.getLocation()).isEqualTo("Delhi");
        assertThat(testWarehouse.getCapacity()).isEqualTo(2000);
        verify(warehouseRepo).save(testWarehouse);
    }

    @Test
    @DisplayName("updateWarehouse() - should not update capacity when null")
    void updateWarehouse_ShouldNotUpdateCapacity_WhenNull() {
        Warehouse updated = new Warehouse();
        updated.setName("Updated Name");
        updated.setLocation("Pune");
        updated.setAddress("789 Area");
        updated.setPhone("2222222222");
        updated.setCapacity(null); // no capacity update

        when(warehouseRepo.findById(1L)).thenReturn(Optional.of(testWarehouse));
        when(warehouseRepo.save(any(Warehouse.class))).thenReturn(testWarehouse);

        warehouseService.updateWarehouse(1L, updated);

        // Original capacity (1000) should be unchanged
        assertThat(testWarehouse.getCapacity()).isEqualTo(1000);
        verify(warehouseRepo).save(testWarehouse);
    }

    // ─────────────────────────────────────────────
    // SET WAREHOUSE ACTIVE / DEACTIVATE
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("setWarehouseActive(false) - should deactivate warehouse")
    void setWarehouseActive_ShouldDeactivate_WhenCalledWithFalse() {
        when(warehouseRepo.findById(1L)).thenReturn(Optional.of(testWarehouse));

        warehouseService.setWarehouseActive(1L, false);

        assertThat(testWarehouse.getIsActive()).isFalse();
        verify(warehouseRepo).save(testWarehouse);
    }

    @Test
    @DisplayName("setWarehouseActive(true) - should activate warehouse")
    void setWarehouseActive_ShouldActivate_WhenCalledWithTrue() {
        testWarehouse.setIsActive(false);
        when(warehouseRepo.findById(1L)).thenReturn(Optional.of(testWarehouse));

        warehouseService.setWarehouseActive(1L, true);

        assertThat(testWarehouse.getIsActive()).isTrue();
        verify(warehouseRepo).save(testWarehouse);
    }

    // ─────────────────────────────────────────────
    // ASSIGN MANAGER
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("assignManager() - should set managerId on warehouse")
    void assignManager_ShouldSetManagerId_WhenWarehouseExists() {
        when(warehouseRepo.findById(1L)).thenReturn(Optional.of(testWarehouse));

        warehouseService.assignManager(1L, 42L);

        assertThat(testWarehouse.getManagerId()).isEqualTo(42L);
        verify(warehouseRepo).save(testWarehouse);
    }

    // ─────────────────────────────────────────────
    // GET STOCK
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("getStock() - should return stock level when found")
    void getStock_ShouldReturn_WhenFound() {
        when(stockRepo.findByWarehouseId(1L)).thenReturn(List.of(testStock));

        StockLevel result = warehouseService.getStock(1L, 10L);

        assertThat(result).isNotNull();
        assertThat(result.getQuantity()).isEqualTo(50);
    }

    @Test
    @DisplayName("getStock() - should throw RuntimeException when not found")
    void getStock_ShouldThrow_WhenNotFound() {
        when(stockRepo.findByWarehouseId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> warehouseService.getStock(1L, 10L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Stock not found");
    }

    // ─────────────────────────────────────────────
    // GET FULL INVENTORY / WAREHOUSE INVENTORY
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("getWarehouseInventory() - should return all stock levels for warehouse")
    void getWarehouseInventory_ShouldReturnStockLevels() {
        when(stockRepo.findByWarehouseId(1L)).thenReturn(List.of(testStock));

        List<StockLevel> result = warehouseService.getWarehouseInventory(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProductId()).isEqualTo(10L);
        assertThat(result.get(0).getQuantity()).isEqualTo(50);
    }

    @Test
    @DisplayName("getWarehouseInventory() - should return empty list when no stock")
    void getWarehouseInventory_ShouldReturnEmpty_WhenNoStock() {
        when(stockRepo.findByWarehouseId(1L)).thenReturn(List.of());

        List<StockLevel> result = warehouseService.getWarehouseInventory(1L);

        assertThat(result).isEmpty();
    }

    // ─────────────────────────────────────────────
    // GET LOW STOCK REPORT
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("getLowStockReport() - should return only low stock items")
    void getLowStockReport_ShouldReturnLowStockItems() {
        // testStock: qty=50, minThreshold=5 → NOT low stock
        StockLevel lowStock = new StockLevel();
        lowStock.setWarehouseId(1L);
        lowStock.setProductId(20L);
        lowStock.setQuantity(3);   // below threshold
        lowStock.setMinThreshold(5);
        lowStock.setReservedQuantity(0);

        when(stockRepo.findByWarehouseId(1L)).thenReturn(List.of(testStock, lowStock));

        List<StockLevel> result = warehouseService.getLowStockReport(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProductId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("getLowStockReport() - should return empty when all stock is above threshold")
    void getLowStockReport_ShouldReturnEmpty_WhenNoneLow() {
        when(stockRepo.findByWarehouseId(1L)).thenReturn(List.of(testStock)); // qty=50 > minThreshold=5

        List<StockLevel> result = warehouseService.getLowStockReport(1L);

        assertThat(result).isEmpty();
    }

    // ─────────────────────────────────────────────
    // UPDATE STOCK THRESHOLD
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("updateStockThreshold() - should update minThreshold")
    void updateStockThreshold_ShouldSetThreshold_WhenStockFound() {
        when(stockRepo.findByWarehouseIdAndProductId(1L, 10L)).thenReturn(Optional.of(testStock));

        warehouseService.updateStockThreshold(1L, 10L, 20);

        assertThat(testStock.getMinThreshold()).isEqualTo(20);
        verify(stockRepo).save(testStock);
    }

    @Test
    @DisplayName("updateStockThreshold() - should publish low-stock alert when stock is already below new threshold")
    void updateStockThreshold_ShouldPublishLowStockAlert_WhenExistingStockBelowNewThreshold() {
        testStock.setQuantity(5);
        when(stockRepo.findByWarehouseIdAndProductId(1L, 10L)).thenReturn(Optional.of(testStock));
        when(stockRepo.save(testStock)).thenReturn(testStock);

        warehouseService.updateStockThreshold(1L, 10L, 25);

        verify(stockEventPublisher).publishStockAlert(
                10L,
                1L,
                5,
                com.stockpro.warehouse.config.RabbitMQConfig.STOCK_LOW_ROUTING_KEY
        );
    }

    @Test
    @DisplayName("updateStockThreshold() - should throw when stock entry not found")
    void updateStockThreshold_ShouldThrow_WhenNotFound() {
        when(stockRepo.findByWarehouseIdAndProductId(1L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseService.updateStockThreshold(1L, 99L, 10))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Stock entry not found");
    }

    // ─────────────────────────────────────────────
    // RESERVE STOCK
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("reserveStock() - should increase reservedQuantity when stock sufficient")
    void reserveStock_ShouldIncreaseReserved_WhenEnoughAvailable() {
        testStock.setQuantity(50);
        testStock.setReservedQuantity(0);
        when(stockRepo.findByWarehouseId(1L)).thenReturn(List.of(testStock));

        warehouseService.reserveStock(1L, 10L, 10);

        assertThat(testStock.getReservedQuantity()).isEqualTo(10);
        verify(stockRepo).save(testStock);
    }

    @Test
    @DisplayName("reserveStock() - should throw when insufficient available stock")
    void reserveStock_ShouldThrow_WhenInsufficientStock() {
        testStock.setQuantity(5);
        testStock.setReservedQuantity(0);
        when(stockRepo.findByWarehouseId(1L)).thenReturn(List.of(testStock));

        assertThatThrownBy(() -> warehouseService.reserveStock(1L, 10L, 50))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Not enough available stock");
    }

    // ─────────────────────────────────────────────
    // RELEASE RESERVATION
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("releaseReservation() - should decrease reservedQuantity")
    void releaseReservation_ShouldDecreaseReserved() {
        testStock.setQuantity(50);
        testStock.setReservedQuantity(10);
        when(stockRepo.findByWarehouseId(1L)).thenReturn(List.of(testStock));

        warehouseService.releaseReservation(1L, 10L, 5);

        assertThat(testStock.getReservedQuantity()).isEqualTo(5);
        verify(stockRepo).save(testStock);
    }

    // UPDATE STOCK / TRANSFER TESTS

    @Test
    @DisplayName("updateStock() - should update stock, capacity, movement, sync, and publish alert")
    void updateStock_ShouldUpdateExistingStockAndPublishLowStockAlert() {
        testWarehouse.setUsedCapacity(20);
        testStock.setQuantity(30);
        testStock.setReservedQuantity(3);
        testStock.setMinThreshold(25);
        when(warehouseRepo.findById(1L)).thenReturn(Optional.of(testWarehouse));
        when(stockRepo.findByWarehouseId(1L)).thenReturn(List.of(testStock));
        when(stockRepo.save(any(StockLevel.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(stockRepo.findByProductId(10L)).thenReturn(List.of(testStock));

        StockLevel result = warehouseService.updateStock(1L, 10L, 20, "manual count");

        assertThat(result.getQuantity()).isEqualTo(20);
        assertThat(result.getReservedQuantity()).isZero();
        assertThat(testWarehouse.getUsedCapacity()).isEqualTo(10);
        verify(movementClient).saveMovement(any());
        verify(productClient).updateTotalStock(10L, 20);
        verify(stockEventPublisher).publishStockMovement(10L, 1L, 10, "OUT", "manual count");
        verify(stockEventPublisher).publishStockAlert(
                10L,
                1L,
                20,
                com.stockpro.warehouse.config.RabbitMQConfig.STOCK_LOW_ROUTING_KEY
        );
    }

    @Test
    @DisplayName("updateStock() - should create stock entry when product is not already stocked")
    void updateStock_ShouldCreateStockEntry_WhenMissing() {
        testWarehouse.setUsedCapacity(0);
        when(warehouseRepo.findById(1L)).thenReturn(Optional.of(testWarehouse));
        when(stockRepo.findByWarehouseId(1L)).thenReturn(List.of());
        when(stockRepo.save(any(StockLevel.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(stockRepo.findByProductId(10L)).thenAnswer(invocation -> List.of());

        StockLevel result = warehouseService.updateStock(1L, 10L, 15, "initial stock");

        assertThat(result.getWarehouseId()).isEqualTo(1L);
        assertThat(result.getProductId()).isEqualTo(10L);
        assertThat(result.getQuantity()).isEqualTo(15);
        assertThat(testWarehouse.getUsedCapacity()).isEqualTo(15);
        verify(stockEventPublisher).publishStockMovement(10L, 1L, 15, "IN", "initial stock");
    }

    @Test
    @DisplayName("updateStock() - should reject invalid product")
    void updateStock_ShouldRejectInvalidProduct() {
        when(productClient.getProductById(99L)).thenThrow(new RuntimeException("missing"));

        assertThatThrownBy(() -> warehouseService.updateStock(1L, 99L, 10, "manual"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid Product ID");
    }

    @Test
    @DisplayName("updateStock() - should reject capacity overflow")
    void updateStock_ShouldRejectCapacityOverflow() {
        testWarehouse.setCapacity(100);
        testWarehouse.setUsedCapacity(95);
        testStock.setQuantity(10);
        when(warehouseRepo.findById(1L)).thenReturn(Optional.of(testWarehouse));
        when(stockRepo.findByWarehouseId(1L)).thenReturn(List.of(testStock));

        assertThatThrownBy(() -> warehouseService.updateStock(1L, 10L, 25, "too much"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("capacity exceeded");
    }

    @Test
    @DisplayName("addStock() - should add delta to current stock through updateStock")
    void addStock_ShouldAddDeltaToCurrentStock() {
        testWarehouse.setUsedCapacity(50);
        testStock.setQuantity(50);
        when(stockRepo.findByWarehouseId(1L)).thenReturn(List.of(testStock));
        when(warehouseRepo.findById(1L)).thenReturn(Optional.of(testWarehouse));
        when(stockRepo.save(any(StockLevel.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(stockRepo.findByProductId(10L)).thenReturn(List.of(testStock));

        StockLevel result = warehouseService.addStock(1L, 10L, 10, "purchase receipt");

        assertThat(result.getQuantity()).isEqualTo(60);
        verify(stockEventPublisher).publishStockMovement(10L, 1L, 10, "IN", "purchase receipt");
    }

    @Test
    @DisplayName("deleteStockEntry() - should remove stock and reduce used capacity")
    void deleteStockEntry_ShouldDeleteStockAndReduceUsedCapacity() {
        testWarehouse.setUsedCapacity(200);
        testStock.setQuantity(50);
        when(stockRepo.findByWarehouseId(1L)).thenReturn(List.of(testStock));
        when(warehouseRepo.findById(1L)).thenReturn(Optional.of(testWarehouse));

        warehouseService.deleteStockEntry(1L, 10L);

        assertThat(testWarehouse.getUsedCapacity()).isEqualTo(150);
        verify(warehouseRepo).save(testWarehouse);
        verify(stockRepo).delete(testStock);
    }

    @Test
    @DisplayName("requestStockTransfer() - should create pending transfer request with candidate warehouses")
    void requestStockTransfer_ShouldCreatePendingRequest() {
        StockLevel source = stock(2L, 10L, 75, 0, 5, 1000);
        StockTransferRequest saved = StockTransferRequest.builder()
                .requestId(77L)
                .productId(10L)
                .requestingWarehouseId(1L)
                .quantity(25)
                .reason("Need stock")
                .requestedBy("SYSTEM")
                .status(TransferRequestStatus.PENDING)
                .build();

        when(warehouseRepo.findById(1L)).thenReturn(Optional.of(testWarehouse));
        when(stockRepo.findByProductId(10L)).thenReturn(List.of(testStock, source));
        when(transferRequestRepo.save(any(StockTransferRequest.class))).thenReturn(saved);

        StockTransferRequest result = warehouseService.requestStockTransfer(10L, 1L, 25, "Need stock");

        assertThat(result.getStatus()).isEqualTo(TransferRequestStatus.PENDING);
        verify(stockEventPublisher).publishTransferRequestEvent(77L, 10L, 1L, 25, "Need stock", "SYSTEM", List.of(2L));
    }

    @Test
    @DisplayName("requestStockTransfer() - should reject non-positive quantity")
    void requestStockTransfer_ShouldRejectNonPositiveQuantity() {
        assertThatThrownBy(() -> warehouseService.requestStockTransfer(10L, 1L, 0, "Need stock"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    @DisplayName("requestStockTransfer() - should reject when no candidate warehouse has stock")
    void requestStockTransfer_ShouldRejectWhenNoCandidateHasStock() {
        when(warehouseRepo.findById(1L)).thenReturn(Optional.of(testWarehouse));
        when(stockRepo.findByProductId(10L)).thenReturn(List.of(testStock));

        assertThatThrownBy(() -> warehouseService.requestStockTransfer(10L, 1L, 25, "Need stock"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No other warehouse");
    }

    @Test
    @DisplayName("acceptTransferRequest() - should execute transfer and complete request")
    void acceptTransferRequest_ShouldExecuteTransferAndCompleteRequest() {
        StockTransferRequest request = StockTransferRequest.builder()
                .requestId(77L)
                .productId(10L)
                .requestingWarehouseId(1L)
                .quantity(20)
                .reason("Need stock")
                .status(TransferRequestStatus.PENDING)
                .build();
        Warehouse sourceWarehouse = warehouse(2L, 100, 60);
        Warehouse targetWarehouse = warehouse(1L, 100, 10);
        StockLevel source = stock(2L, 10L, 60, 0, 5, 1000);
        StockLevel target = stock(1L, 10L, 10, 0, 5, 1000);

        when(transferRequestRepo.findById(77L)).thenReturn(Optional.of(request));
        when(stockRepo.findByWarehouseId(2L)).thenReturn(List.of(source));
        when(stockRepo.findByWarehouseId(1L)).thenReturn(List.of(target));
        when(warehouseRepo.findById(2L)).thenReturn(Optional.of(sourceWarehouse));
        when(warehouseRepo.findById(1L)).thenReturn(Optional.of(targetWarehouse));
        when(stockRepo.findByProductId(10L)).thenReturn(List.of(source, target));
        when(transferRequestRepo.save(any(StockTransferRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockTransferRequest result = warehouseService.acceptTransferRequest(77L, 2L);

        assertThat(source.getQuantity()).isEqualTo(40);
        assertThat(target.getQuantity()).isEqualTo(30);
        assertThat(sourceWarehouse.getUsedCapacity()).isEqualTo(40);
        assertThat(targetWarehouse.getUsedCapacity()).isEqualTo(30);
        assertThat(result.getStatus()).isEqualTo(TransferRequestStatus.COMPLETED);
        assertThat(result.getAcceptedFromWarehouseId()).isEqualTo(2L);
        verify(stockEventPublisher).publishTransferRequestCompleted(77L);
        verify(stockEventPublisher).publishTransferEvent(10L, 2L, 1L, 20, "REQUEST #77 | Need stock", "SYSTEM");
    }

    @Test
    @DisplayName("acceptTransferRequest() - should reject already handled request")
    void acceptTransferRequest_ShouldRejectAlreadyHandledRequest() {
        StockTransferRequest request = StockTransferRequest.builder()
                .requestId(77L)
                .status(TransferRequestStatus.COMPLETED)
                .build();
        when(transferRequestRepo.findById(77L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> warehouseService.acceptTransferRequest(77L, 2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already COMPLETED");
    }

    @Test
    @DisplayName("transferStock() - should reject insufficient source stock")
    void transferStock_ShouldRejectInsufficientSourceStock() {
        StockLevel source = stock(2L, 10L, 5, 0, 5, 1000);
        when(stockRepo.findByWarehouseId(2L)).thenReturn(List.of(source));

        assertThatThrownBy(() -> warehouseService.transferStock(10L, 2L, 1L, 20, "restock"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Insufficient stock");
    }

    // ─────────────────────────────────────────────
    // ISSUE STOCK
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("issueStock() - should deduct quantity for SALES issue")
    void issueStock_ShouldDeductQuantity_WhenValidSalesIssue() {
        testStock.setQuantity(50);
        testStock.setReservedQuantity(0);
        when(stockRepo.findByWarehouseId(1L)).thenReturn(List.of(testStock));
        when(warehouseRepo.findById(1L)).thenReturn(Optional.of(testWarehouse));
        when(stockRepo.save(any())).thenReturn(testStock);
        doNothing().when(stockEventPublisher).publishStockMovement(any(), any(), any(), any(), any());
        when(stockRepo.findByProductId(10L)).thenReturn(List.of(testStock));

        StockIssueRequest request = new StockIssueRequest();
        request.setWarehouseId(1L);
        request.setProductId(10L);
        request.setQuantity(10);
        request.setIssueType("SALES");
        request.setNotes("Sale to customer");

        warehouseService.issueStock(request);

        assertThat(testStock.getQuantity()).isEqualTo(40);
        verify(stockRepo).save(testStock);
    }

    @Test
    @DisplayName("issueStock() - should throw for invalid issue type")
    void issueStock_ShouldThrow_WhenInvalidIssueType() {
        StockIssueRequest request = new StockIssueRequest();
        request.setWarehouseId(1L);
        request.setProductId(10L);
        request.setQuantity(10);
        request.setIssueType("INVALID");

        assertThatThrownBy(() -> warehouseService.issueStock(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid issue type");
    }

    @Test
    @DisplayName("issueStock() - should throw when insufficient available stock")
    void issueStock_ShouldThrow_WhenInsufficientStock() {
        testStock.setQuantity(5);
        testStock.setReservedQuantity(0);
        when(stockRepo.findByWarehouseId(1L)).thenReturn(List.of(testStock));

        StockIssueRequest request = new StockIssueRequest();
        request.setWarehouseId(1L);
        request.setProductId(10L);
        request.setQuantity(100);
        request.setIssueType("SALES");

        assertThatThrownBy(() -> warehouseService.issueStock(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Insufficient available stock");
    }

    // ─────────────────────────────────────────────
    // WRITE-OFF STOCK
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("writeOffStock() - should deduct quantity for damaged goods")
    void writeOffStock_ShouldDeductQuantity_WhenValid() {
        testStock.setQuantity(50);
        when(stockRepo.findByWarehouseId(1L)).thenReturn(List.of(testStock));
        when(stockRepo.save(any())).thenReturn(testStock);
        when(warehouseRepo.findById(1L)).thenReturn(Optional.of(testWarehouse));
        doNothing().when(stockEventPublisher).publishStockMovement(any(), any(), any(), any(), any());
        when(stockRepo.findByProductId(10L)).thenReturn(List.of(testStock));

        StockWriteOffRequest request = new StockWriteOffRequest();
        request.setWarehouseId(1L);
        request.setProductId(10L);
        request.setQuantity(5);
        request.setWriteOffReason("DAMAGED");

        warehouseService.writeOffStock(request);

        assertThat(testStock.getQuantity()).isEqualTo(45);
        verify(stockRepo).save(testStock);
    }

    @Test
    @DisplayName("writeOffStock() - should throw when writing off more than available")
    void writeOffStock_ShouldThrow_WhenExceedsStock() {
        testStock.setQuantity(5);
        when(stockRepo.findByWarehouseId(1L)).thenReturn(List.of(testStock));

        StockWriteOffRequest request = new StockWriteOffRequest();
        request.setWarehouseId(1L);
        request.setProductId(10L);
        request.setQuantity(100);
        request.setWriteOffReason("EXPIRED");

        assertThatThrownBy(() -> warehouseService.writeOffStock(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot write off more than available stock");
    }

    // ─────────────────────────────────────────────
    // RETURN STOCK
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("returnStock() - should add quantity back for SUPPLIER_RETURN")
    void returnStock_ShouldAddQuantity_WhenSupplierReturn() {
        testStock.setQuantity(50);
        when(stockRepo.findByWarehouseId(1L)).thenReturn(List.of(testStock));
        when(stockRepo.save(any())).thenReturn(testStock);
        when(warehouseRepo.findById(1L)).thenReturn(Optional.of(testWarehouse));
        doNothing().when(stockEventPublisher).publishStockMovement(any(), any(), any(), any(), any());
        when(stockRepo.findByProductId(10L)).thenReturn(List.of(testStock));

        StockReturnRequest request = new StockReturnRequest();
        request.setWarehouseId(1L);
        request.setProductId(10L);
        request.setQuantity(10);
        request.setReturnType("SUPPLIER_RETURN");

        warehouseService.returnStock(request);

        assertThat(testStock.getQuantity()).isEqualTo(60);
        verify(stockRepo).save(testStock);
    }

    @Test
    @DisplayName("returnStock() - should throw for invalid return type")
    void returnStock_ShouldThrow_WhenInvalidReturnType() {
        StockReturnRequest request = new StockReturnRequest();
        request.setWarehouseId(1L);
        request.setProductId(10L);
        request.setQuantity(10);
        request.setReturnType("INVALID_TYPE");

        assertThatThrownBy(() -> warehouseService.returnStock(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid return type");
    }

    @Test
    @DisplayName("get transfer request queries should delegate to repository")
    void transferRequestQueries_ShouldDelegateToRepository() {
        StockTransferRequest pending = StockTransferRequest.builder()
                .requestId(77L)
                .status(TransferRequestStatus.PENDING)
                .build();
        when(transferRequestRepo.findByRequestingWarehouseIdOrderByRequestedAtDesc(1L)).thenReturn(List.of(pending));
        when(transferRequestRepo.findByStatusOrderByRequestedAtDesc(TransferRequestStatus.PENDING)).thenReturn(List.of(pending));

        assertThat(warehouseService.getTransferRequestsForWarehouse(1L)).containsExactly(pending);
        assertThat(warehouseService.getPendingTransferRequests()).containsExactly(pending);
    }

    private StockLevel stock(Long warehouseId, Long productId, int quantity, int reserved, int min, int max) {
        StockLevel stock = new StockLevel();
        stock.setWarehouseId(warehouseId);
        stock.setProductId(productId);
        stock.setQuantity(quantity);
        stock.setReservedQuantity(reserved);
        stock.setMinThreshold(min);
        stock.setMaxStockLevel(max);
        return stock;
    }

    private Warehouse warehouse(Long id, int capacity, int usedCapacity) {
        Warehouse warehouse = new Warehouse();
        warehouse.setWarehouseId(id);
        warehouse.setCapacity(capacity);
        warehouse.setUsedCapacity(usedCapacity);
        warehouse.setIsActive(true);
        return warehouse;
    }
}
