package gr.aueb.cf.barberapp.core.exceptions;

import lombok.Getter;

// Root of the application's checked exceptions.
@Getter
public class AppGenericException extends Exception {

    private final String code;

    public AppGenericException(String code, String message) {
        super(message);
        this.code = code;
    }
}
