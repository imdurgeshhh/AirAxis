package com.airline.checkin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckinResponseDTO {

    private Long passengerId;
    private String firstName;
    private String lastName;
    private String seatNumber;
    private String seatClass;
    private String boardingPassNumber;
    private LocalDateTime checkedInAt;
}
