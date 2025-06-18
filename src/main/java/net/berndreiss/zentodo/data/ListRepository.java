package net.berndreiss.zentodo.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ListRepository extends JpaRepository<TaskList, Long> {
    @Query("SELECT tl FROM Profile p JOIN p.lists tl WHERE p.profileId.user.id = :userId")
    List<TaskList> findAllListsByUserId(@Param("userId") long userId);
}
