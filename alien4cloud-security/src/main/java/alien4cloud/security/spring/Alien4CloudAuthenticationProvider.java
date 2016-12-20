package alien4cloud.security.spring;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.security.authentication.AnonymousAuthenticationProvider;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCrypt;

import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.User;
import alien4cloud.security.users.IAlienUserDao;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Alien4CloudAuthenticationProvider implements AuthenticationProvider {
    @Resource
    private IAlienUserDao alienUserDao;
    protected AuthenticationProvider wrappedProvider = null;

    @Resource
    private ListableBeanFactory beanFactory;

    /**
     * Configure the bean from the list of {@link AuthenticationProvider} defined in the spring context.
     */
    @PostConstruct
    public void configure() {
        Map<String, AuthenticationProvider> providers = beanFactory.getBeansOfType(AuthenticationProvider.class);
        for (Entry<String, AuthenticationProvider> provider : providers.entrySet()) {
            if (provider.getValue() != this && !(provider.getValue() instanceof AnonymousAuthenticationProvider)) {
                wrappedProvider = provider.getValue();
            }
        }
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        String login = authentication.getName();
        String password = authentication.getCredentials().toString();
        // get user from internal store
        User user;
        if (login == null || login.isEmpty()) {
            user = null;
        } else {
            user = alienUserDao.find(login);
        }
        if (user == null) {
            return authenticateNewUser(authentication, password);
        }

        if (user.isInternalDirectory()) {
            return internalAuthentication(user, password);
        } else if (wrappedProvider == null) {
            log.error("The user <" + login + "> is not internal but no wrapped provider has been defined. Unable to authenticate.");
            return null;
        }
        return delegateAuthenticate(authentication, user, password);
    }

    private Authentication internalAuthentication(User user, String password) {
        if (BCrypt.checkpw(password, user.getPassword())) {
            return AuthorizationUtil.createAuthenticationToken(user, password);
        } else {
            log.debug("Wrong password for user <" + user.getUsername() + ">");
            throw new BadCredentialsException("Incorrect password for user <" + user.getUsername() + ">");
        }
    }

    private Authentication delegateAuthenticate(Authentication authentication, User user, String password) {
        Authentication auth = wrappedProvider.authenticate(authentication);
        // refresh the user in case the wrapped provider changed some roles
        User updatedUser = alienUserDao.find(user.getUsername());
        if (auth.isAuthenticated()) {
            return AuthorizationUtil.createAuthenticationToken(updatedUser, password);
        } else {
            return auth;
        }
    }

    private Authentication authenticateNewUser(Authentication authentication, String password) {
        if (wrappedProvider == null) {
            throw new UsernameNotFoundException("Cannot find user <" + authentication.getName() + ">");
        } else {
            Authentication providerAuth = wrappedProvider.authenticate(authentication);

            // create a user in local store
            User user;
            if (providerAuth.getPrincipal() != null && providerAuth.getPrincipal() instanceof User) {
                user = (User) providerAuth.getPrincipal();
            } else {
                user = new User();
            }
            user.setUsername(providerAuth.getName());
            userFromPrincipal(user, providerAuth.getPrincipal());
            setUserAuthorities(user, providerAuth.getAuthorities());

            alienUserDao.save(user);

            return AuthorizationUtil.createAuthenticationToken(user, password);
        }
    }

    /**
     * Set the list of roles from the authorities collection.
     *
     * @param user The user to populate.
     * @param authorities The collection of authorities.
     */
    private void setUserAuthorities(User user, Collection<? extends GrantedAuthority> authorities) {
        if (authorities != null && authorities.size() > 0) {
            String[] roles = new String[authorities.size()];
            int i = 0;
            for (GrantedAuthority authority : authorities) {
                roles[i] = authority.getAuthority();
                i++;
            }
            user.setRoles(roles);
        }
    }

    /**
     * Initialize user's fields from the principal object if it is an instance of {@link UserDetails}.
     *
     * @param user The user to populate.
     * @param principal The principal object from {@link Authentication}.
     */
    private void userFromPrincipal(User user, Object principal) {
        if (principal instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) principal;
            user.setAccountNonExpired(userDetails.isAccountNonExpired());
            user.setAccountNonLocked(userDetails.isAccountNonLocked());
            user.setCredentialsNonExpired(userDetails.isCredentialsNonExpired());
            user.setEnabled(userDetails.isEnabled());
        } else {
            log.info("Principal from configured provider is not a UserDetails, do not populate user.");
        }
    }

    public void setWrappedProvider(AuthenticationProvider wrappedProvider) {
        this.wrappedProvider = wrappedProvider;
    }

    public void setAlienUserDao(IAlienUserDao alienUserDao) {
        this.alienUserDao = alienUserDao;
    }

    @Override
    public boolean supports(Class<?> authenticationClass) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authenticationClass);
    }
}
