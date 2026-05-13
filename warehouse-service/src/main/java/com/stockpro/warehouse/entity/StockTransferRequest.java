package com.stockpro.warehouse.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_transfer_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransferRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long requestId;

    private Long productId;
    private Long requestingWarehouseId;
    private Long acceptedFromWarehouseId;
    private Integer quantity;
    private String reason;
    private String requestedBy;
    private String acceptedBy;

    @Enumerated(EnumType.STRING)
    private TransferRequestStatus status;

    private LocalDateTime requestedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime completedAt;
}
