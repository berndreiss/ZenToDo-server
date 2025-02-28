package net.berndreiss.zentodo.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Table (name = "users")
@Getter
@Setter
@NoArgsConstructor
public class ServerUser implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column
    private String userName = null;

    @Column(nullable = false)
    private String password;

    @OneToMany (mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Device> devices = new ArrayList<>();

    @Column
    private boolean enabled = false;

    @Column (nullable = false)
    private long device;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getUsername() {
        return email;
    }

}
