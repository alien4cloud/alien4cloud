package alien4cloud.security.spring;

import javax.annotation.Resource;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.social.UserIdSource;
import org.springframework.social.config.annotation.ConnectionFactoryConfigurer;
import org.springframework.social.config.annotation.EnableSocial;
import org.springframework.social.config.annotation.SocialConfigurer;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.UsersConnectionRepository;
import org.springframework.social.github.connect.GitHubConnectionFactory;
import org.springframework.social.security.AuthenticationNameUserIdSource;

import alien4cloud.security.users.IAlienUserDao;

@Configuration
@EnableSocial
@Profile("github-auth")
public class SocialConfig implements SocialConfigurer {
    @Resource
    private IAlienUserDao alienUserDao;

    @Override
    public UserIdSource getUserIdSource() {
        return new AuthenticationNameUserIdSource();
    }

    @Override
    public void addConnectionFactories(ConnectionFactoryConfigurer connectionFactoryConfigurer, Environment environment) {
        connectionFactoryConfigurer.addConnectionFactory(new GitHubConnectionFactory("ff20f678550f291c177e", "9361d219c221976bba3451080985affb54d685e8"));
    }

    @Override
    public UsersConnectionRepository getUsersConnectionRepository(ConnectionFactoryLocator connectionFactoryLocator) {
        return new AlienUserConnectionRepository(alienUserDao);
    }
}
