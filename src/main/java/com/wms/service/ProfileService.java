package com.wms.service;

import com.wms.entity.Booking;
import com.wms.entity.Customer;
import com.wms.repository.BookingRepository;
import com.wms.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@org.springframework.transaction.annotation.Transactional
public class ProfileService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BookingRepository bookingRepository;

    // =================== CUSTOMER OPERATIONS ===================

    public Optional<Customer> customerLogin(String email, String password) {
        Optional<Customer> customer = customerRepository.findByEmail(email);
        if (customer.isPresent() && customer.get().getPassword().equals(password)) {
            return customer;
        }
        return Optional.empty();
    }

    private static final java.util.regex.Pattern EMAIL_PATTERN =
        java.util.regex.Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final java.util.regex.Pattern PHONE_PATTERN =
        java.util.regex.Pattern.compile("^[+]?[0-9\\s\\-().]{7,20}$");

    public Customer addCustomer(Customer customer) {
        if (customer.getFirstName() == null || customer.getFirstName().trim().isEmpty()) {
            throw new RuntimeException("First name is required.");
        }
        if (customer.getLastName() == null || customer.getLastName().trim().isEmpty()) {
            throw new RuntimeException("Last name is required.");
        }
        if (customer.getEmail() == null || customer.getEmail().trim().isEmpty()) {
            throw new RuntimeException("Email address is required.");
        }
        if (!EMAIL_PATTERN.matcher(customer.getEmail().trim()).matches()) {
            throw new RuntimeException("Please enter a valid email address.");
        }
        if (customer.getPassword() == null || customer.getPassword().length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters.");
        }
        if (customer.getPhone() != null && !customer.getPhone().trim().isEmpty()) {
            if (!PHONE_PATTERN.matcher(customer.getPhone().trim()).matches()) {
                throw new RuntimeException("Please enter a valid phone number (7-20 digits).");
            }
        }
        if (customerRepository.existsByEmail(customer.getEmail().trim().toLowerCase())) {
            throw new RuntimeException("Email already registered: " + customer.getEmail());
        }
        customer.setEmail(customer.getEmail().trim().toLowerCase());
        return customerRepository.save(customer);
    }

    public Customer updateCustomerProfile(Long id, Customer updatedCustomer) {
        Customer existing = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + id));

        if (updatedCustomer.getFirstName() == null || updatedCustomer.getFirstName().trim().isEmpty()) {
            throw new RuntimeException("First name is required.");
        }
        if (updatedCustomer.getLastName() == null || updatedCustomer.getLastName().trim().isEmpty()) {
            throw new RuntimeException("Last name is required.");
        }
        if (updatedCustomer.getPhone() != null && !updatedCustomer.getPhone().trim().isEmpty()) {
            if (!PHONE_PATTERN.matcher(updatedCustomer.getPhone().trim()).matches()) {
                throw new RuntimeException("Please enter a valid phone number (7-20 digits).");
            }
        }

        existing.setFirstName(updatedCustomer.getFirstName().trim());
        existing.setLastName(updatedCustomer.getLastName().trim());
        existing.setPhone(updatedCustomer.getPhone());
        existing.setAddress(updatedCustomer.getAddress());
        
        if (updatedCustomer.getEmail() != null && !updatedCustomer.getEmail().trim().isEmpty()
            && !existing.getEmail().equals(updatedCustomer.getEmail().trim().toLowerCase())) {
            if (!EMAIL_PATTERN.matcher(updatedCustomer.getEmail().trim()).matches()) {
                throw new RuntimeException("Please enter a valid email address.");
            }
            if (customerRepository.existsByEmail(updatedCustomer.getEmail().trim().toLowerCase())) {
                throw new RuntimeException("Email already registered to another account.");
            }
            existing.setEmail(updatedCustomer.getEmail().trim().toLowerCase());
        }
        if (updatedCustomer.getPassword() != null && !updatedCustomer.getPassword().isEmpty()) {
            if (updatedCustomer.getPassword().length() < 6) {
                throw new RuntimeException("Password must be at least 6 characters.");
            }
            existing.setPassword(updatedCustomer.getPassword());
        }
        if (updatedCustomer.getProfilePic() != null && !updatedCustomer.getProfilePic().isEmpty()) {
            existing.setProfilePic(updatedCustomer.getProfilePic());
        }
        return customerRepository.save(existing);
    }

    public Optional<Customer> getCustomerById(Long id) {
        return customerRepository.findById(id);
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findRecentCustomers();
    }

    public List<Customer> getRecentCustomers() {
        return customerRepository.findRecentCustomers();
    }

    public void deleteCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + id));
        // Delete related bookings first to avoid FK constraint violation
        List<Booking> bookings = bookingRepository.findByCustomerOrderByBookingDateDesc(customer);
        bookingRepository.deleteAll(bookings);
        customerRepository.deleteById(id);
    }

    public List<Booking> getCustomerBookings(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + customerId));
        return bookingRepository.findByCustomerWithPackage(customer);
    }

    public long getTotalCustomers() {
        return customerRepository.countNonAdminCustomers();
    }
}
