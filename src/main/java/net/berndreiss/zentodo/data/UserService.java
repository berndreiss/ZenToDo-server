package net.berndreiss.zentodo.data;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import net.berndreiss.zentodo.auth.EmailService;
import net.berndreiss.zentodo.auth.TokenManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
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
    private final TokenManager tokenManager;

    public String exists(String email){
        ServerUser user = userRepository.findByEmail(email).orElse(null);

        if (user == null)
            return null;
        if (user.isEnabled())
            return "enabled";
        return "exists";
    }

    /**
     * TODO DESCRIBE
     * @param email
     * @param password
     */
    public long registerUser(String email, String password) throws Exception {

        if (userRepository.findByEmail(email).isPresent())
            throw new Exception("User already exists");

        ServerUser user = new ServerUser();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));



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
            userRepository.delete(user1);
        });

        thread.start();

        userRepository.save(user);

        String token = tokenManager.generateJwtToken(user);
        // Send verification email
        emailService.sendVerificationEmail(email, token);

        return user.getId();
    }

    /**
     * TODO DESCRIBE
     * @param email
     * @param token
     * @return
     */
    public boolean verifyEmail(String email, String token) {
        ServerUser user = userRepository.findByEmail(email).orElse(null);

        Claims claims = Jwts
                .parserBuilder()
                .setSigningKey(tokenManager.getKey())
                .build()
                .parseClaimsJws(token).getBody();
        if (user == null || claims.getExpiration().before(new Date()) || !claims.getSubject().equals(email)) {
            return false;
        }
        user.setEnabled(true);
        userRepository.save(user);
        return true;
    }
}
