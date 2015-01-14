package alien4cloud.ldap;

import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.stereotype.Component;

import alien4cloud.security.User;

import com.google.common.collect.Lists;

/**
 * Mapping LDAP user to Alien 4 Cloud ES User
 */
@Component
@Conditional(LdapCondition.class)
public class UserLdapAttributeMapper implements AttributesMapper<User> {

    @Value("${ldap.mapping.id}")
    private String userIdKey;

    @Value("${ldap.mapping.firstname}")
    private String userFirstNameKey;

    @Value("${ldap.mapping.lastname}")
    private String userLastNameKey;

    @Value("${ldap.mapping.email}")
    private String userEmailKey;

    @Value("${ldap.mapping.active.key:}")
    private String userActiveKey;

    @Value("${ldap.mapping.active.value:}")
    private String userActiveValue;

    @Value("${ldap.mapping.roles.key:}")
    private String userRolesKey;

    @Override
    public User mapFromAttributes(Attributes attributes) throws NamingException {
        User user = new User();

        // Each attribute name is specific to the LDAP
        Attribute username = attributes.get(userIdKey);
        Attribute lastName = attributes.get(userLastNameKey);
        Attribute firstName = attributes.get(userFirstNameKey);
        Attribute email = attributes.get(userEmailKey);
        Attribute accountStatus = (userActiveKey == null || userActiveKey.isEmpty()) ? null : attributes.get(userActiveKey);
        Attribute roles = (userRolesKey == null || userRolesKey.isEmpty()) ? null : attributes.get(userRolesKey);

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

        if (roles != null && roles.size() != 0) {
            List<String> userRoles = Lists.newArrayList();
            if (roles.size() > 1) {
                // expect only a single role per attribute
                for (int i = 0; i < roles.size(); i++) {
                    userRoles.add((String) roles.get(i));
                }
            } else {
                String rolesStr = (String) roles.get();
                for (String role : rolesStr.split(",")) {
                    if (!role.isEmpty()) {
                        userRoles.add(role);
                    }
                }
            }
            user.setRoles(userRoles.toArray(new String[userRoles.size()]));
        }

        return user;
    }
}
