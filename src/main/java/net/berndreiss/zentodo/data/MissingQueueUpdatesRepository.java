package net.berndreiss.zentodo.data;

import net.berndreiss.zentodo.util.VectorClock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Map;

public interface MissingQueueUpdatesRepository extends JpaRepository<MissingQueueUpdate, Long> {
    MissingQueueUpdate findById(long id);
}
