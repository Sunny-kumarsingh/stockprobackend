package com.stockpro.alert.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    private AlertSeverity severity;

    private String title;
    private String message;

    private Long productId;
    private Long warehouseId;
    private Long poId;
    private Long transferRequestId;

    private String recipientRole;
    private String recipientEmail;

    @Builder.Default
    private Boolean isRead = false;
    @Builder.Default
    private Boolean acknowledged = false;

    private LocalDateTime acknowledgedAt;
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
