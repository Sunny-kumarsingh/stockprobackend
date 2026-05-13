package com.stockpro.stockmovement;

import com.stockpro.stockmovement.controller.StockMovementController;
import com.stockpro.stockmovement.entity.MovementType;
import com.stockpro.stockmovement.entity.StockMovement;
import com.stockpro.stockmovement.service.StockMovementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockMovementControllerTest {

    @Mock
    private StockMovementService service;

    private StockMovementController controller;
    private StockMovement movement;

    @BeforeEach
    void setUp() {
        controller = new StockMovementController(service);
        movement = StockMovement.builder()
                .movementId(1L)
                .warehouseId(2L)
                .productId(3L)
                .quantity(4)
                .type(MovementType.IN)
                .reason("receipt")
                .timestamp(LocalDateTime.parse("2026-05-04T10:15:30"))
                .build();
    }

    @Test
    void saveMovementDelegatesToService() {
        when(service.saveMovement(movement)).thenReturn(movement);

        assertThat(controller.saveMovement(movement).getMovementId()).isEqualTo(1L);
        verify(service).saveMovement(movement);
    }

    @Test
    void getFilteredParsesDatesAndDelegates() {
        LocalDateTime from = LocalDateTime.parse("2026-05-01T00:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-05-04T00:00:00");
        when(service.getFiltered(2L, 3L, MovementType.IN, from, to)).thenReturn(List.of(movement));

        List<StockMovement> result = controller.getFiltered(
                2L, 3L, MovementType.IN, "2026-05-01T00:00:00", "2026-05-04T00:00:00");

        assertThat(result).hasSize(1);
    }

    @Test
    void getFilteredAcceptsBlankDatesAsNull() {
        when(service.getFiltered(null, null, null, null, null)).thenReturn(List.of());

        assertThat(controller.getFiltered(null, null, null, "", "")).isEmpty();
    }

    @Test
    void finderEndpointsDelegate() {
        when(service.getByWarehouse(2L)).thenReturn(List.of(movement));
        when(service.getByProduct(3L)).thenReturn(List.of(movement));
        when(service.getByWarehouseAndProduct(2L, 3L)).thenReturn(List.of(movement));

        assertThat(controller.getByWarehouse(2L)).hasSize(1);
        assertThat(controller.getByProduct(3L)).hasSize(1);
        assertThat(controller.getByWarehouseAndProduct(2L, 3L)).hasSize(1);
    }
}
