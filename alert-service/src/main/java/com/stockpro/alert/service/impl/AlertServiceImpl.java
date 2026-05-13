package com.stockpro.alert.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.stockpro.alert.client.WarehouseClient;
import com.stockpro.alert.entity.Alert;
import com.stockpro.alert.entity.AlertSeverity;
import com.stockpro.alert.entity.AlertType;
import com.stockpro.alert.repository.AlertRepository;
import com.stockpro.alert.service.AlertService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertServiceImpl implements AlertService {

    private final AlertRepository repo;
    private final JavaMailSender mailSender;
    private final WarehouseClient warehouseClient;

    @Override
    public Alert createAlert(AlertType type, AlertSeverity severity,
                             String title, String message,
                             Long productId, Long warehouseId, Long poId) {

        // Deduplication: don't create duplicate active alerts for same product + warehouse + type
        if (productId != null) {
            boolean dup = repo.existsByProductIdAndAlertTypeAndAcknowledgedFalse(productId, type)
                    || (warehouseId != null
                    && repo.existsByProductIdAndWarehouseIdAndAlertTypeAndAcknowledgedFalse(productId, warehouseId, type));
            if (dup) {
                log.info(" Duplicate alert suppressed: product={}, warehouse={}, type={}", productId, warehouseId, type);
                return null;
            }
        }

        Alert alert = Alert.builder()
                .alertType(type)
                .severity(severity)
                .title(title)
                .message(message)
                .productId(productId)
                .warehouseId(warehouseId)
                .poId(poId)
                .recipientRole("MANAGER")
                .isRead(false)
                .acknowledged(false)
                .createdAt(LocalDateTime.now())
                .build();

        Alert saved = repo.save(alert);
        log.info(" Alert created: [{}] {} - {}", severity, title, message);

        //  PDF §2.7: Send email only for CRITICAL alerts
        if (severity == AlertSeverity.CRITICAL) {
            sendEmail(title, message);
        }

        return saved;
    }

    @Override
    public Alert createTransferRequestAlert(Long transferRequestId, String title, String message,
                                            Long productId, Long warehouseId) {
        if (transferRequestId != null && warehouseId != null
                && repo.existsByTransferRequestIdAndWarehouseIdAndAlertTypeAndAcknowledgedFalse(
                transferRequestId, warehouseId, AlertType.STOCK_TRANSFER_REQUEST)) {
            log.info(" Duplicate transfer request alert suppressed: request={}, warehouse={}",
                    transferRequestId, warehouseId);
            return null;
        }

        Alert alert = Alert.builder()
                .alertType(AlertType.STOCK_TRANSFER_REQUEST)
                .severity(AlertSeverity.INFO)
                .title(title)
                .message(message)
                .productId(productId)
                .warehouseId(warehouseId)
                .transferRequestId(transferRequestId)
                .recipientRole("STAFF")
                .isRead(false)
                .acknowledged(false)
                .createdAt(LocalDateTime.now())
                .build();

        Alert saved = repo.save(alert);
        log.info(" Transfer request alert created: request={}, warehouse={}", transferRequestId, warehouseId);
        return saved;
    }

    @Override
    public List<Alert> getAllAlerts() {
        return visibleToCurrentUser(repo.findAll());
    }

    @Override
    public List<Alert> getFilteredAlerts(String type, String severity, Boolean acknowledged) {
        AlertType alertType = (type != null) ? AlertType.valueOf(type.toUpperCase()) : null;
        AlertSeverity alertSeverity = (severity != null) ? AlertSeverity.valueOf(severity.toUpperCase()) : null;

        if (alertType != null && alertSeverity != null && acknowledged != null) {
            return visibleToCurrentUser(repo.findByAlertTypeAndSeverityAndAcknowledged(alertType, alertSeverity, acknowledged));
        } else if (alertType != null && alertSeverity != null) {
            return visibleToCurrentUser(repo.findByAlertTypeAndSeverity(alertType, alertSeverity));
        } else if (alertType != null && acknowledged != null) {
            return visibleToCurrentUser(repo.findByAlertTypeAndAcknowledged(alertType, acknowledged));
        } else if (alertSeverity != null && acknowledged != null) {
            return visibleToCurrentUser(repo.findBySeverityAndAcknowledged(alertSeverity, acknowledged));
        } else if (alertType != null) {
            return visibleToCurrentUser(repo.findByAlertType(alertType));
        } else if (alertSeverity != null) {
            return visibleToCurrentUser(repo.findBySeverity(alertSeverity));
        } else if (acknowledged != null) {
            return visibleToCurrentUser(repo.findByAcknowledged(acknowledged));
        }

        return visibleToCurrentUser(repo.findAll());
    }

    @Override
    public Alert acknowledgeAlert(Long id) {
        Alert alert = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Alert not found: " + id));

        alert.setAcknowledged(true);
        alert.setAcknowledgedAt(LocalDateTime.now());
        alert.setIsRead(true);

        return repo.save(alert);
    }

    @Override
    public long getUnreadCount() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            return repo.countByIsReadFalse();
        }

        return visibleToCurrentUser(repo.findAll()).stream()
                .filter(alert -> Boolean.FALSE.equals(alert.getIsRead()))
                .count();
    }

    @Override
    public Alert markAsRead(Long id) {
        Alert alert = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Alert not found: " + id));

        alert.setIsRead(true);
        return repo.save(alert);
    }

    // ✅ Email sender — only called for CRITICAL alerts (PDF §2.7)
    private void sendEmail(String subject, String body) {
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo("sunnykumarsingh2711@gmail.com"); // Manager's email
            mail.setSubject("[CRITICAL ALERT] " + subject);
            mail.setText(body);
            mailSender.send(mail);
            log.info("📧 CRITICAL email sent: {}", subject);
        } catch (Exception e) {
            log.error("❌ Failed to send email alert: {}", e.getMessage());
        }
    }

    @Override
    public List<Alert> getPendingTransferAlerts(Long warehouseId) {
        return repo.findByWarehouseIdAndAlertTypeAndAcknowledged(
                warehouseId, AlertType.STOCK_TRANSFER_REQUEST, false);
    }

    @Override
    public void acknowledgeTransferRequestAlerts(Long transferRequestId) {
        repo.findByTransferRequestIdAndAlertTypeAndAcknowledged(
                        transferRequestId, AlertType.STOCK_TRANSFER_REQUEST, false)
                .forEach(alert -> {
                    alert.setAcknowledged(true);
                    alert.setAcknowledgedAt(LocalDateTime.now());
                    alert.setIsRead(true);
                    repo.save(alert);
                });
    }

    private List<Alert> visibleToCurrentUser(List<Alert> alerts) {
        return alerts.stream()
                .filter(this::isVisibleToCurrentUser)
                .toList();
    }

    private boolean isVisibleToCurrentUser(Alert alert) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return true;
        }

        if (hasRole(auth, "ADMIN")) {
            return true;
        }

        if (alert.getWarehouseId() == null) {
            return true;
        }

        boolean isWarehouseUser = hasRole(auth, "MANAGER") || hasRole(auth, "STAFF");
        if (!isWarehouseUser) {
            return false;
        }

        String userDepartment = auth.getDetails() instanceof String ? (String) auth.getDetails() : null;
        if (userDepartment == null || userDepartment.isBlank()) {
            return false;
        }

        try {
            Object warehouseName = warehouseClient.getWarehouseById(alert.getWarehouseId()).get("name");
            return userDepartment.equals(warehouseName);
        } catch (Exception e) {
            log.warn("Could not resolve warehouse {} for alert visibility: {}",
                    alert.getWarehouseId(), e.getMessage());
            return false;
        }
    }

    private boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> ("ROLE_" + role).equals(a.getAuthority()));
    }
}
