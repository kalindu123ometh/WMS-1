package com.wms.controller;

import com.wms.entity.Booking;
import com.wms.service.BookingService;
import com.wms.service.PackageService;
import com.wms.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private ProfileService profileService;

    @Autowired
    private PackageService packageService;

    @Autowired
    private com.wms.service.AdminRegistrationService adminRegistrationService;

    @ModelAttribute
    public void addAdminAttributes(jakarta.servlet.http.HttpSession session, Model model) {
        com.wms.entity.User adminUser = (com.wms.entity.User) session.getAttribute("adminUser");
        if (adminUser != null) {
            model.addAttribute("adminUser", adminUser);
            model.addAttribute("pendingAdminRequests", adminRegistrationService.countPending());
        }
    }

    @GetMapping
    public String listBookings(@RequestParam(value = "search", required = false) String search, Model model) {
        if (search != null && !search.isEmpty()) {
            model.addAttribute("bookings", bookingService.searchBookings(search));
            model.addAttribute("search", search);
        } else {
            model.addAttribute("bookings", bookingService.getAllBookingsWithDetails());
        }
        model.addAttribute("totalBookings", bookingService.getTotalBookings());
        model.addAttribute("confirmedBookings", bookingService.getConfirmedBookings());
        return "admin/bookings";
    }

    @GetMapping("/new")
    public String showAddForm(Model model) {
        model.addAttribute("booking", new Booking());
        model.addAttribute("customers", profileService.getAllCustomers());
        model.addAttribute("packages", packageService.getAllPackages());
        model.addAttribute("formTitle", "New Booking");
        model.addAttribute("formAction", "/admin/bookings/save");
        return "admin/booking-form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        return bookingService.getBookingById(id).map(booking -> {
            model.addAttribute("booking", booking);
            model.addAttribute("customers", profileService.getAllCustomers());
            model.addAttribute("packages", packageService.getAllPackages());
            model.addAttribute("formTitle", "Edit Booking");
            model.addAttribute("formAction", "/admin/bookings/update/" + id);
            return "admin/booking-form";
        }).orElseGet(() -> {
            ra.addFlashAttribute("errorMsg", "Booking not found!");
            return "redirect:/admin/bookings";
        });
    }

    @PostMapping("/save")
    public String saveBooking(@ModelAttribute Booking booking,
                              @RequestParam(value = "customerId") Long customerId,
                              @RequestParam(value = "packageId", required = false) Long packageId,
                              RedirectAttributes ra) {
        try {
            bookingService.createBooking(booking, customerId, packageId);
            ra.addFlashAttribute("successMsg", "Booking created successfully!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/bookings";
    }

    @PostMapping("/update/{id}")
    public String updateBooking(@PathVariable Long id,
                                @ModelAttribute Booking booking,
                                @RequestParam(value = "packageId", required = false) Long packageId,
                                RedirectAttributes ra) {
        try {
            bookingService.updateBooking(id, booking, packageId);
            ra.addFlashAttribute("successMsg", "Booking updated successfully!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/bookings";
    }

    @GetMapping("/delete/{id}")
    public String deleteBooking(@PathVariable Long id, RedirectAttributes ra) {
        try {
            bookingService.deleteBooking(id);
            ra.addFlashAttribute("successMsg", "Booking deleted successfully!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/bookings";
    }

    @GetMapping("/history/{customerId}")
    public String bookingHistory(@PathVariable Long customerId, Model model) {
        model.addAttribute("bookings", bookingService.getBookingHistory(customerId));
        model.addAttribute("historyMode", true);
        return "admin/bookings";
    }

    /**
     * REST endpoint — returns JSON list of booked dates for a package.
     * Used by the booking form JS to disable already-taken dates.
     * Optional param excludeBookingId lets edit mode ignore its own date.
     * GET /admin/bookings/booked-dates?packageId=X[&excludeBookingId=Y]
     */
    @GetMapping("/booked-dates")
    @ResponseBody
    public java.util.List<String> getBookedDates(
            @RequestParam Long packageId,
            @RequestParam(required = false) Long excludeBookingId) {
        return bookingService.getBookedDatesForPackage(packageId, excludeBookingId);
    }
}
