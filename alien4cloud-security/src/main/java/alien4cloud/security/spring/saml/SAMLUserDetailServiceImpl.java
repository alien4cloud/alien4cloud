package alien4cloud.security.spring.saml;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.userdetails.SAMLUserDetailsService;
import org.springframework.stereotype.Component;

import alien4cloud.security.model.User;
import alien4cloud.security.users.IAlienUserDao;

@Slf4j
@Component
@ConditionalOnProperty(value = "saml.enabled", havingValue = "true")
public class SAMLUserDetailServiceImpl implements SAMLUserDetailsService {
    @Resource
    private IAlienUserDao userDao;

    @Override
    public Object loadUserBySAML(SAMLCredential samlCredential) throws UsernameNotFoundException {
        String userId = samlCredential.getNameID().getValue();

        User user = userDao.find(userId);
        log.debug("User <{}> has been retrieved from SAML authentication.", user);
        if (user == null) {
            // create a user
            user = new User();
            user.setUsername(userId);
            user.setInternalDirectory(false);
            userDao.save(user);
        }
        return user;
    }
}
