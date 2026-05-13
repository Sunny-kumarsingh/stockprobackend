package com.stockpro.payment.listener;

import com.stockpro.payment.config.RabbitMQConfig;
import com.stockpro.payment.dto.PurchaseOrderEvent;
import com.stockpro.payment.entity.Payment;
import com.stockpro.payment.entity.PaymentStatus;
import com.stockpro.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderPaymentListener {

    private static final String PAYMENT_REF_PREFIX = "Payment ";

    private final PaymentRepository paymentRepository;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitMQConfig.PO_RECEIVED_PAYMENT_QUEUE)
    public void handlePoReceived(PurchaseOrderEvent event) {
        if (event == null || !"RECEIVED".equalsIgnoreCase(event.getStatus())) {
            return;
        }

        if (event.getPoId() == null || paymentRepository.existsByPurchaseOrderId(event.getPoId())) {
            log.info("[Payment] Payment request already exists for PO {}", event == null ? null : event.getPoId());
            return;
        }

        Payment payment = Payment.builder()
                .purchaseOrderId(event.getPoId())
                .warehouseId(event.getWarehouseId())
                .supplierId(event.getSupplierId())
                .amount(event.getTotalAmount())
                .paymentMethod("Bank Transfer")
                .requestedBy("system")
                .remarks("Auto-created after goods received for " + event.getReferenceNumber())
                .status(PaymentStatus.PENDING)
                .build();

        Payment saved = paymentRepository.save(payment);
        log.info("[Payment] Auto-created {} for received PO {}", saved.getReferenceNumber(), event.getReferenceNumber());

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_REQUESTED,
                PAYMENT_REF_PREFIX + saved.getReferenceNumber() + " needs approval for PO " + event.getPoId());
    }
}
