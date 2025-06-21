package net.berndreiss.zentodo.data;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MissingQueueUpdatesRepository extends JpaRepository<MissingQueueUpdate, Long> {
    MissingQueueUpdate findById(long id);
}
