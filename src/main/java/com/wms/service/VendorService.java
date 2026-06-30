package com.wms.service;

import com.wms.entity.Vendor;
import com.wms.repository.VendorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class VendorService {

    @Autowired
    private VendorRepository vendorRepository;

    public List<Vendor> getAllVendors() {
        return vendorRepository.findAll();
    }

    public Optional<Vendor> getVendorById(Long id) {
        return vendorRepository.findById(id);
    }

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN =
        Pattern.compile("^[+]?[0-9\\s\\-().]{7,20}$");

    private void validateVendor(Vendor vendor) {
        if (vendor.getName() == null || vendor.getName().trim().isEmpty()) {
            throw new RuntimeException("Vendor name is required.");
        }
        if (vendor.getCategory() == null || vendor.getCategory().trim().isEmpty()) {
            throw new RuntimeException("Vendor category is required.");
        }
        if (vendor.getEmail() != null && !vendor.getEmail().trim().isEmpty()) {
            if (!EMAIL_PATTERN.matcher(vendor.getEmail().trim()).matches()) {
                throw new RuntimeException("Please enter a valid email address.");
            }
        }
        if (vendor.getPhone() != null && !vendor.getPhone().trim().isEmpty()) {
            if (!PHONE_PATTERN.matcher(vendor.getPhone().trim()).matches()) {
                throw new RuntimeException("Please enter a valid phone number (7-20 digits).");
            }
        }
    }

    public Vendor addVendor(Vendor vendor) {
        validateVendor(vendor);
        return vendorRepository.save(vendor);
    }

    public Vendor updateVendor(Long id, Vendor updatedVendor) {
        validateVendor(updatedVendor);
        Vendor existing = vendorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vendor not found with id: " + id));
        existing.setName(updatedVendor.getName());
        existing.setCategory(updatedVendor.getCategory());
        existing.setEmail(updatedVendor.getEmail());
        existing.setPhone(updatedVendor.getPhone());
        existing.setAddress(updatedVendor.getAddress());
        existing.setPricingTier(updatedVendor.getPricingTier());
        existing.setDescription(updatedVendor.getDescription());
        existing.setStatus(updatedVendor.getStatus());
        return vendorRepository.save(existing);
    }

    public void deleteVendor(Long id) {
        if (!vendorRepository.existsById(id)) {
            throw new RuntimeException("Vendor not found with id: " + id);
        }
        vendorRepository.deleteById(id);
    }

    public List<Vendor> searchVendors(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return vendorRepository.findAll();
        }
        return vendorRepository.searchVendors(keyword.trim());
    }

    public List<Vendor> getVendorsByCategory(String category) {
        return vendorRepository.findByCategory(category);
    }

    public long getTotalVendors() {
        return vendorRepository.count();
    }
}
