package com.stockpro.supplier;

import com.stockpro.supplier.entity.Supplier;
import com.stockpro.supplier.repository.SupplierRepository;
import com.stockpro.supplier.service.impl.SupplierServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SupplierServiceImpl using JUnit 5 + Mockito.
 * Tests: create, getById, getAll, update, deactivate, delete,
 *        search, getByCity, getByCountry, updateRating edge cases.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SupplierServiceImpl - Unit Tests")
class SupplierServiceImplTest {

    @Mock
    private SupplierRepository supplierRepo;

    @InjectMocks
    private SupplierServiceImpl supplierService;

    private Supplier testSupplier;

    @BeforeEach
    void setUp() {
        testSupplier = new Supplier();
        testSupplier.setSupplierId(1L);
        testSupplier.setName("ABC Supplies Ltd");
        testSupplier.setEmail("contact@abcsupplies.com");
        testSupplier.setPhone("9876543210");
        testSupplier.setCity("Mumbai");
        testSupplier.setCountry("India");
        testSupplier.setIsActive(true);
        testSupplier.setRating(4.5);
        testSupplier.setPaymentTerms("Net 30");
        testSupplier.setLeadTimeDays(7);
        testSupplier.setContactPerson("Ravi Sharma");
    }

    // ─────────────────────────────────────────────
    // CREATE SUPPLIER
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("createSupplier() - should save and return supplier with isActive=true")
    void createSupplier_ShouldSaveAndReturn_WithActiveTrue() {
        Supplier input = new Supplier();
        input.setName("New Supplier");
        when(supplierRepo.save(any(Supplier.class))).thenReturn(testSupplier);

        Supplier result = supplierService.createSupplier(input);

        assertThat(result).isNotNull();
        assertThat(input.getIsActive()).isTrue();
        verify(supplierRepo).save(input);
    }

    @Test
    @DisplayName("createSupplier() - should always set isActive=true regardless of input")
    void createSupplier_ShouldForceActiveTrue_EvenIfInputIsInactive() {
        Supplier input = new Supplier();
        input.setName("Inactive Supplier Input");
        input.setIsActive(false); // intentionally false
        when(supplierRepo.save(any(Supplier.class))).thenReturn(testSupplier);

        supplierService.createSupplier(input);

        assertThat(input.getIsActive()).isTrue();
    }

    // ─────────────────────────────────────────────
    // GET BY ID
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("getById() - should return supplier when found")
    void getById_ShouldReturnSupplier_WhenExists() {
        when(supplierRepo.findById(1L)).thenReturn(Optional.of(testSupplier));

        Supplier result = supplierService.getById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("ABC Supplies Ltd");
        assertThat(result.getCity()).isEqualTo("Mumbai");
    }

    @Test
    @DisplayName("getById() - should throw RuntimeException when not found")
    void getById_ShouldThrow_WhenNotFound() {
        when(supplierRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.getById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Supplier not found");
    }

    // ─────────────────────────────────────────────
    // GET ALL SUPPLIERS
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("getAllSuppliers() - should return all suppliers")
    void getAllSuppliers_ShouldReturnAll() {
        when(supplierRepo.findAll()).thenReturn(List.of(testSupplier));

        List<Supplier> result = supplierService.getAllSuppliers();

        assertThat(result).hasSize(1);
        verify(supplierRepo).findAll();
    }

    @Test
    @DisplayName("getAllSuppliers() - should return empty list when none exist")
    void getAllSuppliers_ShouldReturnEmpty_WhenNone() {
        when(supplierRepo.findAll()).thenReturn(List.of());

        List<Supplier> result = supplierService.getAllSuppliers();

        assertThat(result).isEmpty();
    }

    // ─────────────────────────────────────────────
    // UPDATE SUPPLIER
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("updateSupplier() - should update all editable fields")
    void updateSupplier_ShouldUpdateFields_WhenFound() {
        Supplier updatedData = new Supplier();
        updatedData.setName("Updated Supplier");
        updatedData.setCity("Delhi");
        updatedData.setCountry("India");
        updatedData.setEmail("updated@mail.com");
        updatedData.setPhone("1111111111");
        updatedData.setPaymentTerms("Net 60");
        updatedData.setLeadTimeDays(14);
        updatedData.setContactPerson("New Contact");
        updatedData.setAddress("New Address");
        updatedData.setTaxId("GST123");

        when(supplierRepo.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(supplierRepo.save(any(Supplier.class))).thenReturn(testSupplier);

        Supplier result = supplierService.updateSupplier(1L, updatedData);

        assertThat(result).isSameAs(testSupplier);
        assertThat(testSupplier.getName()).isEqualTo("Updated Supplier");
        assertThat(testSupplier.getCity()).isEqualTo("Delhi");
        assertThat(testSupplier.getPaymentTerms()).isEqualTo("Net 60");
        assertThat(testSupplier.getLeadTimeDays()).isEqualTo(14);
        verify(supplierRepo).save(testSupplier);
    }

    @Test
    @DisplayName("updateSupplier() - should throw when supplier not found")
    void updateSupplier_ShouldThrow_WhenNotFound() {
        when(supplierRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.updateSupplier(99L, testSupplier))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Supplier not found");
    }

    // ─────────────────────────────────────────────
    // DEACTIVATE SUPPLIER
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("deactivateSupplier() - should set isActive to false")
    void deactivateSupplier_ShouldSetInactive_WhenFound() {
        testSupplier.setIsActive(true);
        when(supplierRepo.findById(1L)).thenReturn(Optional.of(testSupplier));

        supplierService.deactivateSupplier(1L);

        assertThat(testSupplier.getIsActive()).isFalse();
        verify(supplierRepo).save(testSupplier);
    }

    @Test
    @DisplayName("deactivateSupplier() - should throw when supplier not found")
    void deactivateSupplier_ShouldThrow_WhenNotFound() {
        when(supplierRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.deactivateSupplier(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Supplier not found");
    }

    // ─────────────────────────────────────────────
    // DELETE SUPPLIER
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("deleteSupplier() - should call deleteById on repository")
    void deleteSupplier_ShouldCallDeleteById() {
        doNothing().when(supplierRepo).deleteById(1L);

        supplierService.deleteSupplier(1L);

        verify(supplierRepo).deleteById(1L);
    }

    // ─────────────────────────────────────────────
    // SEARCH SUPPLIERS
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("searchSuppliers() - should return matching suppliers")
    void searchSuppliers_ShouldReturnResults_WhenMatchFound() {
        when(supplierRepo.searchByName("ABC")).thenReturn(List.of(testSupplier));

        List<Supplier> result = supplierService.searchSuppliers("ABC");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("ABC Supplies Ltd");
    }

    @Test
    @DisplayName("searchSuppliers() - should return empty list when no match")
    void searchSuppliers_ShouldReturnEmpty_WhenNoMatch() {
        when(supplierRepo.searchByName("XYZ")).thenReturn(List.of());

        List<Supplier> result = supplierService.searchSuppliers("XYZ");

        assertThat(result).isEmpty();
    }

    // ─────────────────────────────────────────────
    // GET BY CITY
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("getByCity() - should return suppliers in that city")
    void getByCity_ShouldReturnSuppliers_WhenCityMatches() {
        when(supplierRepo.findByCity("Mumbai")).thenReturn(List.of(testSupplier));

        List<Supplier> result = supplierService.getByCity("Mumbai");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCity()).isEqualTo("Mumbai");
    }

    @Test
    @DisplayName("getByCity() - should return empty when city not found")
    void getByCity_ShouldReturnEmpty_WhenCityNotFound() {
        when(supplierRepo.findByCity("London")).thenReturn(List.of());

        List<Supplier> result = supplierService.getByCity("London");

        assertThat(result).isEmpty();
    }

    // ─────────────────────────────────────────────
    // GET BY COUNTRY
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("getByCountry() - should return suppliers in that country")
    void getByCountry_ShouldReturnSuppliers_WhenCountryMatches() {
        when(supplierRepo.findByCountry("India")).thenReturn(List.of(testSupplier));

        List<Supplier> result = supplierService.getByCountry("India");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCountry()).isEqualTo("India");
    }

    @Test
    @DisplayName("getByCountry() - should return empty when country not found")
    void getByCountry_ShouldReturnEmpty_WhenCountryNotFound() {
        when(supplierRepo.findByCountry("Germany")).thenReturn(List.of());

        List<Supplier> result = supplierService.getByCountry("Germany");

        assertThat(result).isEmpty();
    }

    // ─────────────────────────────────────────────
    // UPDATE RATING
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("updateRating() - should update supplier rating")
    void updateRating_ShouldSetRating_WhenSupplierFound() {
        when(supplierRepo.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(supplierRepo.save(any(Supplier.class))).thenReturn(testSupplier);

        Supplier result = supplierService.updateRating(1L, 4.8);

        assertThat(result).isSameAs(testSupplier);
        assertThat(testSupplier.getRating()).isEqualTo(4.8);
        verify(supplierRepo).save(testSupplier);
    }

    @Test
    @DisplayName("updateRating() - should accept 0.0 as minimum rating")
    void updateRating_ShouldAcceptZeroRating() {
        when(supplierRepo.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(supplierRepo.save(any(Supplier.class))).thenReturn(testSupplier);

        Supplier result = supplierService.updateRating(1L, 0.0);

        assertThat(result).isSameAs(testSupplier);
        assertThat(testSupplier.getRating()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("updateRating() - should throw when supplier not found")
    void updateRating_ShouldThrow_WhenSupplierNotFound() {
        when(supplierRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.updateRating(99L, 3.5))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Supplier not found");
    }
}
