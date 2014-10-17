package alien4cloud.ldap;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.stereotype.Component;

import alien4cloud.security.User;

/**
 * Mapping LDAP user to Alien 4 Cloud ES User
 *
 * @author mourouvi
 */
@Component
@Profile("security-ldap")
public class UserLdapAttributeMapper implements AttributesMapper<User> {

    @Value("${ldap.userIdKey}")
    private String userIdKey;

    @Value("${ldap.userFirstNameKey}")
    private String userFirstNameKey;

    @Value("${ldap.userLastNameKey}")
    private String userLastNameKey;

    @Value("${ldap.userEmailKey}")
    private String userEmailKey;

    @Value("${ldap.userActiveKey}")
    private String userActiveKey;

    @Value("${ldap.userActiveValue}")
    private String userActiveValue;

    @Override
    public User mapFromAttributes(Attributes attributes) throws NamingException {
        User user = new User();

        // Each attribute name is specific to the LDAP
        Attribute username = attributes.get(userIdKey);
        Attribute lastName = attributes.get(userLastNameKey);
        Attribute firstName = attributes.get(userFirstNameKey);
        Attribute email = attributes.get(userEmailKey);
        Attribute accountStatus = attributes.get(userActiveKey);

        // Each field may not be defined in LDAP
        if (username != null) {
            user.setUsername((String) username.get());
        }

        if (lastName != null) {
            user.setLastName((String) lastName.get());
        }

        if (firstName != null) {
            user.setFirstName((String) firstName.get());
        }

        if (email != null) {
            user.setEmail((String) email.get());
        }

        if (accountStatus != null) {
            String status = (String) accountStatus.get();
            user.setAccountNonExpired(userActiveValue == null || userActiveValue.equals(status));
        }

        return user;
    }
}
