package alien4cloud.ldap;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.common.collect.Lists;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import alien4cloud.security.IAlienUserDao;
import alien4cloud.security.Role;
import alien4cloud.security.User;

/**
 * Provider responsible to authenticate agains LDAP.
 * Note: we don't use the native Spring security (3.x) provider it is not compatible with spring ldap 2.x
 *
 * @author mourouvi
 */
@Slf4j
//@Profile("security-ldap")
@Conditional(LdapCondition.class)
@Component("ldap-provider")
public class LdapAuthenticationProvider implements AuthenticationProvider {

    @Resource
    private LdapUserDao ldapUserDao;

    @Resource
    private IAlienUserDao alienUserDao;

    @Value("${ldap.defaultRoles}")
    private String defaultRoles;

    @PostConstruct
    public void importLdapUsers() {
        if (ldapUserDao.getLdapTemplate().getContextSource() != null) {
            List<User> users = ldapUserDao.getUsers(true);
            String[] defaultUserRoles = getRoles();
            for (User user : users) {
                if (alienUserDao.find(user.getUsername()) == null) {
                    user.setRoles(defaultUserRoles);
                    alienUserDao.save(user);
                }
            }
        }
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        String login = authentication.getName();
        String password = authentication.getCredentials().toString();

        if (ldapUserDao.authenticate(login, password)) {
            return new UsernamePasswordAuthenticationToken(login, password, Lists.newArrayList(new SimpleGrantedAuthority(getRoles().toString())));
        } else {
            log.debug("Wrong password for user <" + login + ">");
            throw new BadCredentialsException("Incorrect password for user <" + login + ">");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }

    private String[] getRoles() {
        List<String> roles = new ArrayList<String>();
        for (String role : defaultRoles.split(",")) {
            if (Role.valueOf(role) != null) {
                roles.add(Role.valueOf(role).toString());
            }
        }
        return roles.toArray(new String[roles.size()]);
    }
}
