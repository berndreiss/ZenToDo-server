package net.berndreiss.zentodo.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table (name="devices")
@Getter @Setter @NoArgsConstructor
public class Device {

    @Id
    @Column (nullable = false)
    private long id;

    @Column
    private Instant expiration;

    @ManyToOne
    @JoinColumn(name = "user_id",nullable = false)
    private ServerUser user;
}
