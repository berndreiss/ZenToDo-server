package net.berndreiss.zentodo.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@Getter @Setter
@Entity
public class Message {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private long id;

    @OneToMany(mappedBy = "message", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Acknowledgement> acknowledgements;
}
