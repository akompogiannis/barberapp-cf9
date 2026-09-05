package gr.aueb.cf.barberapp.dto;

import java.util.Map;

// Field name -> message, so the front-end can mark up the offending inputs.
public record ValidationErrorResponseDTO(String code, String description, Map<String, String> errors) {
}
