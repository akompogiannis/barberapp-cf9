package gr.aueb.cf.barberapp.service;

import gr.aueb.cf.barberapp.core.exceptions.EntityAlreadyExistsException;
import gr.aueb.cf.barberapp.core.exceptions.EntityInvalidArgumentException;
import gr.aueb.cf.barberapp.core.exceptions.EntityNotFoundException;
import gr.aueb.cf.barberapp.dto.UserInsertDTO;
import gr.aueb.cf.barberapp.dto.UserReadOnlyDTO;
import gr.aueb.cf.barberapp.dto.UserUpdateDTO;
import gr.aueb.cf.barberapp.mapper.Mapper;
import gr.aueb.cf.barberapp.model.Customer;
import gr.aueb.cf.barberapp.model.Role;
import gr.aueb.cf.barberapp.model.User;
import gr.aueb.cf.barberapp.repository.CustomerRepository;
import gr.aueb.cf.barberapp.repository.RoleRepository;
import gr.aueb.cf.barberapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service                        // IoC Container
@RequiredArgsConstructor        // DI
@Slf4j                          // Logger
public class UserService implements IUserService {

    // Self-registration always yields a customer; ADMIN accounts are seeded, not signed up for.
    private static final String CUSTOMER_ROLE = "CUSTOMER";

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final Mapper mapper;

    @Override
    @Transactional(rollbackFor = {EntityAlreadyExistsException.class, EntityInvalidArgumentException.class})
    public UserReadOnlyDTO register(UserInsertDTO dto)
            throws EntityAlreadyExistsException, EntityInvalidArgumentException {

        try {
            if (userRepository.existsByUsername(dto.username())) {
                throw new EntityAlreadyExistsException("Username",
                        "Username " + dto.username() + " is already taken");
            }
            if (userRepository.existsByEmail(dto.email())) {
                throw new EntityAlreadyExistsException("Email",
                        "Email " + dto.email() + " is already registered");
            }

            Role role = roleRepository.findByName(CUSTOMER_ROLE)
                    .orElseThrow(() -> new EntityInvalidArgumentException("Role",
                            "Role " + CUSTOMER_ROLE + " is missing - check the seed migration"));

            User user = mapper.mapToUserEntity(dto);
            user.setPassword(passwordEncoder.encode(dto.password()));
            role.addUser(user);

            // Every customer account carries a Customer profile from the outset, so
            // booking never has to lazily create one half-way through a checkout.
            Customer customer = new Customer();
            customer.addUser(user);
            customerRepository.save(customer);      // cascades the User

            log.info("User with username={} registered successfully", dto.username());
            return mapper.mapToUserReadOnlyDTO(user);

        } catch (EntityAlreadyExistsException e) {
            log.warn("Registration failed for username={}. Already exists.", dto.username());
            throw e;
        } catch (EntityInvalidArgumentException e) {
            log.error("Registration failed for username={}. Invalid argument.", dto.username(), e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserReadOnlyDTO getByUsername(String username) throws EntityNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User",
                        "User with username=" + username + " not found"));
        return mapper.mapToUserReadOnlyDTO(user);
    }

    @Override
    @Transactional(rollbackFor = {EntityNotFoundException.class, EntityAlreadyExistsException.class})
    public UserReadOnlyDTO updateOwnProfile(String username, UserUpdateDTO dto)
            throws EntityNotFoundException, EntityAlreadyExistsException {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User",
                        "User with username=" + username + " not found"));

        // Only check for a clash if the address actually changed, otherwise saving
        // an unmodified profile would collide with the user's own record.
        if (!user.getEmail().equalsIgnoreCase(dto.email())
                && userRepository.existsByEmail(dto.email())) {
            throw new EntityAlreadyExistsException("Email",
                    "Email " + dto.email() + " is already registered");
        }

        user.setFirstname(dto.firstname());
        user.setLastname(dto.lastname());
        user.setEmail(dto.email());
        user.setPhone(dto.phone());

        log.info("User with username={} updated their profile", username);
        return mapper.mapToUserReadOnlyDTO(user);
    }
}
