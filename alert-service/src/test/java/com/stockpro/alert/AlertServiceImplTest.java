package com.stockpro.alert;

import com.stockpro.alert.entity.Alert;
import com.stockpro.alert.entity.AlertSeverity;
import com.stockpro.alert.entity.AlertType;
import com.stockpro.alert.client.WarehouseClient;
import com.stockpro.alert.repository.AlertRepository;
import com.stockpro.alert.service.impl.AlertServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * JUnit 5 + Mockito unit tests for AlertServiceImpl.
 * NOTE: sendEmail() is a private method — we verify by checking mailSender.send() is called.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AlertServiceImpl - Unit Tests")
class AlertServiceImplTest {

    @Mock private AlertRepository repo;
    @Mock private JavaMailSender mailSender;
    @Mock private WarehouseClient warehouseClient;

    @InjectMocks
    private AlertServiceImpl alertService;

    private Alert savedAlert;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        savedAlert = Alert.builder()
                .alertType(AlertType.LOW_STOCK)
                .severity(AlertSeverity.CRITICAL)
                .title("Low Stock Alert")
                .message("Product #1 is low: only 3 units remaining")
                .productId(1L)
                .warehouseId(1L)
                .isRead(false)
                .acknowledged(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ─────────────────────────────────────────────
    // CREATE ALERT TESTS
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("createAlert() - should save alert and return it")
    void createAlert_ShouldSaveAndReturn() {
        when(repo.existsByProductIdAndAlertTypeAndAcknowledgedFalse(1L, AlertType.LOW_STOCK))
                .thenReturn(false);
        when(repo.save(any(Alert.class))).thenReturn(savedAlert);
        // mailSender.send() is called via private sendEmail() — lenient stub
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        Alert result = alertService.createAlert(
                AlertType.LOW_STOCK, AlertSeverity.CRITICAL,
                "Low Stock Alert", "Product #1 is low: only 3 units remaining",
                1L, 1L, null);

        assertThat(result).isNotNull();
        assertThat(result.getAlertType()).isEqualTo(AlertType.LOW_STOCK);
        assertThat(result.getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
        verify(repo).save(any(Alert.class));
    }

    @Test
    @DisplayName("createAlert() - should trigger mailSender.send() for CRITICAL alerts")
    void createAlert_ShouldCallMailSender_ForCritical() {
        when(repo.existsByProductIdAndAlertTypeAndAcknowledgedFalse(anyLong(), any()))
                .thenReturn(false);
        when(repo.save(any(Alert.class))).thenReturn(savedAlert);
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        alertService.createAlert(AlertType.LOW_STOCK, AlertSeverity.CRITICAL,
                "Low Stock", "Only 3 units", 1L, 1L, null);

        // sendEmail() is private but delegates to mailSender.send()
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("createAlert() - should NOT call mailSender.send() for WARNING alerts")
    void createAlert_ShouldNotCallMailSender_ForWarning() {
        Alert warningAlert = Alert.builder()
                .alertType(AlertType.OVERSTOCK).severity(AlertSeverity.WARNING)
                .isRead(false).acknowledged(false).createdAt(LocalDateTime.now()).build();

        when(repo.existsByProductIdAndAlertTypeAndAcknowledgedFalse(anyLong(), any()))
                .thenReturn(false);
        when(repo.save(any(Alert.class))).thenReturn(warningAlert);

        alertService.createAlert(AlertType.OVERSTOCK, AlertSeverity.WARNING,
                "Overstock", "Too many units", 1L, 1L, null);

        // WARNING does not trigger sendEmail()
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("createAlert() - should return null and NOT save when duplicate exists")
    void createAlert_ShouldSuppressDuplicate_WhenAlreadyExists() {
        when(repo.existsByProductIdAndAlertTypeAndAcknowledgedFalse(1L, AlertType.LOW_STOCK))
                .thenReturn(true);

        Alert result = alertService.createAlert(AlertType.LOW_STOCK, AlertSeverity.CRITICAL,
                "Low Stock", "Only 3 units", 1L, 1L, null);

        assertThat(result).isNull();
        verify(repo, never()).save(any());
    }

    // ─────────────────────────────────────────────
    // ACKNOWLEDGE ALERT TESTS
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("acknowledgeAlert() - should set acknowledged=true, isRead=true, acknowledgedAt")
    void acknowledgeAlert_ShouldMarkAcknowledged() {
        savedAlert.setAcknowledged(false);
        savedAlert.setIsRead(false);
        when(repo.findById(1L)).thenReturn(Optional.of(savedAlert));
        when(repo.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

        Alert result = alertService.acknowledgeAlert(1L);

        // Boolean field → getAcknowledged(), not isAcknowledged()
        assertThat(result.getAcknowledged()).isTrue();
        assertThat(result.getIsRead()).isTrue();
        assertThat(result.getAcknowledgedAt()).isNotNull();
        verify(repo).save(savedAlert);
    }

    @Test
    @DisplayName("acknowledgeAlert() - should throw RuntimeException when alert not found")
    void acknowledgeAlert_ShouldThrow_WhenNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertService.acknowledgeAlert(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Alert not found");
    }

    // ─────────────────────────────────────────────
    // GET ALL ALERTS & UNREAD COUNT
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("getAllAlerts() - should return all alerts from repository")
    void getAllAlerts_ShouldReturnAll() {
        when(repo.findAll()).thenReturn(List.of(savedAlert));

        List<Alert> result = alertService.getAllAlerts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
    }

    @Test
    @DisplayName("getUnreadCount() - should return count of unread alerts")
    void getUnreadCount_ShouldReturnCorrectCount() {
        when(repo.countByIsReadFalse()).thenReturn(7L);

        long count = alertService.getUnreadCount();

        assertThat(count).isEqualTo(7L);
    }

    @Test
    @DisplayName("getAllAlerts() - should show PAYMENT_DUE to admin")
    void getAllAlerts_ShouldShowPaymentDueToAdmin() {
        Alert paymentDue = Alert.builder()
                .alertType(AlertType.PAYMENT_DUE)
                .severity(AlertSeverity.CRITICAL)
                .title("Payment Due")
                .warehouseId(1L)
                .isRead(false)
                .acknowledged(false)
                .createdAt(LocalDateTime.now())
                .build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin@gmail.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        when(repo.findAll()).thenReturn(List.of(paymentDue));

        List<Alert> result = alertService.getAllAlerts();

        assertThat(result).containsExactly(paymentDue);
        verifyNoInteractions(warehouseClient);
    }

    @Test
    @DisplayName("getAllAlerts() - should show PAYMENT_DUE only to manager of related warehouse")
    void getAllAlerts_ShouldShowPaymentDueOnlyToRelatedWarehouseManager() {
        Alert paymentDue = Alert.builder()
                .alertType(AlertType.PAYMENT_DUE)
                .severity(AlertSeverity.CRITICAL)
                .title("Payment Due")
                .warehouseId(1L)
                .isRead(false)
                .acknowledged(false)
                .createdAt(LocalDateTime.now())
                .build();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "manager@gmail.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_MANAGER")));
        auth.setDetails("Main Warehouse");
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(repo.findAll()).thenReturn(List.of(paymentDue));
        when(warehouseClient.getWarehouseById(1L)).thenReturn(Map.of("name", "Main Warehouse"));

        List<Alert> result = alertService.getAllAlerts();

        assertThat(result).containsExactly(paymentDue);
    }

    @Test
    @DisplayName("createTransferRequestAlert() - should save transfer alert")
    void createTransferRequestAlert_ShouldSaveAlert() {
        when(repo.existsByTransferRequestIdAndWarehouseIdAndAlertTypeAndAcknowledgedFalse(
                10L, 1L, AlertType.STOCK_TRANSFER_REQUEST)).thenReturn(false);
        when(repo.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

        Alert result = alertService.createTransferRequestAlert(10L, "Transfer", "Move stock", 2L, 1L);

        assertThat(result.getAlertType()).isEqualTo(AlertType.STOCK_TRANSFER_REQUEST);
        assertThat(result.getTransferRequestId()).isEqualTo(10L);
        assertThat(result.getRecipientRole()).isEqualTo("STAFF");
        verify(repo).save(any(Alert.class));
    }

    @Test
    @DisplayName("createTransferRequestAlert() - should suppress duplicate transfer alert")
    void createTransferRequestAlert_ShouldSuppressDuplicate() {
        when(repo.existsByTransferRequestIdAndWarehouseIdAndAlertTypeAndAcknowledgedFalse(
                10L, 1L, AlertType.STOCK_TRANSFER_REQUEST)).thenReturn(true);

        Alert result = alertService.createTransferRequestAlert(10L, "Transfer", "Move stock", 2L, 1L);

        assertThat(result).isNull();
        verify(repo, never()).save(any(Alert.class));
    }

    @Test
    @DisplayName("getFilteredAlerts() - should use matching repository queries")
    void getFilteredAlerts_ShouldUseMatchingRepositoryQueries() {
        when(repo.findByAlertTypeAndSeverityAndAcknowledged(
                AlertType.LOW_STOCK, AlertSeverity.CRITICAL, false)).thenReturn(List.of(savedAlert));
        when(repo.findByAlertTypeAndSeverity(AlertType.LOW_STOCK, AlertSeverity.CRITICAL)).thenReturn(List.of(savedAlert));
        when(repo.findByAlertTypeAndAcknowledged(AlertType.LOW_STOCK, false)).thenReturn(List.of(savedAlert));
        when(repo.findBySeverityAndAcknowledged(AlertSeverity.CRITICAL, false)).thenReturn(List.of(savedAlert));
        when(repo.findByAlertType(AlertType.LOW_STOCK)).thenReturn(List.of(savedAlert));
        when(repo.findBySeverity(AlertSeverity.CRITICAL)).thenReturn(List.of(savedAlert));
        when(repo.findByAcknowledged(false)).thenReturn(List.of(savedAlert));
        when(repo.findAll()).thenReturn(List.of(savedAlert));

        assertThat(alertService.getFilteredAlerts("LOW_STOCK", "CRITICAL", false)).containsExactly(savedAlert);
        assertThat(alertService.getFilteredAlerts("LOW_STOCK", "CRITICAL", null)).containsExactly(savedAlert);
        assertThat(alertService.getFilteredAlerts("LOW_STOCK", null, false)).containsExactly(savedAlert);
        assertThat(alertService.getFilteredAlerts(null, "CRITICAL", false)).containsExactly(savedAlert);
        assertThat(alertService.getFilteredAlerts("LOW_STOCK", null, null)).containsExactly(savedAlert);
        assertThat(alertService.getFilteredAlerts(null, "CRITICAL", null)).containsExactly(savedAlert);
        assertThat(alertService.getFilteredAlerts(null, null, false)).containsExactly(savedAlert);
        assertThat(alertService.getFilteredAlerts(null, null, null)).containsExactly(savedAlert);
    }

    @Test
    @DisplayName("markAsRead() - should set isRead true")
    void markAsRead_ShouldUpdateAlert() {
        savedAlert.setIsRead(false);
        when(repo.findById(1L)).thenReturn(Optional.of(savedAlert));
        when(repo.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

        Alert result = alertService.markAsRead(1L);

        assertThat(result.getIsRead()).isTrue();
        verify(repo).save(savedAlert);
    }

    @Test
    @DisplayName("markAsRead() - should throw when alert not found")
    void markAsRead_ShouldThrow_WhenNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertService.markAsRead(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Alert not found");
    }

    @Test
    @DisplayName("getPendingTransferAlerts() - should load pending transfer alerts by warehouse")
    void getPendingTransferAlerts_ShouldReturnPendingAlerts() {
        when(repo.findByWarehouseIdAndAlertTypeAndAcknowledged(
                1L, AlertType.STOCK_TRANSFER_REQUEST, false)).thenReturn(List.of(savedAlert));

        List<Alert> result = alertService.getPendingTransferAlerts(1L);

        assertThat(result).containsExactly(savedAlert);
    }

    @Test
    @DisplayName("acknowledgeTransferRequestAlerts() - should acknowledge each pending transfer alert")
    void acknowledgeTransferRequestAlerts_ShouldUpdatePendingAlerts() {
        savedAlert.setAcknowledged(false);
        savedAlert.setIsRead(false);
        when(repo.findByTransferRequestIdAndAlertTypeAndAcknowledged(
                10L, AlertType.STOCK_TRANSFER_REQUEST, false)).thenReturn(List.of(savedAlert));

        alertService.acknowledgeTransferRequestAlerts(10L);

        assertThat(savedAlert.getAcknowledged()).isTrue();
        assertThat(savedAlert.getIsRead()).isTrue();
        assertThat(savedAlert.getAcknowledgedAt()).isNotNull();
        verify(repo).save(savedAlert);
    }

    @Test
    @DisplayName("getUnreadCount() - should count visible unread alerts for authenticated users")
    void getUnreadCount_ShouldCountVisibleAlertsForAuthenticatedUser() {
        Alert unread = Alert.builder().warehouseId(null).isRead(false).build();
        Alert read = Alert.builder().warehouseId(null).isRead(true).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin@gmail.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        when(repo.findAll()).thenReturn(List.of(unread, read));

        long count = alertService.getUnreadCount();

        assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("getAllAlerts() - should hide warehouse alert when department does not match")
    void getAllAlerts_ShouldHideWarehouseAlert_WhenDepartmentDoesNotMatch() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "manager@gmail.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_MANAGER")));
        auth.setDetails("Other Warehouse");
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(repo.findAll()).thenReturn(List.of(savedAlert));
        when(warehouseClient.getWarehouseById(1L)).thenReturn(Map.of("name", "Main Warehouse"));

        List<Alert> result = alertService.getAllAlerts();

        assertThat(result).isEmpty();
    }
}
