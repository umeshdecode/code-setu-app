package com.cargosetu.config;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.model.RefreshToken;
import com.cargosetu.repo.RefreshTokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtils {
    private static final long EXPIRATION = 1000 * 60 * 60 * 24; // 24h
    private final String secretKey = "Sa419Xr9r2R9gZPWS1gJNPfzs16rQzqX7MGk4z7E5GY=";
    private final Key key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    private final RefreshTokenRepository refreshTokenRepository;

    public JwtUtils(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public Long getTokenExpirationTime(){
        return System.currentTimeMillis() + EXPIRATION;
    }

    public String generateToken(LoggedInUser user) {

        return Jwts.builder()
                .setSubject(user.getStaffId().toString())
                .claim("staffId",user.getStaffId())
                .claim("uuid", user.getUuid())
                .claim("roles", user.getRoles())
                .claim("userName",user.getUsername())
                .claim("firstName", user.getFirstName())
                .claim("lastName", user.getLastName())
                .claim("email",user.getEmail())
                .claim("permissions", user.getPermissions())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(key)
                .compact();
    }
    public Claims validateToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public RefreshToken createRefreshToken(Long staffId) {

        RefreshToken token = new RefreshToken();
        token.setStaffId(staffId);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiresAt(
                Instant.now().plus(30, ChronoUnit.DAYS)
        );

        return refreshTokenRepository.save(token);
    }

    public Claims getClaimsFromExpiredToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            //  even if expired, we return the claims
            return e.getClaims();
        }
    }
}
