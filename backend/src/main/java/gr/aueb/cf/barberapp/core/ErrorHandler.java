package gr.aueb.cf.barberapp.core;

import gr.aueb.cf.barberapp.core.exceptions.*;
import gr.aueb.cf.barberapp.dto.ErrorResponseDTO;
import gr.aueb.cf.barberapp.dto.ValidationErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

// Maps the checked exceptions onto HTTP status codes in one place, so controllers
// can simply declare throws and stay free of try/catch.
@ControllerAdvice
@Slf4j
public class ErrorHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ValidationErrorResponseDTO> handleValidationException(ValidationException e) {
        log.warn("Validation failed. Message={}", e.getMessage());

        BindingResult bindingResult = e.getBindingResult();
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return new ResponseEntity<>(
                new ValidationErrorResponseDTO(e.getCode(), e.getMessage(), errors),
                HttpStatus.BAD_REQUEST);                    // 400
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleEntityNotFoundException(EntityNotFoundException e) {
        log.warn("Entity not found. Message={}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)               // 404
                .body(new ErrorResponseDTO(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(EntityInvalidArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidArgumentException(EntityInvalidArgumentException e) {
        log.warn("Invalid argument. Message={}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)             // 400
                .body(new ErrorResponseDTO(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(EntityAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDTO> handleEntityAlreadyExistsException(EntityAlreadyExistsException e) {
        log.warn("Entity already exists. Message={}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)                // 409
                .body(new ErrorResponseDTO(e.getCode(), e.getMessage()));
    }

    // 409 as well: like a duplicate, the request lost a race, and the sensible
    // next move for the client is to refresh the slots and pick another one.
    @ExceptionHandler(SlotUnavailableException.class)
    public ResponseEntity<ErrorResponseDTO> handleSlotUnavailableException(SlotUnavailableException e) {
        log.warn("Slot unavailable. Message={}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)                // 409
                .body(new ErrorResponseDTO(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDTO> handleAuthenticationException(AuthenticationException e,
                                                                          HttpServletRequest request) {
        // the username is deliberately not logged - failed logins are a credential-stuffing signal
        log.warn("Failed login attempt from IP={}", request.getRemoteAddr());

        String errorCode = switch (e) {
            case BadCredentialsException ex      -> "INVALID_CREDENTIALS";
            case DisabledException ex            -> "ACCOUNT_DISABLED";
            case LockedException ex              -> "ACCOUNT_LOCKED";
            case AccountExpiredException ex      -> "ACCOUNT_EXPIRED";
            case CredentialsExpiredException ex  -> "CREDENTIALS_EXPIRED";
            default                              -> "AUTHENTICATION_ERROR";
        };

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)            // 401
                .body(new ErrorResponseDTO(errorCode, "Authentication failed"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("Access denied. Message={}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)               // 403
                .body(new ErrorResponseDTO("ACCESS_DENIED", "You do not have permission to perform this action"));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponseDTO> handleDatabaseException(DataAccessException e) {
        log.error("Database error.", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)   // 500
                .body(new ErrorResponseDTO("DATABASE_ERROR", "A database error occurred."));
    }

    // Fallback. The real cause is logged; the client is told nothing revealing.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(Exception e) {
        log.error("Unexpected error.", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)   // 500
                .body(new ErrorResponseDTO("INTERNAL_SERVER_ERROR", "An unexpected error occurred."));
    }
}
