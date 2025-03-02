package net.berndreiss.zentodo.auth;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * TODO DESCRIBE
 */
@Component
public class TokenManager {

    public static final long TOKEN_VALIDITY = 10 * 60 * 60;

    @Value("${SECURITY_JWT_SECRET-KEY}")
    private String jwtSecret;

    /**
     * TODO DESCRIBE
     * @param userDetails
     * @return
     */
    public String generateJwtToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return Jwts
                .builder()
                .setClaims(claims)  // set the claims
                .setSubject(userDetails.getUsername())  // set the username as subject in payload
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + TOKEN_VALIDITY * 1000))
                .signWith(getKey(), SignatureAlgorithm.HS256)  // signature part
                .compact();
    }

    /**
     * TODO DESCRIBE
     * @param token
     * @param userDetails
     * @return
     */
    public Boolean validateJwtToken(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        final Claims claims = Jwts
                .parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token).getBody();
        Boolean isTokenExpired = claims.getExpiration().before(new Date());
        return (username.equals(userDetails.getUsername())) && !isTokenExpired;
    }

    /**
     * TODO DESCRIBE
     * @param token
     * @return
     */
    public String getUsernameFromToken(String token) {
        final Claims claims = Jwts
                .parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token).getBody();
        return claims.getSubject();
    }

    // create a signing key based on secret
    public Key getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public Claims getBodyFromToken(String token){

        return Jwts
                .parserBuilder()
                .setSigningKey(getKey())
                .build().parseClaimsJws(token.substring(7))
                .getBody();

    }
    public String getMailFromToken(String token){
        Claims claims = getBodyFromToken(token);
        return claims.getSubject();
    }
}