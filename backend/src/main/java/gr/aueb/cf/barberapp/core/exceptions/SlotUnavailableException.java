package gr.aueb.cf.barberapp.core.exceptions;

// The requested time cannot be booked: it falls outside working hours, collides with time off, is
// in the past, or someone else took it first.
public class SlotUnavailableException extends AppGenericException {

    private static final String DEFAULT_CODE = "SlotUnavailable";

    public SlotUnavailableException(String code, String message) {
        super(code + DEFAULT_CODE, message);
    }
}
