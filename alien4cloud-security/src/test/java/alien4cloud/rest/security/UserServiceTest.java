package alien4cloud.rest.security;

import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import alien4cloud.exception.AlreadyExistException;
import alien4cloud.security.CreateUserRequest;
import alien4cloud.security.User;
import alien4cloud.security.UserService;

/**
 * 
 * @author luc boutier
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:spring-security-test.xml")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UserServiceTest {
    @Resource
    private UserService userService;

    @Test
    public void testCreateUser() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("oneguy");
        request.setPassword("password");
        userService.createUser(request.getUsername(), request.getEmail(), request.getFirstName(), request.getLastName(), request.getRoles(),
                request.getPassword());
        User createdUser = userService.retrieveUser(request.getUsername());
        Assert.assertNotNull(createdUser);
        Assert.assertEquals(request.getUsername(), createdUser.getUsername());
        // Password must be encrypted
        Assert.assertNotEquals(request.getPassword(), createdUser.getPassword());
    }

    @Test(expected = AlreadyExistException.class)
    public void testCreateUserFail() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("aguy");
        request.setPassword("whatever");
        userService.createUser(request.getUsername(), request.getEmail(), request.getFirstName(), request.getLastName(), request.getRoles(),
                request.getPassword());
        userService.createUser(request.getUsername(), request.getEmail(), request.getFirstName(), request.getLastName(), request.getRoles(),
                request.getPassword());
    }
}
