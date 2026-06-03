package com.airline.aircraft.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAircraftRequest {

    @NotBlank(message = "Aircraft model is required")
    private String model;

    @Min(value = 1, message = "Economy seats must be at least 1")
    private int economySeats;

    @Min(value = 0, message = "Business seats cannot be negative")
    private int businessSeats;
}
