package com.stockpro.supplier.service.impl;

import com.stockpro.supplier.entity.Supplier;
import com.stockpro.supplier.repository.SupplierRepository;
import com.stockpro.supplier.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepo;

    // Create new supplier — isActive set to true by default
    @Override
    public Supplier createSupplier(Supplier supplier) {
        supplier.setIsActive(true);
        return supplierRepo.save(supplier);
    }

    // Get single supplier — throws RuntimeException if not found
    // GlobalExceptionHandler will return this as 400 Bad Request
    @Override
    public Supplier getById(Long id) {
        return supplierRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Supplier not found: " + id));
    }

    // Return all suppliers (active + inactive)
    @Override
    public List<Supplier> getAllSuppliers() {
        return supplierRepo.findAll();
    }

    // Search by name OR city OR country — uses custom @Query in repository
    // PDF §2.4: "Search suppliers by name, city, or country"
    @Override
    public List<Supplier> searchSuppliers(String query) {
        return supplierRepo.searchByName(query); // method name matches PDF spec
    }

    // Update editable fields — does NOT change rating or isActive
    @Override
    public Supplier updateSupplier(Long id, Supplier updated) {
        Supplier s = getById(id);
        s.setName(updated.getName());
        s.setContactPerson(updated.getContactPerson());
        s.setEmail(updated.getEmail());
        s.setPhone(updated.getPhone());
        s.setAddress(updated.getAddress());
        s.setCity(updated.getCity());
        s.setCountry(updated.getCountry());
        s.setTaxId(updated.getTaxId());
        s.setPaymentTerms(updated.getPaymentTerms());
        s.setLeadTimeDays(updated.getLeadTimeDays());
        return supplierRepo.save(s);
    }

    // Soft delete — sets isActive=false
    // PDF §4.5: "prevents new POs but preserves historical records"
    @Override
    public void deactivateSupplier(Long id) {
        Supplier s = getById(id);
        s.setIsActive(false);
        supplierRepo.save(s);
    }

    // Hard delete — Admin only (enforced by @PreAuthorize in controller)
    // 📌 NOTE: Using standard JpaRepository.deleteById() instead of
    // deleteBySupplierId() to avoid @Transactional issues with derived delete queries
    @Override
    public void deleteSupplier(Long id) {
        supplierRepo.deleteById(id);
    }

    // Filter suppliers by city — PDF §2.4 geo-filter
    @Override
    public List<Supplier> getByCity(String city) {
        return supplierRepo.findByCity(city);
    }

    // Filter suppliers by country — PDF §2.4 geo-filter
    @Override
    public List<Supplier> getByCountry(String country) {
        return supplierRepo.findByCountry(country);
    }

    // Update supplier performance rating
    // 📌 NOTE: PDF §2.4 says "Rate supplier performance after goods receipt"
    // Called by the Purchase Order service after a GRN (Goods Received Note) is recorded.
    // Sets the rating directly — the Purchase Order service calculates the score before calling this.
    @Override
    public Supplier updateRating(Long id, double rating) {
        Supplier s = getById(id);
        s.setRating(rating);
        return supplierRepo.save(s);
    }
}