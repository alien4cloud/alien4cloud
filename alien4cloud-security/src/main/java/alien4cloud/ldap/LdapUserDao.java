package alien4cloud.ldap;

import java.util.List;

import javax.annotation.Resource;

import lombok.Getter;
import lombok.Setter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.filter.NotFilter;
import org.springframework.stereotype.Component;

import alien4cloud.security.User;

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
     * @param onlyActiveUsers If true only users that are active in LDAP will be retrieved.
     * @return The list of users in LDAP.
     */
    public List<User> getUsers(Boolean onlyActiveUsers) {
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
