package gr.aueb.cf.barberapp.validator;

import gr.aueb.cf.barberapp.dto.UserInsertDTO;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

// Cross-field rules that bean validation annotations cannot express.
@Component
public class UserInsertValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return UserInsertDTO.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        UserInsertDTO dto = (UserInsertDTO) target;

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "username", "empty", "Username is required");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "password", "empty", "Password is required");

        if (dto.password() != null && !dto.password().equals(dto.confirmPassword())) {
            errors.rejectValue("confirmPassword", "mismatch", "The passwords do not match");
        }

        // A username that looks like an email address is confusing at the login
        // form, where both a username and an email field would accept it.
        if (dto.username() != null && dto.username().contains("@")) {
            errors.rejectValue("username", "invalid", "Username cannot contain @");
        }
    }
}
