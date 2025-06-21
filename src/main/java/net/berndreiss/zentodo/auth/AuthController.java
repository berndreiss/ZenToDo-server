package net.berndreiss.zentodo.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import net.berndreiss.zentodo.data.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * TODO DECRIBE
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final EntryService entryService;
    private final ListService listService;
    private final ProfileService profileService;

    @Autowired
    private TokenManager tokenManager;

    /**
     * TODO DESCRIBE
     * @param requestModel
     * @return
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody JwtRequestModel requestModel) throws Exception {


        User user = userRepository.findByEmail(requestModel.getEmail()).orElse(null);
        if (user != null){

            if (!user.isEnabled())
                return ResponseEntity.ok("exists," + user.getId() + "," + user.getDevice());

            int newDevice = userService.addNewDevice(user);
            entryService.addAllToQueue(user, newDevice);

            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(requestModel.getEmail(), requestModel.getPassword()));
            final String jwtToken = tokenManager.generateJwtToken(new UserWrapper(user));
            return ResponseEntity.ok("logged_in," + user.getId() + "," + newDevice + "," + jwtToken);
        }

        String response = userService.registerUser(requestModel.getEmail(), requestModel.getPassword());

        return ResponseEntity.ok(response);
    }

    /**
     * TODO DESCRIBE
     * @param request
     * @return
     */
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody JwtRequestModel request) throws Exception {
        Optional<User> user = userRepository.findByEmail(request.getEmail());
        if (user.isEmpty())
            return ResponseEntity.status(404).build();
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (DisabledException e) {
            throw new Exception("USER_DISABLED", e);
        } catch (BadCredentialsException e) {
            throw new Exception("INVALID_CREDENTIALS", e);
        }
        final String jwtToken = tokenManager.generateJwtToken(new UserWrapper(user.get()));
        return ResponseEntity.ok(jwtToken);
    }


    /**
     * TODO DESCRIBE
     * @param requestModel
     * @return
     */
    @PostMapping ("/renewToken")
    public ResponseEntity<String> renewToken(@RequestBody JwtRequestModel requestModel, HttpServletResponse response){
        String oldToken = requestModel.getPassword();

        Claims claims = Jwts
                .parserBuilder()
                .setSigningKey(tokenManager.getKey())
                .build()
                .parseClaimsJws(oldToken)
                .getBody();

        boolean expired = claims.getExpiration().before(new Date());

        if (expired || !claims.getSubject().equals(requestModel.getEmail()))
            return ResponseEntity.status(401).build();

        User user = userRepository.findByEmail(requestModel.getEmail()).orElse(null);

        if (user == null)
            return ResponseEntity.status(401).build();

        return ResponseEntity.ok(tokenManager.generateJwtToken(new UserWrapper(user)));
    }

    /**
     * TODO DESCRIBE
     * @param email
     * @param token
     * @return
     */
    @GetMapping("/verify")
    public ResponseEntity<String> verifyEmail(@RequestParam String email, @RequestParam String token) {
        boolean verified = userService.verifyEmail(email, token);
        return verified ? ResponseEntity.ok("Email verified successfully!") :
                ResponseEntity.badRequest().body("Invalid or expired verification token.");
    }

    /**
     * TODO
     * @param requestModel
     * @return
     */
    @PostMapping("/status")
    public ResponseEntity<String> status(@RequestBody JwtRequestModel requestModel) throws Exception {

        System.out.println("STATUS FOR " + requestModel.getEmail());
        String status = userService.exists(requestModel.getEmail());

        if (status == null)
            return ResponseEntity.ok("non");

        if (!status.equals("enabled"))
            return ResponseEntity.ok(status);


        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(requestModel.getEmail(), requestModel.getPassword()));
        } catch (DisabledException e) {
            throw new Exception("USER_DISABLED", e);
        } catch (BadCredentialsException e) {
            throw new Exception("INVALID_CREDENTIALS", e);
        }

        User user = userRepository.findByEmail(requestModel.getEmail()).orElse(null);
        if (user == null)
            throw new Exception("User not retrieved.");
        return ResponseEntity.ok(status + "," + tokenManager.generateJwtToken(new UserWrapper(user)));

    }
    @GetMapping ("test")
    public ResponseEntity<String> test() {
        return  ResponseEntity.ok("okay");
    }
}
