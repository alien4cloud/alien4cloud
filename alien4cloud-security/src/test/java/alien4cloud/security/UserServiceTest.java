package alien4cloud.security;

import java.lang.reflect.Field;

import javax.annotation.Resource;

import alien4cloud.security.model.User;
import alien4cloud.security.users.UserService;
import alien4cloud.security.users.IAlienUserDao;
import org.junit.FixMethodOrder;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import alien4cloud.dao.model.GetMultipleDataResult;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:ldap-authentication-provider-security-test.xml")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UserServiceTest {
    @Resource
    private UserService userService;
    @Resource
    private IAlienUserDao alienUserDao;

    private void enableEnsure() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field f = UserService.class.getDeclaredField("ensure");
        f.setAccessible(true);
        f.setBoolean(userService, true);
    }

    @Test
    public void testEnsureAdminUserShouldNotCreateUser() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        Mockito.reset(alienUserDao);
        enableEnsure();
        GetMultipleDataResult searchResult = new GetMultipleDataResult(null, null, 0, 1, 0, 1);
        Mockito.when(alienUserDao.find(Mockito.anyMap(), Mockito.eq(1))).thenReturn(searchResult);
        userService.ensureAdminUser();

        Mockito.verify(alienUserDao, Mockito.never()).save(Mockito.any(User.class));
    }

    @Test
    public void testEnsureAdminUserShouldCreateUser() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        Mockito.reset(alienUserDao);
        enableEnsure();
        GetMultipleDataResult searchResult = new GetMultipleDataResult(null, null, 0, 0, 0, 0);
        Mockito.when(alienUserDao.find(Mockito.anyMap(), Mockito.eq(1))).thenReturn(searchResult);
        userService.ensureAdminUser();

        Mockito.verify(alienUserDao, Mockito.times(1)).save(Mockito.any(User.class));
    }

    @After
    public void cleanup() {
        Mockito.reset(alienUserDao);
    }
}
