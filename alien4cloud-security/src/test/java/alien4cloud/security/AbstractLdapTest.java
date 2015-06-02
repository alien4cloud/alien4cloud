package alien4cloud.security;

import java.util.List;

import javax.annotation.Resource;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;

import org.junit.Assert;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.core.LdapTemplate;

import alien4cloud.ldap.LdapUserDao;
import alien4cloud.ldap.UserLdapAttributeMapper;
import alien4cloud.security.model.User;

/**
 * Parent class for the LDAP tests.
 * 
 * @author luc boutier
 */
public class AbstractLdapTest {
    @Value("${ldap.mapping.id}")
    private String userIdKey;
    @Value("${ldap.mapping.firstname}")
    private String userFirstNameKey;
    @Value("${ldap.mapping.lastname}")
    private String userLastNameKey;
    @Value("${ldap.mapping.email}")
    private String userEmailKey;
    @Value("${ldap.mapping.active.key}")
    private String userActiveKey;
    @Value("${ldap.mapping.active.value}")
    private String userActiveValue;

    @Resource
    protected LdapTemplate ldapTemplate;

    @Resource
    protected UserLdapAttributeMapper attributeMapper;
    @Resource
    protected LdapUserDao ldapUserDao;

    public void assertUserMapper(Attributes attrUser, User user) throws NamingException {
        Assert.assertEquals(attrUser.get(userIdKey).get(), user.getUsername());
        Assert.assertEquals(attrUser.get(userFirstNameKey).get(), user.getFirstName());
        Assert.assertEquals(attrUser.get(userLastNameKey).get(), user.getLastName());
        Assert.assertEquals(attrUser.get(userEmailKey).get(), user.getEmail());
        Assert.assertEquals(attrUser.get(userActiveKey).get().equals(userActiveValue), user.isEnabled());
    }

    public List<User> createUserList(int userCount) throws NamingException {
        List<User> userList = com.google.common.collect.Lists.newArrayList();
        for (int i = 0; i < userCount; i++) {
            Attributes attrUser = new BasicAttributes();
            attrUser.put(userIdKey, "id_" + i);
            attrUser.put(userFirstNameKey, "firstName_" + i);
            attrUser.put(userLastNameKey, "lastName_" + i);
            attrUser.put(userEmailKey, "lastName_" + i + "@test.com");
            attrUser.put(userActiveKey, userActiveValue);
            User user = attributeMapper.mapFromAttributes(attrUser);
            assertUserMapper(attrUser, user);
            userList.add(user);
        }
        return userList;
    }

    /**
     * Prepare the LDAP template mock to return a list of users.
     * 
     * @param userCount number of users to return.
     * @return The list of users that the ldap template will return.
     * @throws NamingException
     */
    public List<User> prepareGetAllUserMock(int userCount) throws NamingException {
        List<User> userList = createUserList(userCount);
        String filter = "(&(objectClass=person)(objectClass=hordePerson)(!(objectClass=CalendarResource))(accountStatus=active))";
        Mockito.when(ldapTemplate.search("", filter, attributeMapper)).thenReturn(userList);
        return userList;
    }

    public String getUserIdKey() {
        return userIdKey;
    }
}
