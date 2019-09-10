package alien4cloud.security.users;

import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.User;
import alien4cloud.security.users.rest.JwtToken;
import com.google.common.collect.Sets;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.DefaultClaims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;

/**
 * A service that manages JWT tokens.
 */
@Service
public class JwtTokenService {

    private static final String USER_SECRET = "userSecret";

    /**
     * A secret that is used to
     */
    @Value("${jwt.token.secret:my_app_secret}")
    private String secret;

    /**
     * In hours, the duration of the availability for a JWT token.
     */
    @Value("${jwt.token.ttlInHours:12}")
    private Integer tokenTtlInHours;

    public JwtToken createTokens(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        JwtToken token = createToken(user);
        return token;
    }

    public JwtToken createToken(User user) {
        Date expirationDate = getTokenExpirationDate(false);
        String token = Jwts.builder()
                .signWith(SignatureAlgorithm.HS512, secret)
                .setClaims(buildUserClaims(user))
                .setExpiration(expirationDate)
                .setIssuedAt(new Date())
                .compact();
        JwtToken jwtToken = new JwtToken();
        jwtToken.setToken(token);
        jwtToken.setExpireAt(expirationDate.getTime());
        return jwtToken;
    }

    public Jws<Claims> validateJwtToken(String token) {
        return Jwts.parser().setSigningKey(secret).parseClaimsJws(token);
    }

    private Date getTokenExpirationDate(boolean refreshToken) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, tokenTtlInHours);
        return calendar.getTime();
    }

    private Claims buildUserClaims(User user) {
        Claims claims = new DefaultClaims();

        claims.setSubject(String.valueOf(user.getUserId()));
        claims.put("username", user.getUsername());
        claims.put("email", user.getEmail());
        if (user.getRoles() != null) {
            claims.put("roles", String.join(",", user.getRoles()));
        }
        if (user.getGroupRoles() != null) {
            claims.put("groupRoles", String.join(",", user.getGroupRoles()));
        }
        // We use the encoded password as user salt
        claims.put(USER_SECRET, user.getPassword());

        return claims;
    }

    public Authentication buildAuthenticationFromClaim(Jws<Claims> claims) {
        String username = claims.getBody().get("username").toString();
        String email = (claims.getBody().get("email") != null) ? claims.getBody().get("email").toString() : "";

        String password = claims.getBody().get(USER_SECRET).toString();

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        if (claims.getBody().containsKey("roles")) {
            String rolesStr = claims.getBody().get("roles").toString();
            user.setRoles(rolesStr.split(","));
        }
        if (claims.getBody().containsKey("groupRoles")) {
            String groupRolesStr = claims.getBody().get("groupRoles").toString();
            user.setGroupRoles(Sets.newHashSet(groupRolesStr.split(",")));
        }

        // FIXME : we a database call here, for each request !!
        return AuthorizationUtil.createAuthenticationToken(user, password);
    }

}
