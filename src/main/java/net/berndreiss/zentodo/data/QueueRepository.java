package net.berndreiss.zentodo.data;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QueueRepository extends JpaRepository<QueueItem, Long> {
    List<QueueItem> findByUserId(long userId);
}
