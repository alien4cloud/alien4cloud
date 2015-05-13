package alien4cloud.security;

import static org.junit.Assert.*;

import java.util.Arrays;

import javax.annotation.Resource;

import alien4cloud.security.model.User;
import alien4cloud.security.users.UserService;
import alien4cloud.security.users.rest.CreateUserRequest;
import alien4cloud.security.users.InMemoryUserDao;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import alien4cloud.security.spring.Alien4CloudAuthenticationProvider;

/**
 * @author luc boutier
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:spring-security-test.xml")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SecurityTest {

    @Resource
    private Alien4CloudAuthenticationProvider alienAuthenticationProvider;

    @Resource
    private UserService userService;

    @Resource
    private InMemoryUserDao userDao;

    @Test
    public void testSuccessLoginUserRoleFromDelegate() {
        testLoginSuccess("user", "user", new String[] { "ROLE_USER" });
    }

    @Test
    public void testSuccessLoginAdminRoleFromDelegate() {
        testLoginSuccess("admin", "admin", new String[] { "ROLE_USER", "ROLE_ADMIN" });
    }

    @Test(expected = BadCredentialsException.class)
    public void testBadCredentialsFromDelegate() {
        alienAuthenticationProvider.authenticate(new UsernamePasswordAuthenticationToken("admin", "wrongpass"));
    }

    @Test
    public void testCreateUserAndAuthenticate() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("guy");
        request.setPassword("password");
        userService.createUser(request.getUsername(), request.getEmail(), request.getFirstName(), request.getLastName(), request.getRoles(),
                request.getPassword());
    }

    public void testLoginSuccess(String userName, String password, String[] expectedAuthorities) {
        assertNull(userDao.find(userName));
        Authentication authentication = alienAuthenticationProvider.authenticate(new UsernamePasswordAuthenticationToken(userName, password));
        assertTrue(authentication.isAuthenticated());
        String[] authorities = new String[authentication.getAuthorities().size()];
        int i = 0;
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            authorities[i] = authority.getAuthority();
            i++;
        }
        Arrays.sort(expectedAuthorities);
        Arrays.sort(authorities);
        assertArrayEquals(expectedAuthorities, authorities);
        User user = userDao.find(userName);
        assertNotNull(user);
        assertEquals(userName, user.getUsername());
        assertNull(user.getPassword());
        assertArrayEquals(expectedAuthorities, user.getRoles());
        assertFalse(user.isInternalDirectory());
    }

}
