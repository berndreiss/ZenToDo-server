package net.berndreiss.zentodo.data;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import net.berndreiss.zentodo.auth.EmailService;
import net.berndreiss.zentodo.auth.TokenManager;
import net.berndreiss.zentodo.util.VectorClock;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * TODO DESCRIBE
 */
@Service
@RequiredArgsConstructor
public class UserService {

    public final UserRepository repository;
    public final DeviceRepository deviceRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final TokenManager tokenManager;
    private final AuthenticationManager authenticationManager;

    public String exists(String email){
        User user = repository.findByEmail(email).orElse(null);

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

        User user = repository.findByEmail(email).orElse(null);


        List<Device> deviceList = null;
        if (user != null) {
            addNewDevice(user);
            final long userId = user.getId();
            deviceList = deviceRepository.findAll().stream()
                    .filter(device -> device.getUser().getId() == userId)
                    .sorted(Comparator.comparingInt(d -> (int) d.getId()))
                    .toList();
        }
        int deviceId = 0;
        if (deviceList != null && !deviceList.isEmpty())
            deviceId = deviceList.getFirst().getId() + 1;

        if (user != null){
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
            final String jwtToken = tokenManager.generateJwtToken(new UserWrapper(user));
            return "1," + user.getId() + "," + deviceId + "," + jwtToken;
        }



        Random random = new Random();
        long userId = random.nextLong();
        while(repository.findById(userId).isPresent())
            userId++;

        VectorClock clock = new VectorClock();
        clock.entries.put(deviceId, 0L);

        user = new User();
        user.setId(userId);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setDevice(deviceId);
        user.setClock(clock.jsonify());
        repository.save(user);

        addNewDevice(user);


        // Remove token after 10 minutes
        Thread thread = new Thread(() -> {

            try {
                Thread.sleep(600000); // Simulating long-running task
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            User user1 = repository.findByEmail(email).orElse(null);
            if (user1 == null || user1.isEnabled())
                return;
            repository.delete(user1);
        });

        thread.start();


        String token = tokenManager.generateJwtToken(new UserWrapper(user));
        // Send verification email
        emailService.sendVerificationEmail(email, token);

        return "0," + user.getId() + "," + deviceId;
    }

    /**
     * TODO
     * @param user
     */
    public int addNewDevice(User user){
        List<Device> deviceList = deviceRepository.findAll().stream()
                .filter(device -> Objects.equals(device.getUser().getId(), user.getId()))
                .sorted(Comparator.comparingInt(d -> (int) d.getId()))
                .toList();

        int deviceId = 0;
        if (!deviceList.isEmpty())
            deviceId = deviceList.getLast().getId() + 1;

        user.setDevice(deviceId);

        VectorClock clock = new VectorClock(user.getClock());
        clock.addDevice(deviceId);
        user.setClock(clock.jsonify());
        repository.save(user);
        Device device = new Device();
        device.setId(deviceId);
        device.setUser(user);
        device.setExpiration(Instant.now().plus(21, ChronoUnit.DAYS));
        deviceRepository.save(device);

        return deviceId;
    }

    /**
     * TODO DESCRIBE
     * @param email
     * @param token
     * @return
     */
    public boolean verifyEmail(String email, String token) {
        User user = repository.findByEmail(email).orElse(null);

        Claims claims = Jwts
                .parserBuilder()
                .setSigningKey(tokenManager.getKey())
                .build()
                .parseClaimsJws(token).getBody();
        if (user == null || claims.getExpiration().before(new Date()) || !claims.getSubject().equals(email)) {
            return false;
        }
        user.setEnabled(true);
        repository.save(user);
        return true;
    }

    public User getByMail(String email){

        return repository.findByEmail(email).orElse(null);
    }

    public List<Integer> getOtherDevices(User user, Integer device){
        return deviceRepository.findAll().stream()
                .filter(d -> Objects.equals(d.getUser().getId(), user.getId()) && d.getId() != device)
                .map(Device::getId)
                .toList();

    }
    public List<Integer> getDevices(User user){
        return deviceRepository.findAll().stream()
                .map(Device::getId)
                .toList();

    }
}
