package net.berndreiss.zentodo.data;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import java.util.Optional;

/**
 * TODO DESCRIBE
 */
public interface EntryRepository extends JpaRepository<Entry, Long> {
    List<Entry> findAllByUserId(Long userId);
}
