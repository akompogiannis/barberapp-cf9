package gr.aueb.cf.barberapp.model;

// Lifecycle of a booking. PENDING is what a customer creates; the barber moves it on from there.
public enum AppointmentStatus {

    PENDING,
    CONFIRMED,
    COMPLETED,
    CANCELLED,
    NO_SHOW;

    // Whether an appointment in this state still takes up time in the diary.
    public boolean blocksSlot() {
        return this == PENDING || this == CONFIRMED;
    }

    // Whether the barber may still move the booking to target.
    public boolean canTransitionTo(AppointmentStatus target) {
        return switch (this) {
            case PENDING   -> target == CONFIRMED || target == CANCELLED;
            case CONFIRMED -> target == COMPLETED || target == CANCELLED || target == NO_SHOW;
            // terminal states
            case COMPLETED, CANCELLED, NO_SHOW -> false;
        };
    }
}
