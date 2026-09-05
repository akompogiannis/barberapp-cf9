package gr.aueb.cf.barberapp.service;

import gr.aueb.cf.barberapp.core.exceptions.EntityInvalidArgumentException;
import gr.aueb.cf.barberapp.core.exceptions.EntityNotFoundException;
import gr.aueb.cf.barberapp.core.exceptions.SlotUnavailableException;
import gr.aueb.cf.barberapp.dto.AvailabilityDTO;
import gr.aueb.cf.barberapp.dto.TimeSlotDTO;
import gr.aueb.cf.barberapp.model.*;
import gr.aueb.cf.barberapp.repository.AppointmentRepository;
import gr.aueb.cf.barberapp.repository.BarberServiceRepository;
import gr.aueb.cf.barberapp.repository.TimeOffRepository;
import gr.aueb.cf.barberapp.repository.WorkingHoursRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

// Exercises the service around the calculator: the guard rails, and the wiring between the
// repositories and the slot maths.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AvailabilityServiceTest {

    // Tuesday 15 September 2026, 08:00 - before the shop opens at 09:00.
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 15);
    private static final Clock FIXED_CLOCK = Clock.fixed(
            TODAY.atTime(8, 0).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault());

    private static final UUID SERVICE_UUID = UUID.randomUUID();
    private static final int MAX_DAYS_AHEAD = 60;

    @Mock private BarberServiceRepository barberServiceRepository;
    @Mock private WorkingHoursRepository workingHoursRepository;
    @Mock private TimeOffRepository timeOffRepository;
    @Mock private AppointmentRepository appointmentRepository;

    private AvailabilityService availabilityService;

    private Barber barber;
    private BarberService haircut;

    @BeforeEach
    void setUp() {
        barber = new Barber();
        barber.setId(1L);
        barber.setUuid(UUID.randomUUID());
        barber.setShopName("Test Studio");
        barber.setSlotStepMinutes(15);

        haircut = new BarberService();
        haircut.setId(10L);
        haircut.setUuid(SERVICE_UUID);
        haircut.setName("Haircut");
        haircut.setDurationMinutes(30);
        haircut.setPrice(new BigDecimal("15.00"));
        haircut.setActive(true);
        haircut.setBarber(barber);

        availabilityService = new AvailabilityService(
                barberServiceRepository, workingHoursRepository,
                timeOffRepository, appointmentRepository, FIXED_CLOCK);
        ReflectionTestUtils.setField(availabilityService, "maxDaysAhead", MAX_DAYS_AHEAD);

        when(barberServiceRepository.findByUuidAndDeletedFalse(SERVICE_UUID))
                .thenReturn(Optional.of(haircut));
        when(timeOffRepository.findOverlapping(anyLong(), any(), any())).thenReturn(List.of());
        when(appointmentRepository.findBlockingBetween(any(), any(), any())).thenReturn(List.of());
        when(appointmentRepository.existsBlockingOverlap(any(), any(), any(), any())).thenReturn(false);
    }

    // Tuesday 09:00-14:00 and 17:00-21:00.
    private void givenSplitWorkingDay() {
        when(workingHoursRepository.findAllByBarber_IdAndDayOfWeekAndDeletedFalseOrderByStartTimeAsc(
                eq(1L), eq(DayOfWeek.TUESDAY)))
                .thenReturn(List.of(
                        workingHours(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(14, 0)),
                        workingHours(DayOfWeek.TUESDAY, LocalTime.of(17, 0), LocalTime.of(21, 0))));
    }

    private void givenClosed(DayOfWeek day) {
        when(workingHoursRepository.findAllByBarber_IdAndDayOfWeekAndDeletedFalseOrderByStartTimeAsc(
                eq(1L), eq(day)))
                .thenReturn(List.of());
    }

    // ---------- getAvailability ----------

    @Test
    @DisplayName("rejects a date in the past")
    void rejectsPastDate() {
        assertThatThrownBy(() -> availabilityService.getAvailability(SERVICE_UUID, TODAY.minusDays(1)))
                .isInstanceOf(EntityInvalidArgumentException.class)
                .hasMessageContaining("past");
    }

    @Test
    @DisplayName("rejects a date beyond the booking horizon")
    void rejectsDateTooFarAhead() {
        assertThatThrownBy(() -> availabilityService.getAvailability(
                SERVICE_UUID, TODAY.plusDays(MAX_DAYS_AHEAD + 1)))
                .isInstanceOf(EntityInvalidArgumentException.class)
                .hasMessageContaining("days ahead");
    }

    @Test
    @DisplayName("rejects an unknown service")
    void rejectsUnknownService() {
        UUID unknown = UUID.randomUUID();
        when(barberServiceRepository.findByUuidAndDeletedFalse(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> availabilityService.getAvailability(unknown, TODAY))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("rejects a service that is no longer offered")
    void rejectsInactiveService() {
        haircut.setActive(false);

        assertThatThrownBy(() -> availabilityService.getAvailability(SERVICE_UUID, TODAY))
                .isInstanceOf(EntityInvalidArgumentException.class)
                .hasMessageContaining("not currently offered");
    }

    @Test
    @DisplayName("reports closed rather than an empty list when the shop does not work that day")
    void reportsClosedDay() throws Exception {
        LocalDate sunday = LocalDate.of(2026, 9, 20);
        givenClosed(DayOfWeek.SUNDAY);

        AvailabilityDTO result = availabilityService.getAvailability(SERVICE_UUID, sunday);

        // The front-end shows "closed on Sundays", not "fully booked".
        assertThat(result.closed()).isTrue();
        assertThat(result.slots()).isEmpty();
    }

    @Test
    @DisplayName("returns open with slots on a working day")
    void returnsSlotsOnWorkingDay() throws Exception {
        givenSplitWorkingDay();

        AvailabilityDTO result = availabilityService.getAvailability(SERVICE_UUID, TODAY);

        assertThat(result.closed()).isFalse();
        assertThat(result.serviceName()).isEqualTo("Haircut");
        assertThat(result.durationMinutes()).isEqualTo(30);
        assertThat(result.slots()).isNotEmpty();
        assertThat(result.slots()).extracting(TimeSlotDTO::startTime)
                .contains(LocalTime.of(9, 0))
                .doesNotContain(LocalTime.of(14, 0), LocalTime.of(16, 45));
    }

    @Test
    @DisplayName("removes slots taken by an existing booking")
    void excludesBookedSlots() throws Exception {
        givenSplitWorkingDay();
        when(appointmentRepository.findBlockingBetween(any(), any(), any()))
                .thenReturn(List.of(appointment(TODAY.atTime(10, 0), TODAY.atTime(10, 30))));

        AvailabilityDTO result = availabilityService.getAvailability(SERVICE_UUID, TODAY);

        assertThat(result.slots()).extracting(TimeSlotDTO::startTime)
                .doesNotContain(LocalTime.of(9, 45), LocalTime.of(10, 0), LocalTime.of(10, 15))
                .contains(LocalTime.of(10, 30));
    }

    @Test
    @DisplayName("removes slots covered by time off")
    void excludesTimeOff() throws Exception {
        givenSplitWorkingDay();
        when(timeOffRepository.findOverlapping(anyLong(), any(), any()))
                .thenReturn(List.of(timeOff(TODAY.atTime(9, 0), TODAY.atTime(12, 0))));

        AvailabilityDTO result = availabilityService.getAvailability(SERVICE_UUID, TODAY);

        assertThat(result.slots()).extracting(TimeSlotDTO::startTime)
                .doesNotContain(LocalTime.of(9, 0), LocalTime.of(11, 30))
                .contains(LocalTime.of(12, 0));
    }

    // ---------- assertBookable ----------

    @Test
    @DisplayName("refuses to book a time that has already passed")
    void refusesPastBooking() {
        givenSplitWorkingDay();

        assertThatThrownBy(() -> availabilityService.assertBookable(
                haircut, TODAY.atTime(7, 0), null))
                .isInstanceOf(SlotUnavailableException.class)
                .hasMessageContaining("past");
    }

    @Test
    @DisplayName("refuses to book beyond the booking horizon")
    void refusesBookingTooFarAhead() {
        assertThatThrownBy(() -> availabilityService.assertBookable(
                haircut, TODAY.plusDays(MAX_DAYS_AHEAD + 1).atTime(10, 0), null))
                .isInstanceOf(SlotUnavailableException.class)
                .hasMessageContaining("days ahead");
    }

    @Test
    @DisplayName("refuses to book on a day the shop is closed")
    void refusesBookingOnClosedDay() {
        LocalDate sunday = LocalDate.of(2026, 9, 20);
        givenClosed(DayOfWeek.SUNDAY);

        assertThatThrownBy(() -> availabilityService.assertBookable(
                haircut, sunday.atTime(10, 0), null))
                .isInstanceOf(SlotUnavailableException.class)
                .hasMessageContaining("closed");
    }

    @Test
    @DisplayName("refuses a start time that was never offered as a slot")
    void refusesOffGridStartTime() {
        givenSplitWorkingDay();

        // 09:07 is not on the 15 minute grid, so the UI never showed it.
        assertThatThrownBy(() -> availabilityService.assertBookable(
                haircut, TODAY.atTime(9, 7), null))
                .isInstanceOf(SlotUnavailableException.class)
                .hasMessageContaining("not an offered time");
    }

    @Test
    @DisplayName("refuses a booking that would run past closing time")
    void refusesBookingThatOverrunsClosing() {
        givenSplitWorkingDay();
        haircut.setDurationMinutes(50);

        // 13:30 + 50 minutes = 14:20, past the 14:00 close.
        assertThatThrownBy(() -> availabilityService.assertBookable(
                haircut, TODAY.atTime(13, 30), null))
                .isInstanceOf(SlotUnavailableException.class)
                .hasMessageContaining("does not fit");
    }

    @Test
    @DisplayName("refuses a booking during time off")
    void refusesBookingDuringTimeOff() {
        givenSplitWorkingDay();
        when(timeOffRepository.findOverlapping(anyLong(), any(), any()))
                .thenReturn(List.of(timeOff(TODAY.atTime(10, 0), TODAY.atTime(11, 0))));

        assertThatThrownBy(() -> availabilityService.assertBookable(
                haircut, TODAY.atTime(10, 0), null))
                .isInstanceOf(SlotUnavailableException.class)
                .hasMessageContaining("unavailable");
    }

    @Test
    @DisplayName("refuses a booking that lost the race for the slot")
    void refusesWhenSlotJustTaken() {
        givenSplitWorkingDay();
        // Simulates the other half of a double-booking race: availability said the
        // slot was free, but by commit time another transaction had inserted it.
        when(appointmentRepository.existsBlockingOverlap(any(), any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> availabilityService.assertBookable(
                haircut, TODAY.atTime(10, 0), null))
                .isInstanceOf(SlotUnavailableException.class)
                .hasMessageContaining("just been taken");
    }

    @Test
    @DisplayName("accepts a legitimate slot")
    void acceptsValidSlot() {
        givenSplitWorkingDay();

        assertThatCode(() -> availabilityService.assertBookable(haircut, TODAY.atTime(10, 0), null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("lets a reschedule keep its own slot by excluding itself from the overlap check")
    void rescheduleExcludesItself() {
        givenSplitWorkingDay();
        UUID self = UUID.randomUUID();

        // The repository is asked to ignore this appointment; with it excluded
        // there is no conflict, so the move is allowed.
        when(appointmentRepository.existsBlockingOverlap(any(), any(), any(), eq(self)))
                .thenReturn(false);

        assertThatCode(() -> availabilityService.assertBookable(haircut, TODAY.atTime(10, 0), self))
                .doesNotThrowAnyException();
    }

    // ---------- fixtures ----------

    private WorkingHours workingHours(DayOfWeek day, LocalTime start, LocalTime end) {
        WorkingHours hours = new WorkingHours();
        hours.setBarber(barber);
        hours.setDayOfWeek(day);
        hours.setStartTime(start);
        hours.setEndTime(end);
        return hours;
    }

    private Appointment appointment(LocalDateTime start, LocalDateTime end) {
        Appointment appointment = new Appointment();
        appointment.setUuid(UUID.randomUUID());
        appointment.setStartAt(start);
        appointment.setEndAt(end);
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        return appointment;
    }

    private TimeOff timeOff(LocalDateTime start, LocalDateTime end) {
        TimeOff off = new TimeOff();
        off.setUuid(UUID.randomUUID());
        off.setBarber(barber);
        off.setStartAt(start);
        off.setEndAt(end);
        return off;
    }
}
