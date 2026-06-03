package com.airline.aircraft.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "aircraft")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Aircraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(name = "total_seats", nullable = false)
    private int totalSeats;

    @Column(name = "economy_seats", nullable = false)
    private int economySeats;

    @Column(name = "business_seats", nullable = false)
    private int businessSeats;
}
