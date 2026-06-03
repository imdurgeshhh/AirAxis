package com.airline.user.mapper;

import com.airline.user.dto.UserDTO;
import com.airline.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper — generates the impl at compile time.
 * Uses componentModel="spring" so it's injectable as a @Component.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "role", expression = "java(user.getRole().name())")
    UserDTO toDTO(User user);
}
