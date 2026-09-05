package gr.aueb.cf.barberapp.core.exceptions;

import lombok.Getter;
import org.springframework.validation.BindingResult;

// Carries the whole BindingResult so the error handler can report every invalid
// field at once rather than failing on the first one.
@Getter
public class ValidationException extends AppGenericException {

    private static final String DEFAULT_CODE = "ValidationError";

    private final BindingResult bindingResult;

    public ValidationException(String code, String message, BindingResult bindingResult) {
        super(code + DEFAULT_CODE, message);
        this.bindingResult = bindingResult;
    }
}
