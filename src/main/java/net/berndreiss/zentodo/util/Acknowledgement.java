package net.berndreiss.zentodo.util;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class Acknowledgement {

    private long id;
    private String email;
    private long device;

}
