package com.wms.service;

import com.wms.entity.Booking;
import com.wms.entity.Customer;
import com.wms.entity.WeddingPackage;
import com.wms.repository.BookingRepository;
import com.wms.repository.CustomerRepository;
import com.wms.repository.PackageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PackageRepository packageRepository;

    @Autowired
    private ReportService reportService;

    public List<Booking> getAllBookings() {
        return bookingRepository.findAllByOrderByBookingDateDesc();
    }

    /**
     * Returns all bookings with customer and weddingPackage eagerly loaded.
     * Safe to use in Thymeleaf templates (avoids LazyInitializationException).
     */
    public List<Booking> getAllBookingsWithDetails() {
        return bookingRepository.findAllWithDetails();
    }

    public Optional<Booking> getBookingById(Long id) {
        return bookingRepository.findById(id);
    }

    /** Eager-loads customer + weddingPackage — use for REST/report endpoints. */
    public Optional<Booking> getBookingByIdWithDetails(Long id) {
        return bookingRepository.findByIdWithDetails(id);
    }

    public Booking createBooking(Booking booking, Long customerId, Long packageId) {
        // Validate event date
        if (booking.getEventDate() == null) {
            throw new RuntimeException("Event date is required.");
        }
        if (booking.getEventDate().isBefore(java.time.LocalDate.now())) {
            throw new RuntimeException("Event date cannot be in the past.");
        }
        // Validate total amount
        if (booking.getTotalAmount() != null && booking.getTotalAmount().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Total amount cannot be negative.");
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + customerId));
        booking.setCustomer(customer);

        if (packageId != null) {
            WeddingPackage pkg = packageRepository.findById(packageId)
                    .orElseThrow(() -> new RuntimeException("Package not found: " + packageId));
            booking.setWeddingPackage(pkg);
            if (booking.getTotalAmount() == null) {
                booking.setTotalAmount(pkg.getPrice());
            }
        }
        return bookingRepository.save(booking);
    }

    public Booking updateBooking(Long id, Booking updatedBooking, Long packageId) {
        // Validate event date
        if (updatedBooking.getEventDate() == null) {
            throw new RuntimeException("Event date is required.");
        }
        if (updatedBooking.getEventDate().isBefore(java.time.LocalDate.now())) {
            throw new RuntimeException("Event date cannot be in the past.");
        }
        if (updatedBooking.getTotalAmount() != null && updatedBooking.getTotalAmount().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Total amount cannot be negative.");
        }

        Booking existing = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + id));
        existing.setEventDate(updatedBooking.getEventDate());
        existing.setVenue(updatedBooking.getVenue());
        existing.setGuestCount(updatedBooking.getGuestCount());
        existing.setTotalAmount(updatedBooking.getTotalAmount());
        existing.setStatus(updatedBooking.getStatus());
        existing.setNotes(updatedBooking.getNotes());
        existing.setPaymentMethod(updatedBooking.getPaymentMethod());
        existing.setPaymentStatus(updatedBooking.getPaymentStatus());
        existing.setTransactionId(updatedBooking.getTransactionId());

        if (packageId != null) {
            WeddingPackage pkg = packageRepository.findById(packageId)
                    .orElseThrow(() -> new RuntimeException("Package not found: " + packageId));
            existing.setWeddingPackage(pkg);
        } else {
            existing.setWeddingPackage(null);
        }

        Booking saved = bookingRepository.save(existing);

        // Auto-generate report on Admin Update
        try {
            com.wms.entity.Report adminReport = new com.wms.entity.Report();
            String customerName = saved.getCustomer() != null ? saved.getCustomer().getFullName() : "N/A";
            String packageName = saved.getWeddingPackage() != null ? saved.getWeddingPackage().getName() : "Custom";
            
            adminReport.setTitle("Status Update Report – " + packageName + " (" + customerName + ")");
            adminReport.setType("Booking");
            adminReport.setGeneratedBy("Admin (System Update)");
            adminReport.setContent(
                "--------------------------------------------------\n" +
                "           OFFICIAL STATUS UPDATE REPORT          \n" +
                "--------------------------------------------------\n" +
                "BOOKING STATUS  : " + saved.getStatus() + " 📋\n" +
                "PAYMENT STATUS  : " + ("Paid".equals(saved.getPaymentStatus()) ? "✅ PAYMENT SUCCESSFUL" : "❌ PENDING / UNPAID") + "\n" +
                "Transaction ID  : " + (saved.getTransactionId() != null ? saved.getTransactionId() : "N/A") + "\n" +
                "Payment Method  : " + (saved.getPaymentMethod() != null ? saved.getPaymentMethod() : "N/A") + "\n" +
                "--------------------------------------------------\n\n" +
                
                "CUSTOMER DETAILS\n" +
                "Name            : " + customerName + "\n" +
                "Email           : " + (saved.getCustomer() != null ? saved.getCustomer().getEmail() : "N/A") + "\n\n" +
                
                "PACKAGE DETAILS\n" +
                "Package Name    : " + packageName + "\n" +
                "Total Amount    : LKR " + (saved.getTotalAmount() != null ? saved.getTotalAmount() : "0.00") + "\n\n" +
                
                "EVENT INFORMATION\n" +
                "Event Date      : " + saved.getEventDate() + "\n" +
                "Venue           : " + saved.getVenue() + "\n\n" +
                
                "FINAL SUMMARY\n" +
                "Updated On      : " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")) + "\n" +
                "--------------------------------------------------\n" +
                "CONFIRMED BY    : Admin Dashboard Action"
            );
            reportService.addReport(adminReport);
        } catch (Exception e) {
            System.err.println("Admin report generation failed: " + e.getMessage());
        }

        return saved;
    }

    public void deleteBooking(Long id) {
        if (!bookingRepository.existsById(id)) {
            throw new RuntimeException("Booking not found: " + id);
        }
        bookingRepository.deleteById(id);
    }

    /**
     * Deletes a booking ONLY if it belongs to the given customer.
     * Throws an exception if the booking doesn't exist or doesn't belong to them.
     */
    public void deleteCustomerBooking(Long bookingId, Long customerId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));
        if (booking.getCustomer() == null || !booking.getCustomer().getId().equals(customerId)) {
            throw new RuntimeException("You are not authorised to delete this booking.");
        }
        bookingRepository.deleteById(bookingId);
    }

    public List<Booking> searchBookings(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return bookingRepository.findAllWithDetails();
        }
        return bookingRepository.searchBookings(keyword.trim());
    }

    public List<Booking> getBookingHistory(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + customerId));
        return bookingRepository.findByCustomerWithPackage(customer);
    }

    public long getTotalBookings() {
        return bookingRepository.count();
    }

    public long getConfirmedBookings() {
        return bookingRepository.findByStatus("Confirmed").size();
    }

    @Autowired
    private com.wms.repository.PaymentRepository paymentRepository;

    @jakarta.transaction.Transactional
    public void recordKokoPayment(Long bookingId, int installmentNum) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found."));

        // Use null-safe getter
        int currentPaid = (booking.getInstallmentsPaid() != null) ? booking.getInstallmentsPaid() : 0;

        // Progressive check: cannot pay installment 2 if 1 isn't paid, etc.
        if (installmentNum != (currentPaid + 1)) {
            throw new RuntimeException("Installments must be paid in order. Current step: " + (currentPaid + 1));
        }

        // Apply 10% Installment Fee on the first installment
        if (installmentNum == 1 && (booking.getInstallmentsPaid() == null || booking.getInstallmentsPaid() == 0)) {
            java.math.BigDecimal fee = booking.getTotalAmount().multiply(new java.math.BigDecimal("0.10"));
            booking.setTotalAmount(booking.getTotalAmount().add(fee));
        }

        // Calculate amounts safely to avoid rounding issues
        java.math.BigDecimal totalAmount = booking.getTotalAmount();
        java.math.BigDecimal standardInstallment = totalAmount
                .divide(new java.math.BigDecimal("3"), 2, java.math.RoundingMode.HALF_UP);
        
        java.math.BigDecimal amountToPay;
        if (installmentNum == 3) {
            // Final payment is the remainder of the total
            java.math.BigDecimal paidSoFar = standardInstallment.multiply(new java.math.BigDecimal("2"));
            amountToPay = totalAmount.subtract(paidSoFar);
            
            booking.setPaymentStatus("Paid");
            booking.setStatus("Confirmed"); // Auto-confirm on full payment
        } else {
            amountToPay = standardInstallment;
            booking.setPaymentStatus("Partial");
        }

        // Increment progress
        booking.setInstallmentsPaid(installmentNum);

        // Create Payment record for history
        com.wms.entity.Payment payment = new com.wms.entity.Payment(
            booking, 
            amountToPay, 
            "Koko", 
            "KOKO-FIN-" + installmentNum + "-" + java.util.UUID.randomUUID().toString().substring(0, 4).toUpperCase()
        );
        
        paymentRepository.save(payment);
        bookingRepository.save(booking);
    }

    /**
     * Returns ISO date strings (yyyy-MM-dd) of all dates already booked
     * for the given package (excluding Cancelled bookings).
     * Optional excludeBookingId skips that booking's date (edit mode).
     */
    public List<String> getBookedDatesForPackage(Long packageId, Long excludeBookingId) {
        List<java.time.LocalDate> dates;
        if (excludeBookingId != null) {
            dates = bookingRepository.findBookedDatesByPackageIdExcluding(packageId, excludeBookingId);
        } else {
            dates = bookingRepository.findBookedDatesByPackageId(packageId);
        }
        return dates.stream()
                .map(java.time.LocalDate::toString)
                .collect(java.util.stream.Collectors.toList());
    }
}
