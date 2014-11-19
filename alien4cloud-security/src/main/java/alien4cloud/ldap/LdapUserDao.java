package alien4cloud.ldap;

import java.util.List;

import javax.annotation.Resource;

import lombok.Getter;
import lombok.Setter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Profile;
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
//@Profile("security-ldap")
@Conditional(LdapCondition.class)
public class LdapUserDao {

    @Resource
    private LdapTemplate ldapTemplate;

    @Resource
    private UserLdapAttributeMapper userLdapAttributeMapper;

    @Value("${ldap.objectClassesInclude}")
    private String objectClassInclude;

    @Value("${ldap.objectClassesExclude}")
    private String objectClassExclude;

    @Value("${ldap.userIdKey}")
    private String userIdKey;

    @Value("${ldap.userActiveKey}")
    private String userActiveKey;

    @Value("${ldap.userActiveValue}")
    private String userActiveValue;

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
        AndFilter andFilter = getIncludeAndExcludeClass(objectClassInclude, objectClassExclude);
        if (onlyActiveUsers) {
            andFilter.and(new EqualsFilter(userActiveKey, userActiveValue));
        }
        return ldapTemplate.search("", andFilter.encode(), userLdapAttributeMapper);
    }

    /**
     * Find a user based on it's username/id
     * 
     * @param id The username / id of the user.
     * @return The user found in LDAP matching this username/id.
     */
    public User getById(String id) {
        AndFilter andFilter = getIncludeAndExcludeClass(objectClassInclude, objectClassExclude);

        // filter by id
        EqualsFilter equalsFilter = new EqualsFilter(userIdKey, id);
        andFilter.and(equalsFilter);

        List<User> users = ldapTemplate.search("", andFilter.encode(), userLdapAttributeMapper);

        if (users == null || users.size() == 0) {
            return null;
        }
        return users.get(0);
    }

    private AndFilter getIncludeAndExcludeClass(final String classesInclude, final String classesExclude) {
        AndFilter andFilter = new AndFilter();

        // included classes filters
        for (String clazz : classesInclude.split(",")) {
            andFilter.and(new EqualsFilter("objectClass", clazz));
        }

        // excluded classes filters
        for (String clazz : classesExclude.split(",")) {
            andFilter.and(new NotFilter(new EqualsFilter("objectClass", clazz)));
        }

        return andFilter;
    }
}
