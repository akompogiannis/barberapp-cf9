package gr.aueb.cf.barberapp.repository;

import gr.aueb.cf.barberapp.model.Customer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByUuid(UUID uuid);
    Optional<Customer> findByUuidAndDeletedFalse(UUID uuid);

    @EntityGraph(attributePaths = {"user"})
    Optional<Customer> findByUser_Username(String username);

    boolean existsByUuidAndUser_Username(UUID customerUuid, String username);
}
