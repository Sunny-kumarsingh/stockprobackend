package com.stockpro.alert;

import com.stockpro.alert.controller.AlertController;
import com.stockpro.alert.entity.Alert;
import com.stockpro.alert.entity.AlertSeverity;
import com.stockpro.alert.entity.AlertType;
import com.stockpro.alert.service.AlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertControllerTest {

    @Mock
    private AlertService service;

    private AlertController controller;
    private Alert alert;

    @BeforeEach
    void setUp() {
        controller = new AlertController(service);
        alert = Alert.builder()
                .id(1L)
                .alertType(AlertType.LOW_STOCK)
                .severity(AlertSeverity.CRITICAL)
                .title("Low stock")
                .message("Restock product")
                .isRead(false)
                .acknowledged(false)
                .build();
    }

    @Test
    void getAllDelegatesWithFilters() {
        when(service.getFilteredAlerts("LOW_STOCK", "CRITICAL", false)).thenReturn(List.of(alert));

        List<Alert> result = controller.getAll("LOW_STOCK", "CRITICAL", false);

        assertThat(result).hasSize(1);
        verify(service).getFilteredAlerts("LOW_STOCK", "CRITICAL", false);
    }

    @Test
    void acknowledgeReadAndCountDelegate() {
        when(service.acknowledgeAlert(1L)).thenReturn(alert);
        when(service.markAsRead(1L)).thenReturn(alert);
        when(service.getUnreadCount()).thenReturn(3L);

        assertThat(controller.ack(1L).getId()).isEqualTo(1L);
        assertThat(controller.read(1L).getTitle()).isEqualTo("Low stock");
        assertThat(controller.count()).isEqualTo(3L);
    }
}
