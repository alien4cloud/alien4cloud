package alien4cloud.security.spring.github;

import javax.annotation.Resource;

import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.social.security.SocialUserDetails;
import org.springframework.social.security.SocialUserDetailsService;
import org.springframework.stereotype.Component;

import alien4cloud.security.users.IAlienUserDao;

@Component
@Profile("github-auth")
public class SimpleSocialUsersDetailService implements SocialUserDetailsService {
    @Resource
    private IAlienUserDao userDao;

    @Override
    public SocialUserDetails loadUserByUserId(String userId) throws UsernameNotFoundException, DataAccessException {
        return userDao.find(userId);
    }
}