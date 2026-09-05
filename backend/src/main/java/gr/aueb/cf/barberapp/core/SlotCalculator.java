package gr.aueb.cf.barberapp.core;

import gr.aueb.cf.barberapp.dto.TimeSlotDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// Works out the free slots of a day. No Spring and no repositories here on purpose, so the
// arithmetic can be unit tested on its own.
public final class SlotCalculator {

    private SlotCalculator() {}

    // One block of the working day, e.g. 09:00 - 14:00
    public record WorkingBlock(LocalTime start, LocalTime end) {}

    // Time already taken: an appointment or a period of time off
    public record BusyInterval(LocalDateTime start, LocalDateTime end) {

        public boolean overlaps(LocalDateTime otherStart, LocalDateTime otherEnd) {
            return start.isBefore(otherEnd) && otherStart.isBefore(end);
        }
    }

    // Every start time on the given date where a durationMinutes service fits inside
    // a working block and collides with nothing. stepMinutes is the grid (15 -> :00, :15, :30, :45).
    public static List<TimeSlotDTO> freeSlots(LocalDate date,
                                              List<WorkingBlock> blocks,
                                              List<BusyInterval> busy,
                                              int durationMinutes,
                                              int stepMinutes,
                                              LocalDateTime now) {

        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("durationMinutes must be positive, was " + durationMinutes);
        }
        if (stepMinutes <= 0) {
            throw new IllegalArgumentException("stepMinutes must be positive, was " + stepMinutes);
        }
        if (blocks == null || blocks.isEmpty()) {
            return List.of();       // κλειστά
        }

        List<TimeSlotDTO> slots = new ArrayList<>();

        for (WorkingBlock block : blocks) {
            if (block.start() == null || block.end() == null || !block.start().isBefore(block.end())) {
                continue;           // skip a malformed block instead of blowing up
            }

            LocalDateTime blockEnd = date.atTime(block.end());
            LocalDateTime candidateStart = date.atTime(block.start());

            while (true) {
                LocalDateTime candidateEnd = candidateStart.plusMinutes(durationMinutes);
                if (candidateEnd.isAfter(blockEnd)) break;      // the service has to finish inside the block

                if (!candidateStart.isBefore(now) && isFree(candidateStart, candidateEnd, busy)) {
                    slots.add(new TimeSlotDTO(candidateStart.toLocalTime(), candidateEnd.toLocalTime()));
                }

                candidateStart = candidateStart.plusMinutes(stepMinutes);
            }
        }

        slots.sort(Comparator.comparing(TimeSlotDTO::startTime));
        return slots;
    }

    public static boolean isFree(LocalDateTime start, LocalDateTime end, List<BusyInterval> busy) {
        if (busy == null || busy.isEmpty()) return true;
        return busy.stream().noneMatch(interval -> interval.overlaps(start, end));
    }

    // True only if [start, end) sits inside ONE block. A booking is not allowed to
    // span the lunch break, so two adjacent blocks do not count.
    public static boolean isWithinWorkingHours(LocalDate date,
                                               List<WorkingBlock> blocks,
                                               LocalDateTime start,
                                               LocalDateTime end) {
        if (blocks == null || blocks.isEmpty()) return false;

        return blocks.stream().anyMatch(block -> {
            LocalDateTime blockStart = date.atTime(block.start());
            LocalDateTime blockEnd = date.atTime(block.end());
            return !start.isBefore(blockStart) && !end.isAfter(blockEnd);
        });
    }

    // Without this check somebody could POST 09:07 straight to the booking endpoint,
    // even though that time was never shown as a slot.
    public static boolean isOnSlotGrid(List<WorkingBlock> blocks, LocalTime startTime, int stepMinutes) {
        if (blocks == null || blocks.isEmpty()) return false;

        return blocks.stream().anyMatch(block -> {
            if (startTime.isBefore(block.start())) return false;
            long minutesFromBlockStart = java.time.Duration.between(block.start(), startTime).toMinutes();
            return minutesFromBlockStart >= 0 && minutesFromBlockStart % stepMinutes == 0;
        });
    }
}
