package gr.aueb.cf.barberapp.core;

import gr.aueb.cf.barberapp.core.SlotCalculator.BusyInterval;
import gr.aueb.cf.barberapp.core.SlotCalculator.WorkingBlock;
import gr.aueb.cf.barberapp.dto.TimeSlotDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

// The slot arithmetic is the fiddliest logic in the application, so it is tested
// directly rather than through the service. No Spring, no database, no clock.
class SlotCalculatorTest {

    private static final LocalDate DATE = LocalDate.of(2026, 9, 15);        // a Tuesday
    // Far enough in the past that nothing on DATE is filtered as "already gone".
    private static final LocalDateTime WELL_BEFORE = DATE.minusDays(1).atTime(8, 0);

    private static List<LocalTime> startTimesOf(List<TimeSlotDTO> slots) {
        return slots.stream().map(TimeSlotDTO::startTime).toList();
    }

    @Nested
    @DisplayName("freeSlots")
    class FreeSlots {

        @Test
        @DisplayName("returns nothing when the barber does not work that day")
        void noWorkingHoursMeansNoSlots() {
            List<TimeSlotDTO> slots = SlotCalculator.freeSlots(
                    DATE, List.of(), List.of(), 30, 15, WELL_BEFORE);

            assertThat(slots).isEmpty();
        }

        @Test
        @DisplayName("returns nothing when working hours are null")
        void nullWorkingHoursMeansNoSlots() {
            List<TimeSlotDTO> slots = SlotCalculator.freeSlots(
                    DATE, null, List.of(), 30, 15, WELL_BEFORE);

            assertThat(slots).isEmpty();
        }

        @Test
        @DisplayName("steps through an empty block on the slot grid")
        void generatesSlotsOnTheGrid() {
            // 09:00-12:00 is 180 minutes. A 30 minute service on a 15 minute grid
            // can start at 09:00 through 11:30 inclusive - 11 slots.
            List<TimeSlotDTO> slots = SlotCalculator.freeSlots(
                    DATE,
                    List.of(new WorkingBlock(LocalTime.of(9, 0), LocalTime.of(12, 0))),
                    List.of(), 30, 15, WELL_BEFORE);

            assertThat(slots).hasSize(11);
            assertThat(slots.get(0).startTime()).isEqualTo(LocalTime.of(9, 0));
            assertThat(slots.get(0).endTime()).isEqualTo(LocalTime.of(9, 30));
            assertThat(slots.get(slots.size() - 1).startTime()).isEqualTo(LocalTime.of(11, 30));
            assertThat(slots.get(slots.size() - 1).endTime()).isEqualTo(LocalTime.of(12, 0));
        }

        @Test
        @DisplayName("never offers a slot the service would not finish inside")
        void serviceMustFitInsideTheBlock() {
            // Closing at 14:00, a 50 minute service cannot start after 13:10.
            List<TimeSlotDTO> slots = SlotCalculator.freeSlots(
                    DATE,
                    List.of(new WorkingBlock(LocalTime.of(13, 0), LocalTime.of(14, 0))),
                    List.of(), 50, 15, WELL_BEFORE);

            assertThat(startTimesOf(slots)).containsExactly(LocalTime.of(13, 0));
            assertThat(startTimesOf(slots)).doesNotContain(LocalTime.of(13, 15), LocalTime.of(13, 30));
        }

        @Test
        @DisplayName("treats the lunch gap as unbookable rather than joining the blocks")
        void doesNotBridgeTwoBlocks() {
            // A split day. A 30 minute service must not be offered at 13:45,
            // which would run through the closed period.
            List<TimeSlotDTO> slots = SlotCalculator.freeSlots(
                    DATE,
                    List.of(new WorkingBlock(LocalTime.of(9, 0), LocalTime.of(14, 0)),
                            new WorkingBlock(LocalTime.of(17, 0), LocalTime.of(21, 0))),
                    List.of(), 30, 15, WELL_BEFORE);

            assertThat(startTimesOf(slots))
                    .contains(LocalTime.of(13, 30), LocalTime.of(17, 0))
                    .doesNotContain(LocalTime.of(13, 45), LocalTime.of(14, 0),
                                    LocalTime.of(16, 45), LocalTime.of(20, 45));
        }

        @Test
        @DisplayName("removes every slot an existing booking overlaps")
        void busyIntervalsRemoveSlots() {
            // A 10:00-10:30 booking blocks any 30 minute slot starting at
            // 09:45, 10:00 or 10:15.
            List<TimeSlotDTO> slots = SlotCalculator.freeSlots(
                    DATE,
                    List.of(new WorkingBlock(LocalTime.of(9, 0), LocalTime.of(12, 0))),
                    List.of(new BusyInterval(DATE.atTime(10, 0), DATE.atTime(10, 30))),
                    30, 15, WELL_BEFORE);

            assertThat(startTimesOf(slots))
                    .doesNotContain(LocalTime.of(9, 45), LocalTime.of(10, 0), LocalTime.of(10, 15))
                    .contains(LocalTime.of(9, 30), LocalTime.of(10, 30));
        }

        @Test
        @DisplayName("frees the boundary minute, so a 10:30 finish leaves 10:30 bookable")
        void intervalsAreHalfOpen() {
            List<TimeSlotDTO> slots = SlotCalculator.freeSlots(
                    DATE,
                    List.of(new WorkingBlock(LocalTime.of(10, 30), LocalTime.of(11, 0))),
                    List.of(new BusyInterval(DATE.atTime(10, 0), DATE.atTime(10, 30))),
                    30, 15, WELL_BEFORE);

            assertThat(startTimesOf(slots)).containsExactly(LocalTime.of(10, 30));
        }

        @Test
        @DisplayName("hides slots that have already passed today")
        void pastSlotsAreFilteredOut() {
            LocalDateTime now = DATE.atTime(10, 20);

            List<TimeSlotDTO> slots = SlotCalculator.freeSlots(
                    DATE,
                    List.of(new WorkingBlock(LocalTime.of(9, 0), LocalTime.of(12, 0))),
                    List.of(), 30, 15, now);

            assertThat(startTimesOf(slots))
                    .doesNotContain(LocalTime.of(9, 0), LocalTime.of(10, 0), LocalTime.of(10, 15))
                    .startsWith(LocalTime.of(10, 30));
        }

        @Test
        @DisplayName("still offers a slot starting exactly now")
        void slotStartingNowIsStillOffered() {
            LocalDateTime now = DATE.atTime(10, 0);

            List<TimeSlotDTO> slots = SlotCalculator.freeSlots(
                    DATE,
                    List.of(new WorkingBlock(LocalTime.of(9, 0), LocalTime.of(12, 0))),
                    List.of(), 30, 15, now);

            assertThat(startTimesOf(slots)).startsWith(LocalTime.of(10, 0));
        }

        @Test
        @DisplayName("subtracts time off exactly like a booking")
        void timeOffBlocksSlots() {
            List<TimeSlotDTO> slots = SlotCalculator.freeSlots(
                    DATE,
                    List.of(new WorkingBlock(LocalTime.of(9, 0), LocalTime.of(12, 0))),
                    List.of(new BusyInterval(DATE.atTime(9, 0), DATE.atTime(11, 0))),
                    30, 15, WELL_BEFORE);

            assertThat(startTimesOf(slots)).containsExactly(
                    LocalTime.of(11, 0), LocalTime.of(11, 15), LocalTime.of(11, 30));
        }

        @Test
        @DisplayName("returns slots in chronological order across blocks")
        void slotsAreSorted() {
            List<TimeSlotDTO> slots = SlotCalculator.freeSlots(
                    DATE,
                    // deliberately out of order
                    List.of(new WorkingBlock(LocalTime.of(17, 0), LocalTime.of(18, 0)),
                            new WorkingBlock(LocalTime.of(9, 0), LocalTime.of(10, 0))),
                    List.of(), 30, 30, WELL_BEFORE);

            assertThat(startTimesOf(slots)).isSorted();
        }

        @Test
        @DisplayName("skips a malformed block instead of blowing up")
        void malformedBlockIsSkipped() {
            List<TimeSlotDTO> slots = SlotCalculator.freeSlots(
                    DATE,
                    List.of(new WorkingBlock(LocalTime.of(12, 0), LocalTime.of(9, 0)),   // inverted
                            new WorkingBlock(LocalTime.of(9, 0), LocalTime.of(10, 0))),
                    List.of(), 30, 30, WELL_BEFORE);

            assertThat(startTimesOf(slots)).containsExactly(LocalTime.of(9, 0), LocalTime.of(9, 30));
        }

        @Test
        @DisplayName("rejects a non-positive duration")
        void rejectsNonPositiveDuration() {
            assertThatThrownBy(() -> SlotCalculator.freeSlots(
                    DATE, List.of(new WorkingBlock(LocalTime.of(9, 0), LocalTime.of(12, 0))),
                    List.of(), 0, 15, WELL_BEFORE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("durationMinutes");
        }

        @Test
        @DisplayName("rejects a non-positive step, which would otherwise loop forever")
        void rejectsNonPositiveStep() {
            assertThatThrownBy(() -> SlotCalculator.freeSlots(
                    DATE, List.of(new WorkingBlock(LocalTime.of(9, 0), LocalTime.of(12, 0))),
                    List.of(), 30, 0, WELL_BEFORE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("stepMinutes");
        }
    }

    @Nested
    @DisplayName("isWithinWorkingHours")
    class WithinWorkingHours {

        private final List<WorkingBlock> splitDay = List.of(
                new WorkingBlock(LocalTime.of(9, 0), LocalTime.of(14, 0)),
                new WorkingBlock(LocalTime.of(17, 0), LocalTime.of(21, 0)));

        @Test
        @DisplayName("accepts an appointment wholly inside one block")
        void acceptsContainedAppointment() {
            assertThat(SlotCalculator.isWithinWorkingHours(
                    DATE, splitDay, DATE.atTime(10, 0), DATE.atTime(10, 30))).isTrue();
        }

        @Test
        @DisplayName("accepts an appointment that ends exactly at closing time")
        void acceptsAppointmentEndingAtClose() {
            assertThat(SlotCalculator.isWithinWorkingHours(
                    DATE, splitDay, DATE.atTime(13, 30), DATE.atTime(14, 0))).isTrue();
        }

        @Test
        @DisplayName("rejects an appointment that runs past closing time")
        void rejectsOverrun() {
            assertThat(SlotCalculator.isWithinWorkingHours(
                    DATE, splitDay, DATE.atTime(13, 45), DATE.atTime(14, 15))).isFalse();
        }

        @Test
        @DisplayName("rejects an appointment straddling the lunch break")
        void rejectsStraddlingTheBreak() {
            assertThat(SlotCalculator.isWithinWorkingHours(
                    DATE, splitDay, DATE.atTime(13, 0), DATE.atTime(18, 0))).isFalse();
        }

        @Test
        @DisplayName("rejects everything when the shop is closed")
        void rejectsWhenClosed() {
            assertThat(SlotCalculator.isWithinWorkingHours(
                    DATE, List.of(), DATE.atTime(10, 0), DATE.atTime(10, 30))).isFalse();
        }
    }

    @Nested
    @DisplayName("isOnSlotGrid")
    class OnSlotGrid {

        private final List<WorkingBlock> morning =
                List.of(new WorkingBlock(LocalTime.of(9, 0), LocalTime.of(12, 0)));

        @Test
        @DisplayName("accepts a time on the grid")
        void acceptsGridAlignedTime() {
            assertThat(SlotCalculator.isOnSlotGrid(morning, LocalTime.of(9, 45), 15)).isTrue();
        }

        @Test
        @DisplayName("rejects an arbitrary time that was never offered")
        void rejectsOffGridTime() {
            // Without this check a caller could POST 09:07 straight to the booking
            // endpoint even though the UI never showed it.
            assertThat(SlotCalculator.isOnSlotGrid(morning, LocalTime.of(9, 7), 15)).isFalse();
        }

        @Test
        @DisplayName("rejects a time before the shop opens")
        void rejectsTimeBeforeOpening() {
            assertThat(SlotCalculator.isOnSlotGrid(morning, LocalTime.of(8, 45), 15)).isFalse();
        }

        @Test
        @DisplayName("measures the grid from each block start, not from midnight")
        void gridIsRelativeToBlockStart() {
            List<WorkingBlock> oddStart =
                    List.of(new WorkingBlock(LocalTime.of(9, 10), LocalTime.of(12, 0)));

            assertThat(SlotCalculator.isOnSlotGrid(oddStart, LocalTime.of(9, 25), 15)).isTrue();
            assertThat(SlotCalculator.isOnSlotGrid(oddStart, LocalTime.of(9, 30), 15)).isFalse();
        }
    }

    @Nested
    @DisplayName("isFree")
    class IsFree {

        @Test
        @DisplayName("is free when nothing is booked")
        void freeWhenNoBusyIntervals() {
            assertThat(SlotCalculator.isFree(DATE.atTime(10, 0), DATE.atTime(10, 30), List.of())).isTrue();
            assertThat(SlotCalculator.isFree(DATE.atTime(10, 0), DATE.atTime(10, 30), null)).isTrue();
        }

        @Test
        @DisplayName("detects an interval fully containing the candidate")
        void detectsEnclosingInterval() {
            assertThat(SlotCalculator.isFree(DATE.atTime(10, 0), DATE.atTime(10, 30),
                    List.of(new BusyInterval(DATE.atTime(9, 0), DATE.atTime(12, 0))))).isFalse();
        }

        @Test
        @DisplayName("detects a partial overlap at either edge")
        void detectsPartialOverlap() {
            assertThat(SlotCalculator.isFree(DATE.atTime(10, 0), DATE.atTime(10, 30),
                    List.of(new BusyInterval(DATE.atTime(9, 45), DATE.atTime(10, 15))))).isFalse();

            assertThat(SlotCalculator.isFree(DATE.atTime(10, 0), DATE.atTime(10, 30),
                    List.of(new BusyInterval(DATE.atTime(10, 15), DATE.atTime(10, 45))))).isFalse();
        }

        @Test
        @DisplayName("allows back-to-back appointments")
        void allowsAdjacentIntervals() {
            assertThat(SlotCalculator.isFree(DATE.atTime(10, 30), DATE.atTime(11, 0),
                    List.of(new BusyInterval(DATE.atTime(10, 0), DATE.atTime(10, 30))))).isTrue();

            assertThat(SlotCalculator.isFree(DATE.atTime(9, 30), DATE.atTime(10, 0),
                    List.of(new BusyInterval(DATE.atTime(10, 0), DATE.atTime(10, 30))))).isTrue();
        }
    }
}
