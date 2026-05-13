package com.stockpro.supplier.controller;

import com.stockpro.supplier.entity.Supplier;
import com.stockpro.supplier.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 📌 NOTE: This is 'SupplierResource' in PDF terminology
// Exposes /api/v1/suppliers endpoints as per PDF §4.5
// Security: @EnableMethodSecurity in SecurityConfig activates @PreAuthorize
@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    // POST /suppliers — create new supplier
    // 📌 Only ADMIN and MANAGER can create suppliers (PDF §2.4)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping
    public Supplier create(@RequestBody Supplier s) {
        return supplierService.createSupplier(s);
    }

    // GET /suppliers — all suppliers (no role restriction — all staff can view)
    @GetMapping
    public List<Supplier> getAll() {
        return supplierService.getAllSuppliers();
    }

    // GET /suppliers/{id} — single supplier by ID
    @GetMapping("/{id}")
    public Supplier getById(@PathVariable Long id) {
        return supplierService.getById(id);
    }

    // /suppliers/{id}/active — check if supplier is active
    @GetMapping("/{id}/active")
    public boolean isSupplierActive(@PathVariable Long id) {
        Supplier supplier = supplierService.getById(id);
        return supplier != null && Boolean.TRUE.equals(supplier.getIsActive());
    }

    // npm /suppliers/search?q= — search by name, city, or country
    // "Search suppliers by name, city, or country"
    @GetMapping("/search")
    public List<Supplier> search(@RequestParam String q) {
        return supplierService.searchSuppliers(q);
    }

    // GET /suppliers/city/{city} — geo-filter by city (PDF §4.5)
    @GetMapping("/city/{city}")
    public List<Supplier> getByCity(@PathVariable String city) {
        return supplierService.getByCity(city);
    }

    // GET /suppliers/country/{country} — geo-filter by country (PDF §4.5)
    @GetMapping("/country/{country}")
    public List<Supplier> getByCountry(@PathVariable String country) {
        return supplierService.getByCountry(country);
    }

    // PUT /suppliers/{id} — update supplier details
    // 📌 ADMIN and MANAGER can edit (PDF §2.4)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PutMapping("/{id}")
    public Supplier update(@PathVariable Long id, @RequestBody Supplier s) {
        return supplierService.updateSupplier(id, s);
    }

    // PUT /suppliers/{id}/deactivate — soft delete (preserves PO history)
    // 📌 PDF §4.5: "deactivation flag to prevent new POs against inactive suppliers"
    // Only ADMIN can deactivate
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id) {
        supplierService.deactivateSupplier(id);
        return "Supplier deactivated ✅";
    }

    // PUT /suppliers/{id}/rating?score= — update performance rating after GRN
    // 📌 PDF §2.4: "Rate supplier performance after goods receipt"
    // Called manually or by Purchase Order service after goods receipt
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PutMapping("/{id}/rating")
    public Supplier updateRating(@PathVariable Long id, @RequestParam double score) {
        return supplierService.updateRating(id, score);
    }

    // DELETE /suppliers/{id} — hard delete
    // 📌 NOTE: Not explicitly in PDF SupplierResource spec, but added for Admin cleanup
    // ADMIN only — use deactivate for normal flow
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        supplierService.deleteSupplier(id);
        return "Supplier deleted ✅";
    }
}
