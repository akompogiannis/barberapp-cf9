package gr.aueb.cf.barberapp.repository;

import gr.aueb.cf.barberapp.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Role + capabilities are fetched eagerly, otherwise getAuthorities() would blow up
    // with a LazyInitializationException since open-in-view is off.
    @EntityGraph(attributePaths = {"role", "role.capabilities"})
    Optional<User> findByUsername(String username);

    Optional<User> findByUuid(UUID uuid);
    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
