package net.berndreiss.zentodo.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor @Getter @Setter
public class Acknowledgement {

    @Id
    @GeneratedValue (strategy =  GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn (name = "message_id", nullable = false)
    private Message message;

    @Column
    private long missingQueueUpdateId;


}
