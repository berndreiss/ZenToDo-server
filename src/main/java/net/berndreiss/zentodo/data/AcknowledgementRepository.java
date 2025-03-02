package net.berndreiss.zentodo.data;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface AcknowledgementRepository extends JpaRepository<Acknowledgement, String> {
}
