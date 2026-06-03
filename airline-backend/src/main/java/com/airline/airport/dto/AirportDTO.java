package com.airline.airport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AirportDTO {

    private Long id;
    private String iataCode;
    private String name;
    private String city;
    private String country;
    private String timezone;
}
