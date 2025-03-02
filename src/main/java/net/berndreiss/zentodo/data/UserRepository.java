package net.berndreiss.zentodo.data;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * TODO DESCRIBE
 */
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findById(long id);
}
