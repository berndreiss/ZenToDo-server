package net.berndreiss.zentodo.util;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import net.berndreiss.zentodo.data.DeviceRepository;
import net.berndreiss.zentodo.data.User;
import net.berndreiss.zentodo.data.UserRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StartupTasks {

    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;

    @PostConstruct
    public void onStartup() {

        List<User> disabledUsers = userRepository.findAll().stream().filter(u -> !u.isEnabled()).toList();

        for (User user: disabledUsers) {
            userRepository.deleteById(user.getId());
        }

        if (!disabledUsers.isEmpty())
            System.out.println("Removed disabled users.");
    }
}
