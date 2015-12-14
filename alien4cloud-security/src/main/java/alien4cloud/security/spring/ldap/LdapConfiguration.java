package alien4cloud.security.spring.ldap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;

@Conditional(LdapCondition.class)
@Configuration
public class LdapConfiguration {
    @Value("${ldap.anonymousReadOnly}")
    private boolean anonymousReadOnly;
    @Value("${ldap.url}")
    private String url;
    @Value("${ldap.base}")
    private String base;
    @Value("${ldap.userDn}")
    private String userDn;
    @Value("${ldap.password}")
    private String password;

    @Bean
    public LdapContextSource ldapContextSource() {
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setAnonymousReadOnly(anonymousReadOnly);
        contextSource.setUrl(url);
        contextSource.setBase(base);
        contextSource.setUserDn(userDn);
        contextSource.setPassword(password);
        return contextSource;
    }

    @Bean
    public LdapTemplate ldapTemplate(LdapContextSource contextSource) {
        LdapTemplate ldapTemplate = new LdapTemplate(contextSource);
        return ldapTemplate;
    }
}