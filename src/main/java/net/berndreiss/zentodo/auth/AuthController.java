package net.berndreiss.zentodo.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletResponse;
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

import java.time.Instant;
import java.util.Date;
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
     * @param requestModel
     * @return
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody JwtRequestModel requestModel) throws Exception {

        String status = userService.exists(requestModel.getEmail());

        if (status != null)
            return ResponseEntity.ok(status);

        long id = userService.registerUser(requestModel.getEmail(), requestModel.getPassword());

        return ResponseEntity.ok(String.valueOf(id));
    }

    /**
     * TODO DESCRIBE
     * @param request
     * @return
     */
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody JwtRequestModel
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

        ServerUser user = userRepository.findByEmail(requestModel.getEmail()).orElse(null);

        if (user == null)
            return ResponseEntity.status(401).build();


        return ResponseEntity.ok(tokenManager.generateJwtToken(user));
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
