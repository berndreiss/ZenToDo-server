package net.berndreiss.zentodo.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "missing_queue_updates")
@Setter @Getter @NoArgsConstructor
public class MissingQueueUpdate {

    @Id
    private long id;

    @Column
    private List<Long> devices = new ArrayList<>();

    @Column (nullable = false)
    private String clock;

}
