package alien4cloud.security;

import java.util.List;

import javax.naming.NamingException;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import alien4cloud.security.model.User;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:spring-security-test.xml")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles("security-ldap")
public class LdapUserDaoTest extends AbstractLdapTest {

    @Test
    public void getAllUsers() throws NamingException {
        int userCount = 10;
        prepareGetAllUserMock(userCount);

        List<User> users = ldapUserDao.getUsers();
        Assert.assertEquals(userCount, users.size());
    }

    @Test
    public void getUserByUid() throws NamingException {
        int userCount = 10;
        List<User> userList = createUserList(userCount);

        String userName = userList.get(0).getUsername();
        String filter = "(uid=" + userName + ")";

        Mockito.when(ldapTemplate.search("", filter, attributeMapper)).thenReturn(Lists.newArrayList(userList.get(0)));

        User user = ldapUserDao.getById(userList.get(0).getUsername());
        Assert.assertNotNull("A user with username <" + userList.get(0).getUsername() + "> must exist.", user);
    }

    @Test
    public void testLdapAuthenticate() {
        String userName = "admin";
        String password = "admin";

        Mockito.when(ldapTemplate.authenticate("", getUserIdKey() + "=" + userName, password)).thenReturn(true);

        boolean auth = ldapUserDao.authenticate(userName, password);
        Assert.assertTrue(auth);
    }
}