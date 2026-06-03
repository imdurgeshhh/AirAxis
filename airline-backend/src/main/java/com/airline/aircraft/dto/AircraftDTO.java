package com.airline.aircraft.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AircraftDTO {

    private Long id;
    private String model;
    private int totalSeats;
    private int economySeats;
    private int businessSeats;
}
