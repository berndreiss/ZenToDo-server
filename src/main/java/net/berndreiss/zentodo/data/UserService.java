package net.berndreiss.zentodo.data;

import lombok.RequiredArgsConstructor;
import net.berndreiss.zentodo.auth.EmailService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * TODO DESCRIBE
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /**
     * TODO DESCRIBE
     * @param email
     * @param password
     */
    public void registerUser(String email, String password) {

        if (userRepository.findByEmail(email).isPresent())
            return;

        ServerUser user = new ServerUser();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));


        // Generate token for email verification
        String token = UUID.randomUUID().toString();
        user.setToken(token);
        user.setExpirationDate(LocalDateTime.now().plusMinutes(10));

        // Remove token after 10 minutes
        Thread thread = new Thread(() -> {

            try {
                Thread.sleep(600000); // Simulating long-running task
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            ServerUser user1 = userRepository.findByEmail(email).orElse(null);
            if (user1 == null || user1.isEnabled())
                return;
            user1.setToken(null);
            user1.setExpirationDate(null);
            userRepository.save(user1);
        });

        thread.start();

        userRepository.save(user);

        // Send verification email
        emailService.sendVerificationEmail(email, token);
    }

    /**
     * TODO DESCRIBE
     * @param email
     * @param token
     * @return
     */
    public boolean verifyEmail(String email, String token) {
        ServerUser user = userRepository.findByToken(token).orElse(null);
        if (user == null || user.getExpirationDate().isBefore(LocalDateTime.now())) {
            return false;
        }
        user.setEnabled(true);
        user.setToken(null);
        user.setExpirationDate(null);
        userRepository.save(user);
        return true;
    }
}
