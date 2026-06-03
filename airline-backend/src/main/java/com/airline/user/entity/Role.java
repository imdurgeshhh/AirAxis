package com.airline.user.entity;

/**
 * User roles in the airline system.
 * Spring Security's hasRole("ADMIN") automatically prefixes with ROLE_,
 * so we store just PASSENGER / ADMIN and use SimpleGrantedAuthority("ROLE_" + name).
 */
public enum Role {
    PASSENGER,
    ADMIN
}
