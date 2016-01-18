package alien4cloud.security.spring.ldap;

import java.util.List;

import javax.annotation.Resource;

import lombok.Getter;
import lombok.Setter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Component;

import alien4cloud.security.model.User;

/**
 * Manage connection with LDAP for security management.
 */
@Getter
@Setter
@Component
@Conditional(LdapCondition.class)
public class LdapUserDao {
    @Resource
    private LdapTemplate ldapTemplate;

    @Resource
    private UserLdapAttributeMapper userLdapAttributeMapper;

    @Value("${ldap.filter}")
    private String filter;

    @Value("${ldap.mapping.id}")
    private String userIdKey;

    /**
     * Authenticate the user against ldap.
     * 
     * @param userName the user name.
     * @param password the user password.
     */
    public boolean authenticate(String userName, String password) {
        return ldapTemplate.authenticate("", userIdKey + "=" + userName, password);
    }

    /**
     * Return all users from LDAP.
     * 
     * @return The list of users in LDAP.
     */
    public List<User> getUsers() {
        return ldapTemplate.search("", this.filter, userLdapAttributeMapper);
    }

    /**
     * Find a user based on it's username/id
     * 
     * @param id The username / id of the user.
     * @return The user found in LDAP matching this username/id.
     */
    public User getById(String id) {
        String idFilter = "(" + userIdKey + "=" + id + ")";
        List<User> users = ldapTemplate.search("", idFilter, userLdapAttributeMapper);

        if (users == null || users.size() == 0) {
            return null;
        }
        return users.get(0);
    }
}
