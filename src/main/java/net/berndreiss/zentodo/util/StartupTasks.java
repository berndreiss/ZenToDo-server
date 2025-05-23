package net.berndreiss.zentodo.util;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import net.berndreiss.zentodo.data.DeviceRepository;
import net.berndreiss.zentodo.data.User;
import net.berndreiss.zentodo.data.UserRepository;
import net.berndreiss.zentodo.data.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class StartupTasks {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void onStartup() {

        List<User> disabledUsers = userRepository.findAll().stream().filter(u -> !u.isEnabled()).toList();

        for (User user: disabledUsers) {
            userRepository.deleteById(user.getId());
        }

        if (!disabledUsers.isEmpty())
            System.out.println("Removed disabled users.");
        //Random random = new Random();
        //long userId = random.nextLong();
        //while(userRepository.findById(userId).isPresent())
            //userId++;
        //User user = new User();
        //user.setEmail("test@test.net");
        //user.setId(userId);
        //user.setClock((new VectorClock().jsonify()));
        //user.setPassword(passwordEncoder.encode("Test1234!?"));
        //user.setEnabled(true);
        //userRepository.save(user);
    }
}
