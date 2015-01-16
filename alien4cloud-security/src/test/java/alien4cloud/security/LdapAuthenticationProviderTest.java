package alien4cloud.security;

import java.util.List;

import javax.annotation.Resource;
import javax.naming.NamingException;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import alien4cloud.ldap.LdapAuthenticationProvider;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:ldap-authentication-provider-security-test.xml")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LdapAuthenticationProviderTest extends AbstractLdapTest {
    @Resource
    private IAlienUserDao alienUserDao;
    @Resource
    private LdapTemplate ldapTemplate;
    @Resource
    private LdapAuthenticationProvider ldapAuthenticationProvider;

    @Test
    public void testLdapUserImport() throws NamingException {
        Mockito.when(ldapTemplate.getContextSource()).thenReturn(Mockito.mock(ContextSource.class));
        int userCount = 10;
        List<User> users = prepareGetAllUserMock(userCount);

        // for each user we should check if it exists in the user repository and only if not then we add it.
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            if (i % 2 == 0) {
                user.setLastName("test");
                Mockito.when(alienUserDao.find(user.getUsername())).thenReturn(user);
            } else {
                Mockito.when(alienUserDao.find(user.getUsername())).thenReturn(user);
            }
        }

        ldapAuthenticationProvider.importLdapUsers();

        Mockito.verify(alienUserDao, Mockito.times(users.size())).save(Mockito.any(User.class));
    }

    @Test
    public void testAuthenticate() {
        String userName = "admin";
        String password = "admin";

        Mockito.when(ldapTemplate.authenticate("", getUserIdKey() + "=" + userName, password)).thenReturn(true);
        ldapAuthenticationProvider.authenticate(new UsernamePasswordAuthenticationToken(userName, password));
    }

    @Test(expected = BadCredentialsException.class)
    public void testAuthenticateShouldFailIfWrontPassword() {
        String userName = "admin";
        String password = "admin";

        Mockito.when(ldapTemplate.authenticate("", getUserIdKey() + "=" + userName, password)).thenReturn(false);
        ldapAuthenticationProvider.authenticate(new UsernamePasswordAuthenticationToken(userName, password));
    }
}
