package com.stockpro.alert;

import com.stockpro.alert.entity.Alert;
import com.stockpro.alert.entity.AlertSeverity;
import com.stockpro.alert.entity.AlertType;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * JUnit 5 + Mockito unit tests for AlertServiceImpl.
 * Tests: createAlert, email for CRITICAL, duplicate suppression, acknowledge, unreadCount.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AlertServiceImpl - Unit Tests")
class AlertServiceApplicationTests {

	@Mock
	private AlertRepository repo;

	@Mock
	private JavaMailSender mailSender;

	@InjectMocks
	private AlertServiceImpl alertService;

	private Alert savedAlert;

	@BeforeEach
	void setUp() {
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

	@Test
	@DisplayName("createAlert() - should save alert and return it")
	void createAlert_ShouldSaveAndReturn() {
		when(repo.existsByProductIdAndAlertTypeAndAcknowledgedFalse(1L, AlertType.LOW_STOCK))
				.thenReturn(false);
		when(repo.save(any(Alert.class))).thenReturn(savedAlert);
		doNothing().when(mailSender).send(any(SimpleMailMessage.class));

		Alert result = alertService.createAlert(
				AlertType.LOW_STOCK, AlertSeverity.CRITICAL,
				"Low Stock Alert", "Product #1 is low: only 3 units remaining",
				1L, 1L, null
		);

		assertThat(result).isNotNull();
		assertThat(result.getAlertType()).isEqualTo(AlertType.LOW_STOCK);
		verify(repo).save(any(Alert.class));
	}

	@Test
	@DisplayName("createAlert() - should send email for CRITICAL severity")
	void createAlert_ShouldSendEmail_ForCritical() {
		when(repo.existsByProductIdAndAlertTypeAndAcknowledgedFalse(anyLong(), any()))
				.thenReturn(false);
		when(repo.save(any(Alert.class))).thenReturn(savedAlert);
		doNothing().when(mailSender).send(any(SimpleMailMessage.class));

		alertService.createAlert(AlertType.LOW_STOCK, AlertSeverity.CRITICAL,
				"Low Stock Alert", "Only 3 units", 1L, 1L, null);

		verify(mailSender).send(any(SimpleMailMessage.class));
	}

	@Test
	@DisplayName("createAlert() - should NOT send email for WARNING severity")
	void createAlert_ShouldNotSendEmail_ForWarning() {
		when(repo.existsByProductIdAndAlertTypeAndAcknowledgedFalse(anyLong(), any()))
				.thenReturn(false);
		Alert warningAlert = Alert.builder()
				.alertType(AlertType.OVERSTOCK).severity(AlertSeverity.WARNING)
				.isRead(false).acknowledged(false).createdAt(LocalDateTime.now()).build();
		when(repo.save(any(Alert.class))).thenReturn(warningAlert);

		alertService.createAlert(AlertType.OVERSTOCK, AlertSeverity.WARNING,
				"Overstock", "Too many units", 1L, 1L, null);

		verify(mailSender, never()).send(any(SimpleMailMessage.class));
	}

	@Test
	@DisplayName("createAlert() - should return null and not save duplicate active alerts")
	void createAlert_ShouldSuppressDuplicate() {
		when(repo.existsByProductIdAndAlertTypeAndAcknowledgedFalse(1L, AlertType.LOW_STOCK))
				.thenReturn(true);

		Alert result = alertService.createAlert(AlertType.LOW_STOCK, AlertSeverity.CRITICAL,
				"Low Stock", "Only 3 units", 1L, 1L, null);

		assertThat(result).isNull();
		verify(repo, never()).save(any());
	}

	@Test
	@DisplayName("acknowledgeAlert() - should mark alert as acknowledged and read")
	void acknowledgeAlert_ShouldUpdateAlert() {
		savedAlert.setAcknowledged(false);
		savedAlert.setIsRead(false);
		when(repo.findById(1L)).thenReturn(Optional.of(savedAlert));
		// thenAnswer returns the actual mutated object so assertions reflect changes
		when(repo.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

		Alert result = alertService.acknowledgeAlert(1L);

		// Alert.acknowledged is Boolean (wrapper) → use getAcknowledged(), not isAcknowledged()
		assertThat(result.getAcknowledged()).isTrue();
		assertThat(result.getIsRead()).isTrue();
		assertThat(result.getAcknowledgedAt()).isNotNull();
	}

	@Test
	@DisplayName("acknowledgeAlert() - should throw when alert not found")
	void acknowledgeAlert_ShouldThrow_WhenNotFound() {
		when(repo.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> alertService.acknowledgeAlert(99L))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("Alert not found");
	}

	@Test
	@DisplayName("getAllAlerts() - should return all alerts")
	void getAllAlerts_ShouldReturnAll() {
		when(repo.findAll()).thenReturn(List.of(savedAlert));

		List<Alert> result = alertService.getAllAlerts();

		assertThat(result).hasSize(1);
	}

	@Test
	@DisplayName("getUnreadCount() - should return correct count")
	void getUnreadCount_ShouldReturnCount() {
		when(repo.countByIsReadFalse()).thenReturn(5L);

		long count = alertService.getUnreadCount();

		assertThat(count).isEqualTo(5L);
	}
}
