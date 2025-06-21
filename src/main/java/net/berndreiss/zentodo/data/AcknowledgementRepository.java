package net.berndreiss.zentodo.data;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AcknowledgementRepository extends JpaRepository<Acknowledgement, String> {
}
