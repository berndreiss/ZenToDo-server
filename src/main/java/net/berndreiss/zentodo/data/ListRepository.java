package net.berndreiss.zentodo.data;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ListRepository extends JpaRepository<TaskList, Long> {
}
