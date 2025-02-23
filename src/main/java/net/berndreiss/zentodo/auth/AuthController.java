package net.berndreiss.zentodo.auth;

import lombok.RequiredArgsConstructor;
import net.berndreiss.zentodo.data.ServerUser;
import net.berndreiss.zentodo.data.UserRepository;
import net.berndreiss.zentodo.data.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * TODO DECRIBE
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final UserRepository userRepository;

    @Autowired
    private TokenManager tokenManager;

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
     * @param request
     * @return
     */
    @PostMapping("/login")
    public ResponseEntity<String> createToken(@RequestBody JwtRequestModel
                                                                request) throws Exception {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (DisabledException e) {
            throw new Exception("USER_DISABLED", e);
        } catch (BadCredentialsException e) {
            throw new Exception("INVALID_CREDENTIALS", e);
        }
        Optional<ServerUser> user = userRepository.findByEmail(request.getEmail());
        if (user.isEmpty())
            return ResponseEntity.notFound().build();
        final String jwtToken = tokenManager.generateJwtToken(user.get());
        return ResponseEntity.ok(jwtToken);
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
