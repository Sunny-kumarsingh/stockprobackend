package com.stockpro.stockmovement;

import com.stockpro.stockmovement.entity.MovementType;
import com.stockpro.stockmovement.entity.StockMovement;
import com.stockpro.stockmovement.repository.StockMovementRepository;
import com.stockpro.stockmovement.service.impl.StockMovementServiceImpl;

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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * JUnit 5 + Mockito unit tests for StockMovementServiceImpl.
 * Uses exact StockMovement entity fields: movementId, warehouseId, productId, quantity, type, reason.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StockMovementServiceImpl - Unit Tests")
class StockMovementServiceImplTest {

    @Mock
    private StockMovementRepository movementRepo;

    @InjectMocks
    private StockMovementServiceImpl movementService;

    private StockMovement testMovement;

    @BeforeEach
    void setUp() {
        // ✅ Use builder with correct field names from entity
        testMovement = StockMovement.builder()
                .movementId(1L)
                .productId(10L)
                .warehouseId(5L)
                .quantity(50)
                .type(MovementType.IN)
                .reason("Purchase Order Receipt")
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ─────────────────────────────────────────────
    // RECORD MOVEMENT TESTS
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("saveMovement() - should save and return the stock movement")
    void saveMovement_ShouldSaveAndReturn() {
        when(movementRepo.save(any(StockMovement.class))).thenReturn(testMovement);

        StockMovement result = movementService.saveMovement(testMovement);

        assertThat(result).isNotNull();
        assertThat(result.getMovementId()).isEqualTo(1L);
        assertThat(result.getType()).isEqualTo(MovementType.IN);
        assertThat(result.getQuantity()).isEqualTo(50);
        verify(movementRepo).save(testMovement);
    }

    @Test
    @DisplayName("saveMovement() - should save ADJUSTMENT type with negative quantity")
    void saveMovement_ShouldSaveAdjustment_WithNegativeQty() {
        StockMovement adjustment = StockMovement.builder()
                .productId(10L).warehouseId(5L)
                .quantity(-10).type(MovementType.ADJUSTMENT)
                .reason("Manual correction").build();
        when(movementRepo.save(any(StockMovement.class))).thenReturn(adjustment);

        StockMovement result = movementService.saveMovement(adjustment);

        assertThat(result.getType()).isEqualTo(MovementType.ADJUSTMENT);
        assertThat(result.getQuantity()).isEqualTo(-10);
    }

    // ─────────────────────────────────────────────
    // GET BY WAREHOUSE TESTS
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("getByWarehouse() - should return movements for given warehouseId")
    void getByWarehouse_ShouldReturnMovements() {
        when(movementRepo.findByWarehouseIdOrderByTimestampDesc(5L))
                .thenReturn(List.of(testMovement));

        List<StockMovement> result = movementService.getByWarehouse(5L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getWarehouseId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("getByWarehouse() - should return empty list when no movements")
    void getByWarehouse_ShouldReturnEmpty_WhenNone() {
        when(movementRepo.findByWarehouseIdOrderByTimestampDesc(99L)).thenReturn(List.of());

        List<StockMovement> result = movementService.getByWarehouse(99L);

        assertThat(result).isEmpty();
    }

    // ─────────────────────────────────────────────
    // GET BY PRODUCT TESTS
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("getByProduct() - should return movements for given productId")
    void getByProduct_ShouldReturnMovements() {
        when(movementRepo.findByProductIdOrderByTimestampDesc(10L))
                .thenReturn(List.of(testMovement));

        List<StockMovement> result = movementService.getByProduct(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProductId()).isEqualTo(10L);
    }

    // ─────────────────────────────────────────────
    // GET BY WAREHOUSE AND PRODUCT TESTS
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("getByWarehouseAndProduct() - should return filtered movements")
    void getByWarehouseAndProduct_ShouldReturnFiltered() {
        when(movementRepo.findByWarehouseIdAndProductIdOrderByTimestampDesc(5L, 10L))
                .thenReturn(List.of(testMovement));

        List<StockMovement> result = movementService.getByWarehouseAndProduct(5L, 10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getWarehouseId()).isEqualTo(5L);
        assertThat(result.get(0).getProductId()).isEqualTo(10L);
    }

    // ─────────────────────────────────────────────
    // GET FILTERED TESTS
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("getFiltered() - should call findFiltered with all params")
    void getFiltered_ShouldReturnFilteredResults() {
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();
        when(movementRepo.findFiltered(5L, 10L, MovementType.IN, from, to))
                .thenReturn(List.of(testMovement));

        List<StockMovement> result = movementService.getFiltered(5L, 10L, MovementType.IN, from, to);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo(MovementType.IN);
    }

    @Test
    @DisplayName("getFiltered() - should return empty when no matching movements")
    void getFiltered_ShouldReturnEmpty_WhenNoMatch() {
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now();
        when(movementRepo.findFiltered(null, null, null, from, to)).thenReturn(List.of());

        List<StockMovement> result = movementService.getFiltered(null, null, null, from, to);

        assertThat(result).isEmpty();
    }
}
