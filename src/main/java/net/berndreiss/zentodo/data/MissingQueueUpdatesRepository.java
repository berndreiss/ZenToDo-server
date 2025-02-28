package net.berndreiss.zentodo.data;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MissingQueueUpdatesRepository extends JpaRepository<MissingQueueUpdate, Long> {
    List<MissingQueueUpdate> findById(long id);
}
