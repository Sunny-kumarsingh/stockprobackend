package com.stockpro.alert.repository;

import com.stockpro.alert.entity.Alert;
import com.stockpro.alert.entity.AlertSeverity;
import com.stockpro.alert.entity.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    // Existing
    List<Alert> findByAcknowledgedFalse();
    long countByIsReadFalse();
    boolean existsByProductIdAndAlertTypeAndAcknowledgedFalse(Long productId, AlertType alertType);
    boolean existsByProductIdAndWarehouseIdAndAlertTypeAndAcknowledgedFalse(Long productId, Long warehouseId, AlertType alertType);
    boolean existsByTransferRequestIdAndWarehouseIdAndAlertTypeAndAcknowledgedFalse(Long transferRequestId, Long warehouseId, AlertType alertType);

    //  New filter queries (PDF §2.7)
    List<Alert> findByAlertType(AlertType alertType);
    List<Alert> findBySeverity(AlertSeverity severity);
    List<Alert> findByAcknowledged(Boolean acknowledged);
    List<Alert> findByAlertTypeAndSeverity(AlertType alertType, AlertSeverity severity);
    List<Alert> findByAlertTypeAndAcknowledged(AlertType alertType, Boolean acknowledged);
    List<Alert> findBySeverityAndAcknowledged(AlertSeverity severity, Boolean acknowledged);
    List<Alert> findByAlertTypeAndSeverityAndAcknowledged(AlertType alertType, AlertSeverity severity, Boolean acknowledged);

    // Warehouse-specific queries for incoming transfer notifications
    List<Alert> findByWarehouseIdAndAlertTypeAndAcknowledged(Long warehouseId, AlertType alertType, Boolean acknowledged);
    List<Alert> findByWarehouseIdAndAlertType(Long warehouseId, AlertType alertType);
    List<Alert> findByTransferRequestIdAndAlertTypeAndAcknowledged(Long transferRequestId, AlertType alertType, Boolean acknowledged);
}
