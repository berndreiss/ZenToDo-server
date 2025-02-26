package net.berndreiss.zentodo.data;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import net.berndreiss.zentodo.auth.EmailService;
import net.berndreiss.zentodo.auth.TokenManager;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * TODO DESCRIBE
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final TokenManager tokenManager;
    private final AuthenticationManager authenticationManager;

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
     * @return
     */
    public String registerUser(String email, String password) throws Exception {

        ServerUser user = userRepository.findByEmail(email).orElse(null);

        List<Device> deviceList = deviceRepository.findAll().stream()
                .filter(device -> device.getEmail().equals(email))
                .sorted(Comparator.comparingInt(d -> (int) d.getId()))
                .toList();

        long deviceId = 0;
        if (!deviceList.isEmpty())
            deviceId = deviceList.getFirst().getId() + 1;

        if (user != null){
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
            final String jwtToken = tokenManager.generateJwtToken(user);
            return "1," + user.getId() + "," + deviceId + "," + jwtToken;
        }



        user = new ServerUser();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setDevice(deviceId);

        addNewDevice(deviceId, email);

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

        return "0," + user.getId() + "," + deviceId;
    }

    /**
     * TODO
     * @param id
     * @param email
     */
    public void addNewDevice(long id, String email){
        Device device = new Device();
        device.setId(id);
        device.setEmail(email);
        device.setExpiration(Instant.now().plus(21, ChronoUnit.DAYS));
        deviceRepository.save(device);
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
