package net.berndreiss.zentodo.data;

import net.berndreiss.zentodo.data.Entry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;


/**
 * TODO DESCRIBE
 */
public interface EntryRepository extends JpaRepository<Entry, Long> {
    List<Entry> findAllByUserId(Long userId);
}
