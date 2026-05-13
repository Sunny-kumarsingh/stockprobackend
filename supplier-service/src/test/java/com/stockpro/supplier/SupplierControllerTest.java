package com.stockpro.supplier;

import com.stockpro.supplier.controller.SupplierController;
import com.stockpro.supplier.entity.Supplier;
import com.stockpro.supplier.service.SupplierService;
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
class SupplierControllerTest {

    @Mock
    private SupplierService service;

    private SupplierController controller;
    private Supplier supplier;

    @BeforeEach
    void setUp() {
        controller = new SupplierController(service);
        supplier = new Supplier();
        supplier.setSupplierId(1L);
        supplier.setName("ABC Supplies");
        supplier.setCity("Mumbai");
        supplier.setCountry("India");
        supplier.setIsActive(true);
    }

    @Test
    void createAndReadEndpointsDelegate() {
        when(service.createSupplier(supplier)).thenReturn(supplier);
        when(service.getAllSuppliers()).thenReturn(List.of(supplier));
        when(service.getById(1L)).thenReturn(supplier);

        assertThat(controller.create(supplier).getName()).isEqualTo("ABC Supplies");
        assertThat(controller.getAll()).hasSize(1);
        assertThat(controller.getById(1L).getSupplierId()).isEqualTo(1L);
        assertThat(controller.isSupplierActive(1L)).isTrue();
    }

    @Test
    void searchAndFilterEndpointsDelegate() {
        when(service.searchSuppliers("ABC")).thenReturn(List.of(supplier));
        when(service.getByCity("Mumbai")).thenReturn(List.of(supplier));
        when(service.getByCountry("India")).thenReturn(List.of(supplier));

        assertThat(controller.search("ABC")).hasSize(1);
        assertThat(controller.getByCity("Mumbai")).hasSize(1);
        assertThat(controller.getByCountry("India")).hasSize(1);
    }

    @Test
    void mutationEndpointsReturnExpectedResponses() {
        when(service.updateSupplier(1L, supplier)).thenReturn(supplier);
        when(service.updateRating(1L, 4.2)).thenReturn(supplier);

        assertThat(controller.update(1L, supplier).getSupplierId()).isEqualTo(1L);
        assertThat(controller.updateRating(1L, 4.2).getName()).isEqualTo("ABC Supplies");
        assertThat(controller.deactivate(1L)).contains("deactivated");
        assertThat(controller.delete(1L)).contains("deleted");

        verify(service).deactivateSupplier(1L);
        verify(service).deleteSupplier(1L);
    }

    @Test
    void activeCheckReturnsFalseForNullOrInactiveSupplier() {
        supplier.setIsActive(false);
        when(service.getById(1L)).thenReturn(supplier);
        when(service.getById(2L)).thenReturn(null);

        assertThat(controller.isSupplierActive(1L)).isFalse();
        assertThat(controller.isSupplierActive(2L)).isFalse();
    }
}
