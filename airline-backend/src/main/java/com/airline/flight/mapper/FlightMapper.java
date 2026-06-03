package com.airline.flight.mapper;

import com.airline.flight.dto.FlightDTO;
import com.airline.flight.entity.Flight;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FlightMapper {

    @Mapping(target = "originIataCode", source = "originAirport.iataCode")
    @Mapping(target = "originCity", source = "originAirport.city")
    @Mapping(target = "originAirportName", source = "originAirport.name")
    @Mapping(target = "destIataCode", source = "destinationAirport.iataCode")
    @Mapping(target = "destCity", source = "destinationAirport.city")
    @Mapping(target = "destAirportName", source = "destinationAirport.name")
    @Mapping(target = "aircraftModel", source = "aircraft.model")
    @Mapping(target = "status", expression = "java(flight.getStatus().name())")
    @Mapping(target = "durationMinutes", expression = "java(flight.getDurationMinutes())")
    FlightDTO toDTO(Flight flight);
}
