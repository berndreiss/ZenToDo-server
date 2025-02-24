package net.berndreiss.zentodo.data;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * TODO DESCRIBE
 */
public interface UserRepository extends JpaRepository<ServerUser, Long> {
    Optional<ServerUser> findByEmail(String email);
}
