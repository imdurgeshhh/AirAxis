package com.airline.admin.controller;

import com.airline.admin.dto.DashboardDTO;
import com.airline.admin.service.DashboardService;
import com.airline.booking.dto.BookingDTO;
import com.airline.common.ApiResponse;
import com.airline.exception.ResourceNotFoundException;
import com.airline.user.entity.Role;
import com.airline.user.entity.User;
import com.airline.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final DashboardService dashboardService;
    private final UserRepository userRepository;

    /**
     * GET /api/admin/dashboard
     * Full dashboard stats: users, flights, bookings, revenue, status breakdown.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardDTO>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getDashboard()));
    }

    /**
     * GET /api/admin/bookings?page=0&size=20
     * All bookings, paginated.
     */
    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<List<BookingDTO>>> getAllBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getAllBookings(page, size)));
    }

    /**
     * PUT /api/admin/users/{id}/role?role=ADMIN
     * Promote or change a user's role.
     */
    @PutMapping("/users/{id}/role")
    public ResponseEntity<ApiResponse<String>> updateUserRole(
            @PathVariable Long id,
            @RequestParam String role) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        try {
            Role newRole = Role.valueOf(role.toUpperCase());
            user.setRole(newRole);
            userRepository.save(user);
            log.info("User role updated: userId={}, newRole={}", id, newRole);
            return ResponseEntity.ok(ApiResponse.success(
                    "User " + user.getEmail() + " role updated to " + newRole));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + role + ". Valid roles: PASSENGER, ADMIN");
        }
    }
}
