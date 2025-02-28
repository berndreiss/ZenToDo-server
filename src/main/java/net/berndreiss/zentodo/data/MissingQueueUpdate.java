package net.berndreiss.zentodo.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "missing_queue_updates")
@Setter @Getter @NoArgsConstructor
public class MissingQueueUpdate {

    @Id
    private long id;

    @Column
    private List<Long> devices;
}
