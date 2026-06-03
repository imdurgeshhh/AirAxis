package com.airline.airport.mapper;

import com.airline.airport.dto.AirportDTO;
import com.airline.airport.entity.Airport;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AirportMapper {

    AirportDTO toDTO(Airport airport);

    Airport toEntity(AirportDTO dto);
}
