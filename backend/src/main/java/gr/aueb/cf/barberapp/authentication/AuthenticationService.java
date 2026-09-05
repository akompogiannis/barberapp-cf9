package gr.aueb.cf.barberapp.authentication;

import gr.aueb.cf.barberapp.dto.AuthenticationRequestDTO;
import gr.aueb.cf.barberapp.dto.AuthenticationResponseDTO;
import gr.aueb.cf.barberapp.mapper.Mapper;
import gr.aueb.cf.barberapp.model.User;
import gr.aueb.cf.barberapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final Mapper mapper;

    // Verifies the credentials and mints a token.
    @Transactional(readOnly = true)
    public AuthenticationResponseDTO authenticate(AuthenticationRequestDTO dto) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.username(), dto.password()));

        User user = userRepository.findByUsername(dto.username())
                .orElseThrow(() -> new BadCredentialsException("Authentication failed"));

        String token = jwtService.generateToken(user.getUsername(), user.getRole().getName());
        log.info("User with username={} authenticated successfully", user.getUsername());

        return new AuthenticationResponseDTO(
                token,
                jwtService.getJwtExpiration(),
                mapper.mapToUserReadOnlyDTO(user)
        );
    }
}
