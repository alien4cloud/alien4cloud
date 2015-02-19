package alien4cloud.test.utils;

import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import alien4cloud.security.Role;

public class SecurityTestUtils {

    public static void setTestAuthentication(Role role) {
        Authentication auth = new TestingAuthenticationToken(role.name().toLowerCase(), "", role.name());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

}
