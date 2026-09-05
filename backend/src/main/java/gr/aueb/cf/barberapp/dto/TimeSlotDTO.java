package gr.aueb.cf.barberapp.dto;

import java.time.LocalTime;

public record TimeSlotDTO(LocalTime startTime, LocalTime endTime) {
}
