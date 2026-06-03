package com.airline.admin.controller;

import com.airline.aircraft.dto.AircraftDTO;
import com.airline.aircraft.dto.CreateAircraftRequest;
import com.airline.aircraft.service.AircraftService;
import com.airline.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/aircraft")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAircraftController {

    private final AircraftService aircraftService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AircraftDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(aircraftService.getAllAircraft()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AircraftDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(aircraftService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AircraftDTO>> create(
            @Valid @RequestBody CreateAircraftRequest request) {
        AircraftDTO created = aircraftService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Aircraft created"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AircraftDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateAircraftRequest request) {
        return ResponseEntity.ok(ApiResponse.success(aircraftService.update(id, request), "Aircraft updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        aircraftService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Aircraft deleted"));
    }
}
