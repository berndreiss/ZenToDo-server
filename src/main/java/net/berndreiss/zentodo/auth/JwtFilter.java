package net.berndreiss.zentodo.auth;

import java.io.IOException;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import net.berndreiss.zentodo.data.User;
import net.berndreiss.zentodo.data.UserRepository;
import net.berndreiss.zentodo.data.UserWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import io.jsonwebtoken.ExpiredJwtException;


/**
 * TODO DESRIBE
 */
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final UserRepository userRepository;
    @Autowired
    private TokenManager tokenManager;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String tokenHeader = request.getHeader("Authorization");

        User user = null;
        System.out.println(tokenHeader);
        String email = null;
        String token = null;
        // if bearer token is provided, get the username
        if (tokenHeader != null && tokenHeader.startsWith("Bearer ")) {
            token = tokenHeader.substring(7);
            try {
                email = tokenManager.getUsernameFromToken(token);
                user = userRepository.findByEmail(email).orElse(null);
                if (user == null || !user.isEnabled()) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User account is disabled");
                    return;
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Unable to get JWT Token");
            } catch (ExpiredJwtException e) {
                System.out.println("JWT Token has expired");
            }
        } else {
            System.out.println("Bearer String not found in token");
        }
        // validate the JWT Token and create a new authentication token and set in security context
        if (null != email && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (user == null)
                throw new UsernameNotFoundException("User with mail " + email + " could not be found.");

            UserWrapper userDetails = new UserWrapper(user);

            if (tokenManager.validateJwtToken(token, userDetails)) {
                UsernamePasswordAuthenticationToken
                        authenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authenticationToken.setDetails(new
                        WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }


        String device = request.getHeader("device");

        System.out.println("DEVICE: " + device);

        if (device != null){
            if (user != null){
                user.setDevice(Long.parseLong(device));
                System.out.println(user.getDevice());
                userRepository.save(user);
            }
        }

        filterChain.doFilter(request, response);
    }
}
