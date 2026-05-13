package com.stockpro.purchase;

import com.stockpro.purchase.client.ProductClient;
import com.stockpro.purchase.client.SupplierClient;
import com.stockpro.purchase.client.WarehouseClient;
import com.stockpro.purchase.dto.ProductDTO;
import com.stockpro.purchase.entity.*;
import com.stockpro.purchase.publisher.PurchaseOrderEventPublisher;
import com.stockpro.purchase.repository.PurchaseOrderRepository;
import com.stockpro.purchase.service.impl.PurchaseOrderServiceImpl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * JUnit 5 + Mockito unit tests for PurchaseOrderServiceImpl.
 * Tests: createPO, submitPO, approvePO, rejectPO, cancelPO, getPOs filter.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PurchaseOrderServiceImpl - Unit Tests")
class PurchaseOrderServiceImplTest {

    @Mock private PurchaseOrderRepository poRepository;
    @Mock private SupplierClient supplierClient;
    @Mock private ProductClient productClient;
    @Mock private WarehouseClient warehouseClient;
    @Mock private PurchaseOrderEventPublisher eventPublisher;

    @InjectMocks
    private PurchaseOrderServiceImpl purchaseService;

    private PurchaseOrder testPO;

    @BeforeEach
    void setUp() {
        testPO = new PurchaseOrder();
        testPO.setId(1L);                           //  correct: 'id' not 'poId'
        testPO.setSupplierId(10L);
        testPO.setWarehouseId(5L);
        testPO.setStatus(POStatus.DRAFT);
        testPO.setTotalAmount(5000.0);
        testPO.setReferenceNumber("PO-12345");
        testPO.setOrderDate(LocalDateTime.now());
        testPO.setItems(new ArrayList<>());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ─────────────────────────────────────────────
    // CREATE PO TESTS
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("createPO() - should create PO with DRAFT status when supplier is active")
    void createPO_ShouldCreateWithDraftStatus_WhenSupplierIsActive() {
        when(supplierClient.isSupplierActive(10L)).thenReturn(true);
        when(poRepository.save(any(PurchaseOrder.class))).thenReturn(testPO);

        PurchaseOrder result = purchaseService.createPO(testPO);

        assertThat(result).isNotNull();
        assertThat(testPO.getStatus()).isEqualTo(POStatus.DRAFT);
        assertThat(testPO.getReferenceNumber()).startsWith("PO-");
        verify(poRepository).save(testPO);
    }

    @Test
    @DisplayName("createPO() - should throw when supplier is inactive")
    void createPO_ShouldThrow_WhenSupplierIsInactive() {
        when(supplierClient.isSupplierActive(10L)).thenReturn(false);

        assertThatThrownBy(() -> purchaseService.createPO(testPO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Supplier is inactive");

        verify(poRepository, never()).save(any());
    }

    @Test
    @DisplayName("createPO() - should validate SKU and productId items and calculate totals")
    void createPO_ShouldValidateItemsAndCalculateTotal() {
        ProductDTO skuProduct = new ProductDTO();
        skuProduct.setProductId(101L);

        POLineItem skuItem = lineItem(null, "SKU-101", 2, 15.0, 0);
        POLineItem idItem = lineItem(202L, null, 3, 20.0, 0);
        testPO.setItems(new ArrayList<>(List.of(skuItem, idItem)));

        when(supplierClient.isSupplierActive(10L)).thenReturn(true);
        when(productClient.getProductBySku("SKU-101")).thenReturn(skuProduct);
        when(productClient.getProductById(202L)).thenReturn(new ProductDTO());
        when(poRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseOrder result = purchaseService.createPO(testPO);

        assertThat(result.getTotalAmount()).isEqualTo(90.0);
        assertThat(skuItem.getProductId()).isEqualTo(101L);
        assertThat(skuItem.getTotalCost()).isEqualTo(30.0);
        assertThat(idItem.getTotalCost()).isEqualTo(60.0);
        assertThat(skuItem.getPurchaseOrder()).isSameAs(testPO);
        assertThat(idItem.getPurchaseOrder()).isSameAs(testPO);
        verify(productClient).getProductBySku("SKU-101");
        verify(productClient).getProductById(202L);
    }

    @Test
    @DisplayName("createPO() - should reject line item without productId or SKU")
    void createPO_ShouldRejectLineItemWithoutProductReference() {
        testPO.setItems(new ArrayList<>(List.of(lineItem(null, null, 1, 10.0, 0))));
        when(supplierClient.isSupplierActive(10L)).thenReturn(true);

        assertThatThrownBy(() -> purchaseService.createPO(testPO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Line item must have either productId or sku");

        verify(poRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────
    // SUBMIT PO TESTS
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("submitPO() - should change status to PENDING and publish event")
    void submitPO_ShouldChangeToPending_AndPublishEvent() {
        testPO.setStatus(POStatus.DRAFT);
        when(poRepository.findById(1L)).thenReturn(Optional.of(testPO));
        when(poRepository.save(any(PurchaseOrder.class))).thenReturn(testPO);
        doNothing().when(eventPublisher).publish(any(), anyString());

        purchaseService.submitPO(1L);

        assertThat(testPO.getStatus()).isEqualTo(POStatus.PENDING);
        verify(eventPublisher).publish(any(), anyString());
    }

    @Test
    @DisplayName("submitPO() - should throw when PO not found")
    void submitPO_ShouldThrow_WhenPONotFound() {
        when(poRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> purchaseService.submitPO(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("PO not found");
    }

    // ─────────────────────────────────────────────
    // APPROVE PO TESTS
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("approvePO() - should change status to APPROVED when PENDING")
    void approvePO_ShouldChangeToApproved_WhenPending() {
        testPO.setStatus(POStatus.PENDING);
        when(poRepository.findById(1L)).thenReturn(Optional.of(testPO));
        when(poRepository.save(any(PurchaseOrder.class))).thenReturn(testPO);
        doNothing().when(eventPublisher).publish(any(), anyString());

        purchaseService.approvePO(1L);

        assertThat(testPO.getStatus()).isEqualTo(POStatus.APPROVED);
        verify(eventPublisher).publish(any(), anyString());
    }

    @Test
    @DisplayName("approvePO() - should throw when PO is not PENDING")
    void approvePO_ShouldThrow_WhenNotPending() {
        testPO.setStatus(POStatus.DRAFT);
        when(poRepository.findById(1L)).thenReturn(Optional.of(testPO));

        assertThatThrownBy(() -> purchaseService.approvePO(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Only pending PO can be approved");
    }

    // ─────────────────────────────────────────────
    // REJECT PO TESTS
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("rejectPO() - should set status to REJECTED with reason")
    void rejectPO_ShouldSetRejected_WithReason() {
        testPO.setStatus(POStatus.PENDING);
        when(poRepository.findById(1L)).thenReturn(Optional.of(testPO));
        when(poRepository.save(any(PurchaseOrder.class))).thenReturn(testPO);

        purchaseService.rejectPO(1L, "Budget exceeded");

        assertThat(testPO.getStatus()).isEqualTo(POStatus.REJECTED);
        assertThat(testPO.getCancelReason()).isEqualTo("Budget exceeded");
    }

    // ─────────────────────────────────────────────
    // CANCEL PO TESTS
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("cancelPO() - should set status to CANCELLED with reason")
    void cancelPO_ShouldSetCancelled_WithReason() {
        when(poRepository.findById(1L)).thenReturn(Optional.of(testPO));
        when(poRepository.save(any(PurchaseOrder.class))).thenReturn(testPO);

        purchaseService.cancelPO(1L, "Supplier unavailable");

        assertThat(testPO.getStatus()).isEqualTo(POStatus.CANCELLED);
        assertThat(testPO.getCancelReason()).isEqualTo("Supplier unavailable");
    }

    // RECEIVE GOODS TESTS

    @Test
    @DisplayName("receiveGoods() - should partially receive goods and publish event")
    void receiveGoods_ShouldPartiallyReceiveGoods() {
        POLineItem item = lineItem(55L, null, 10, 5.0, 2);
        testPO.setStatus(POStatus.APPROVED);
        testPO.setItems(new ArrayList<>(List.of(item)));

        when(poRepository.findById(1L)).thenReturn(Optional.of(testPO));
        when(poRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseOrder result = purchaseService.receiveGoods(1L, 55L, 3);

        assertThat(item.getReceivedQuantity()).isEqualTo(5);
        assertThat(result.getStatus()).isEqualTo(POStatus.PARTIALLY_RECEIVED);
        assertThat(result.getReceivedDate()).isNull();
        verify(warehouseClient).addStock(5L, 55L, 3, "PO Receipt: PO-12345");
        verify(eventPublisher).publish(any(PurchaseOrder.class), anyString());
    }

    @Test
    @DisplayName("receiveGoods() - should mark PO received when all items are received")
    void receiveGoods_ShouldMarkReceivedWhenComplete() {
        POLineItem item = lineItem(55L, null, 10, 5.0, 7);
        testPO.setStatus(POStatus.PARTIALLY_RECEIVED);
        testPO.setItems(new ArrayList<>(List.of(item)));

        when(poRepository.findById(1L)).thenReturn(Optional.of(testPO));
        when(poRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseOrder result = purchaseService.receiveGoods(1L, 55L, 3);

        assertThat(item.getReceivedQuantity()).isEqualTo(10);
        assertThat(result.getStatus()).isEqualTo(POStatus.RECEIVED);
        assertThat(result.getReceivedDate()).isNotNull();
    }

    @Test
    @DisplayName("receiveGoods() - should reject ineligible PO status")
    void receiveGoods_ShouldRejectIneligibleStatus() {
        testPO.setStatus(POStatus.DRAFT);
        testPO.setItems(new ArrayList<>(List.of(lineItem(55L, null, 10, 5.0, 0))));
        when(poRepository.findById(1L)).thenReturn(Optional.of(testPO));

        assertThatThrownBy(() -> purchaseService.receiveGoods(1L, 55L, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("PO not eligible for receiving");

        verify(warehouseClient, never()).addStock(anyLong(), anyLong(), anyInt(), anyString());
    }

    @Test
    @DisplayName("receiveGoods() - should reject item not in PO")
    void receiveGoods_ShouldRejectMissingItem() {
        testPO.setStatus(POStatus.APPROVED);
        testPO.setItems(new ArrayList<>(List.of(lineItem(55L, null, 10, 5.0, 0))));
        when(poRepository.findById(1L)).thenReturn(Optional.of(testPO));

        assertThatThrownBy(() -> purchaseService.receiveGoods(1L, 99L, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Item not found");
    }

    @Test
    @DisplayName("receiveGoods() - should reject over receiving")
    void receiveGoods_ShouldRejectOverReceiving() {
        testPO.setStatus(POStatus.APPROVED);
        testPO.setItems(new ArrayList<>(List.of(lineItem(55L, null, 10, 5.0, 8))));
        when(poRepository.findById(1L)).thenReturn(Optional.of(testPO));

        assertThatThrownBy(() -> purchaseService.receiveGoods(1L, 55L, 3))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Received quantity exceeds ordered");
    }

    @Test
    @DisplayName("receiveGoods() - should deny manager from another warehouse")
    void receiveGoods_ShouldDenyManagerFromAnotherWarehouse() {
        testPO.setStatus(POStatus.APPROVED);
        testPO.setItems(new ArrayList<>(List.of(lineItem(55L, null, 10, 5.0, 0))));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "manager",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))
        );
        auth.setDetails("Main Warehouse");
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(poRepository.findById(1L)).thenReturn(Optional.of(testPO));
        when(warehouseClient.getAllWarehouses()).thenReturn(List.of(Map.of("warehouseId", 99L, "name", "Main Warehouse")));

        assertThatThrownBy(() -> purchaseService.receiveGoods(1L, 55L, 1))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("assigned warehouse");
    }

    // ─────────────────────────────────────────────
    // GET POs FILTER TESTS
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("getPOs() - should return all when no filter")
    void getPOs_ShouldReturnAll_WhenNoFilter() {
        POLineItem item = lineItem(55L, "SKU-55", 10, 5.0, 4);
        item.setId(7L);
        item.setTotalCost(50.0);
        item.setPurchaseOrder(testPO);
        testPO.setItems(new ArrayList<>(List.of(item)));
        when(poRepository.findAll()).thenReturn(List.of(testPO));

        List<PurchaseOrder> result = purchaseService.getPOs(null, null, null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isNotSameAs(testPO);
        assertThat(result.get(0).getItems()).hasSize(1);
        assertThat(result.get(0).getItems().get(0).getPurchaseOrder()).isNull();
        assertThat(result.get(0).getItems().get(0).getSku()).isEqualTo("SKU-55");
        verify(poRepository).findAll();
    }

    @Test
    @DisplayName("getPOs() - should filter by supplierId")
    void getPOs_ShouldFilterBySupplierId() {
        when(poRepository.findBySupplierId(10L)).thenReturn(List.of(testPO));

        List<PurchaseOrder> result = purchaseService.getPOs(10L, null, null, null, null);

        assertThat(result).hasSize(1);
        verify(poRepository).findBySupplierId(10L);
    }

    @Test
    @DisplayName("getPOs() - should filter by status")
    void getPOs_ShouldFilterByStatus() {
        when(poRepository.findByStatus(POStatus.DRAFT)).thenReturn(List.of(testPO));

        List<PurchaseOrder> result = purchaseService.getPOs(null, null, "DRAFT", null, null);

        assertThat(result).hasSize(1);
        verify(poRepository).findByStatus(POStatus.DRAFT);
    }

    @Test
    @DisplayName("getPOs() - should filter by warehouseId")
    void getPOs_ShouldFilterByWarehouseId() {
        when(poRepository.findByWarehouseId(5L)).thenReturn(List.of(testPO));

        List<PurchaseOrder> result = purchaseService.getPOs(null, 5L, null, null, null);

        assertThat(result).hasSize(1);
        verify(poRepository).findByWarehouseId(5L);
    }

    @Test
    @DisplayName("getPOs() - should filter by order date range")
    void getPOs_ShouldFilterByDateRange() {
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();
        when(poRepository.findByOrderDateBetween(start, end)).thenReturn(List.of(testPO));

        List<PurchaseOrder> result = purchaseService.getPOs(null, null, null, start, end);

        assertThat(result).hasSize(1);
        verify(poRepository).findByOrderDateBetween(start, end);
    }

    @Test
    @DisplayName("resolveWarehouseId() - should return matching numeric warehouse id")
    void resolveWarehouseId_ShouldReturnMatchingNumericId() {
        when(warehouseClient.getAllWarehouses()).thenReturn(List.of(
                Map.of("warehouseId", 5L, "name", "Central"),
                Map.of("warehouseId", 8, "name", "North")
        ));

        assertThat(purchaseService.resolveWarehouseId("North")).isEqualTo(8L);
    }

    @Test
    @DisplayName("resolveWarehouseId() - should return null when lookup fails")
    void resolveWarehouseId_ShouldReturnNullWhenLookupFails() {
        when(warehouseClient.getAllWarehouses()).thenThrow(new RuntimeException("down"));

        assertThat(purchaseService.resolveWarehouseId("North")).isNull();
    }

    private POLineItem lineItem(Long productId, String sku, int orderedQuantity, double unitCost, int receivedQuantity) {
        return POLineItem.builder()
                .productId(productId)
                .sku(sku)
                .orderedQuantity(orderedQuantity)
                .unitCost(unitCost)
                .receivedQuantity(receivedQuantity)
                .build();
    }
}
