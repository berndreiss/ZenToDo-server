package net.berndreiss.zentodo.data;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;


/**
 * TODO DESCRIBE
 */
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findAllByUserId(Long userId);
}
