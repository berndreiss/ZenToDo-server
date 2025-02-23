package net.berndreiss.zentodo.auth;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * TODO DESCRIBE
 */
@Getter @Setter @NoArgsConstructor
public class JwtRequestModel {
    private String email;
    private String password;

}