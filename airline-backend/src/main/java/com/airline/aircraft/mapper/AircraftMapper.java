package com.airline.aircraft.mapper;

import com.airline.aircraft.dto.AircraftDTO;
import com.airline.aircraft.entity.Aircraft;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AircraftMapper {

    AircraftDTO toDTO(Aircraft aircraft);
}
