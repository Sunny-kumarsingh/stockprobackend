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
 * JUnit 5 + Mockito unit tests for SupplierServiceImpl.
 * Tests: create, getById, update, deactivate, delete, search, rating.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SupplierServiceImpl - Unit Tests")
class SupplierServiceApplicationTests {

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
	}

	@Test
	@DisplayName("createSupplier() - should save with isActive=true")
	void createSupplier_ShouldSaveWithActiveTrue() {
		Supplier input = new Supplier();
		input.setName("New Supplier");
		when(supplierRepo.save(any(Supplier.class))).thenReturn(testSupplier);

		supplierService.createSupplier(input);

		assertThat(input.getIsActive()).isTrue();
		verify(supplierRepo).save(input);
	}

	@Test
	@DisplayName("getById() - should return supplier when found")
	void getById_ShouldReturn_WhenExists() {
		when(supplierRepo.findById(1L)).thenReturn(Optional.of(testSupplier));

		Supplier result = supplierService.getById(1L);

		assertThat(result.getName()).isEqualTo("ABC Supplies Ltd");
	}

	@Test
	@DisplayName("getById() - should throw when not found")
	void getById_ShouldThrow_WhenNotFound() {
		when(supplierRepo.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> supplierService.getById(99L))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("Supplier not found");
	}

	@Test
	@DisplayName("updateSupplier() - should update fields")
	void updateSupplier_ShouldUpdateFields() {
		Supplier updatedData = new Supplier();
		updatedData.setName("Updated Supplier");
		updatedData.setCity("Delhi");
		updatedData.setCountry("India");
		updatedData.setEmail("updated@mail.com");
		updatedData.setPhone("1111111111");
		updatedData.setPaymentTerms("Net 60");
		updatedData.setLeadTimeDays(14);

		when(supplierRepo.findById(1L)).thenReturn(Optional.of(testSupplier));
		when(supplierRepo.save(any(Supplier.class))).thenReturn(testSupplier);

		supplierService.updateSupplier(1L, updatedData);

		assertThat(testSupplier.getName()).isEqualTo("Updated Supplier");
		assertThat(testSupplier.getCity()).isEqualTo("Delhi");
		verify(supplierRepo).save(testSupplier);
	}

	@Test
	@DisplayName("deactivateSupplier() - should set isActive to false")
	void deactivateSupplier_ShouldSetInactive() {
		when(supplierRepo.findById(1L)).thenReturn(Optional.of(testSupplier));

		supplierService.deactivateSupplier(1L);

		assertThat(testSupplier.getIsActive()).isFalse();
		verify(supplierRepo).save(testSupplier);
	}

	@Test
	@DisplayName("deleteSupplier() - should call deleteById")
	void deleteSupplier_ShouldCallDeleteById() {
		doNothing().when(supplierRepo).deleteById(1L);

		supplierService.deleteSupplier(1L);

		verify(supplierRepo).deleteById(1L);
	}

	@Test
	@DisplayName("searchSuppliers() - should return matching results")
	void searchSuppliers_ShouldReturnResults() {
		when(supplierRepo.searchByName("ABC")).thenReturn(List.of(testSupplier));

		List<Supplier> result = supplierService.searchSuppliers("ABC");

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getName()).isEqualTo("ABC Supplies Ltd");
	}

	@Test
	@DisplayName("updateRating() - should update supplier rating")
	void updateRating_ShouldSetRating() {
		when(supplierRepo.findById(1L)).thenReturn(Optional.of(testSupplier));
		when(supplierRepo.save(any(Supplier.class))).thenReturn(testSupplier);

		supplierService.updateRating(1L, 4.8);

		assertThat(testSupplier.getRating()).isEqualTo(4.8);
		verify(supplierRepo).save(testSupplier);
	}

	@Test
	@DisplayName("getAllSuppliers() - should return all")
	void getAllSuppliers_ShouldReturnAll() {
		when(supplierRepo.findAll()).thenReturn(List.of(testSupplier));

		List<Supplier> result = supplierService.getAllSuppliers();

		assertThat(result).hasSize(1);
	}
}
