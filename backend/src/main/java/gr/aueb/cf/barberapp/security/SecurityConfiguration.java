package gr.aueb.cf.barberapp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

// URL-level authorization.
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Value("${allowed.origins}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationProvider authenticationProvider)
            throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Safe to disable: the API is stateless and carries no cookies, so
                // there is no ambient authority for a CSRF attack to ride on.
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(req -> req
                        // --- public: the shop window ---
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/authenticate").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/users/register").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/barber/profile").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/services").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/services/{uuid}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/promotions/active").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/availability").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()

                        // --- catalogue administration ---
                        .requestMatchers(HttpMethod.POST, "/api/v1/services").hasAuthority("MANAGE_SERVICES")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/services/{uuid}").hasAuthority("MANAGE_SERVICES")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/services/{uuid}").hasAuthority("MANAGE_SERVICES")

                        // --- schedule administration ---
                        .requestMatchers("/api/v1/schedule/**").hasAuthority("MANAGE_SCHEDULE")

                        // --- promotions administration ---
                        .requestMatchers(HttpMethod.POST, "/api/v1/promotions").hasAuthority("MANAGE_PROMOTIONS")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/promotions/{uuid}").hasAuthority("MANAGE_PROMOTIONS")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/promotions/{uuid}").hasAuthority("MANAGE_PROMOTIONS")
                        .requestMatchers(HttpMethod.GET, "/api/v1/promotions").hasAuthority("MANAGE_PROMOTIONS")

                        // --- appointments ---
                        // /me must be matched before /{uuid}, otherwise "me" is read as a uuid
                        .requestMatchers(HttpMethod.GET, "/api/v1/appointments/me").hasAuthority("VIEW_OWN_APPOINTMENTS")
                        .requestMatchers(HttpMethod.POST, "/api/v1/appointments").hasAuthority("BOOK_APPOINTMENT")
                        .requestMatchers(HttpMethod.GET, "/api/v1/appointments").hasAuthority("VIEW_APPOINTMENTS")
                        // the fine-grained own-vs-any decision lives on the service method
                        .requestMatchers(HttpMethod.GET, "/api/v1/appointments/{uuid}").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/appointments/{uuid}/status").hasAuthority("EDIT_APPOINTMENT")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/appointments/{uuid}/reschedule").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/appointments/{uuid}").authenticated()

                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(customAuthenticationEntryPoint())
                        .accessDeniedHandler(customAccessDeniedHandler()));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService,
                                                         PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder);
        return authenticationProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder(@Value("${security.bcrypt.strength}") int strength) {
        return new BCryptPasswordEncoder(strength);
    }

    @Bean
    public AuthenticationEntryPoint customAuthenticationEntryPoint() {
        return new CustomAuthenticationEntryPoint(objectMapper);
    }

    @Bean
    public AccessDeniedHandler customAccessDeniedHandler() {
        return new CustomAccessDeniedHandler(objectMapper);
    }
}
