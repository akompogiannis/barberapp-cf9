package gr.aueb.cf.barberapp.core;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// OpenAPI metadata. The @SecurityScheme is what puts the "Authorize" button in
// Swagger UI, where a JWT obtained from /api/v1/auth/authenticate can be pasted.
@Configuration
@SecurityScheme(
        name = "Bearer Authentication",     // must match the name used by @SecurityRequirement
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Barber Appointment API")
                        .version("1.0.0")
                        .description("""
                                REST API for a freelance barber appointment book.

                                Public endpoints expose the shop profile, the service catalogue,
                                live promotions and free slots. Booking and administration require
                                a JWT: obtain one from POST /api/v1/auth/authenticate and paste it
                                into the Authorize dialog above.

                                Demo accounts (dev profile):
                                  barber / barber12345      - ADMIN
                                  customer / customer12345  - CUSTOMER
                                """)
                        .contact(new Contact()
                                .name("Coding Factory @ AUEB")
                                .email("codingfactory@aueb.gr")
                                .url("https://codingfactory.aueb.gr"))
                        .license(new License()
                                .name("CC0 1.0 Universal")
                                .url("https://creativecommons.org/publicdomain/zero/1.0")));
    }

    // Adds 401/403 to every secured operation so those responses do not have to
    // be repeated as @ApiResponse on each handler.
    @Bean
    public OperationCustomizer globalSecurityResponses() {
        return (operation, handlerMethod) -> {
            boolean isSecured = handlerMethod.hasMethodAnnotation(SecurityRequirement.class)
                    || handlerMethod.getBeanType().isAnnotationPresent(SecurityRequirement.class);

            if (isSecured) {
                operation.getResponses()
                        .addApiResponse("401", new ApiResponse().description("Unauthorized - JWT is missing or invalid"))
                        .addApiResponse("403", new ApiResponse().description("Forbidden - the account lacks the required capability"));
            }
            return operation;
        };
    }
}
