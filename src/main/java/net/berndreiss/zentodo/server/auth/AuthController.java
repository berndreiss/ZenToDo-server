package net.berndreiss.zentodo.server.auth;

import lombok.RequiredArgsConstructor;
import net.berndreiss.zentodo.server.user.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO DECRIBE
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private static final String SECRET_KEY = "your_secret_key";

    /**
     * TODO DESCRIBE
     * @param email
     * @param password
     * @return
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestParam String email, @RequestParam String password) {
        userService.registerUser(email, password);
        return ResponseEntity.ok("Registration successful! Please check your email to verify.");
    }

    /**
     * TODO DESCRIBE
     * @param email
     * @param password
     * @return
     */
    @PostMapping("/login")
    public Map<String, String> login(@RequestParam String email, @RequestParam String password){
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // Generate JWT Token
        String token = "";

        Map<String, String> response = new HashMap<>();
        response.put("message", "Login successful!");
        response.put("token", token);

        return response;
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
}
