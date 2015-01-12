package alien4cloud.ldap;

import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import alien4cloud.security.IAlienUserDao;
import alien4cloud.security.Role;
import alien4cloud.security.User;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Provider responsible to authenticate agains LDAP.
 * Note: we don't use the native Spring security (3.x) provider it is not compatible with spring ldap 2.x
 *
 * @author mourouvi
 */
@Slf4j
@Conditional(LdapCondition.class)
@Component("ldap-provider")
public class LdapAuthenticationProvider implements AuthenticationProvider {

    @Resource
    private LdapUserDao ldapUserDao;

    @Resource
    private IAlienUserDao alienUserDao;

    @Value("${ldap.mapping.roles.defaults}")
    private String[] defaultRoles;
    @Value("${ldap.mapping.roles.mapping:}")
    private String[] roleMappings;
    private Map<String, String> parsedRoleMappings;

    @PostConstruct
    public void importLdapUsers() {
        // parse role mappings
        for (String roleMapping : roleMappings) {
            String[] mapping = roleMapping.split("=");
            if (mapping.length != 2) {
                throw new IllegalArgumentException(
                        "Check your alien configuration, every entry in ldap.roles.mapping must be matching the <LDAP_ROLE>=<ALIEN_ROLE> expression");
            }
            // check that the alien role is indeed an alien role.
            Role.valueOf(mapping[1]);
            if (parsedRoleMappings == null) {
                parsedRoleMappings = Maps.newHashMap();
            }
            parsedRoleMappings.put(mapping[0], mapping[1]);
        }

        if (ldapUserDao.getLdapTemplate().getContextSource() != null) {
            List<User> users = ldapUserDao.getUsers(true);
            checkRoles();
            for (User user : users) {
                // refresh roles based on ldap.
                User alienUser = alienUserDao.find(user.getUsername());
                if (alienUser == null) {
                    mapLdapRoles(user, user);
                    alienUserDao.save(user);
                } else {
                    mapLdapRoles(user, alienUser);
                    alienUserDao.save(alienUser);
                }
            }
        }
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        String login = authentication.getName();
        String password = authentication.getCredentials().toString();

        if (ldapUserDao.authenticate(login, password)) {
            List<? extends GrantedAuthority> emptyList = Lists.newArrayList();
            Authentication auth = new UsernamePasswordAuthenticationToken(login, password, emptyList);
            updateLdapUserRoles(login, auth);
            return auth;
        } else {
            log.debug("Wrong password for user <" + login + ">");
            throw new BadCredentialsException("Incorrect password for user <" + login + ">");
        }
    }

    private void updateLdapUserRoles(String login, Authentication auth) {
        if (auth.isAuthenticated() && parsedRoleMappings != null) {
            // refresh roles if loaded from mapping
            User ldapUser = ldapUserDao.getById(login);
            User user = alienUserDao.find(login);

            if (ldapUser != null) {
                mapLdapRoles(ldapUser, user);
            }
            alienUserDao.save(user);
        }
    }

    private void mapLdapRoles(User ldapUser, User user) {
        if(ldapUser.getRoles() == null) {
            user.setRoles(defaultRoles);
            return;
        }
        
        List<String> userRoles = Lists.newArrayList();
        for (String role : ldapUser.getRoles()) {
            String alienRole = parsedRoleMappings.get(role);
            if (alienRole != null) {
                userRoles.add(alienRole);
            }
        }
        user.setRoles(userRoles.toArray(new String[userRoles.size()]));

    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }

    private void checkRoles() {
        for (String role : defaultRoles) {
            // should throw an exception and fail if the role doesn't exists.
            Role.valueOf(role);
        }
    }
}
