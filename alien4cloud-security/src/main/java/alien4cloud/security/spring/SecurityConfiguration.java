package alien4cloud.security.spring;

import java.util.List;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.ManagementServerProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.social.security.SpringSocialConfigurer;

import alien4cloud.security.AuthorizationUtil;

import com.google.common.collect.Lists;

@Slf4j
@Configuration
@ConditionalOnProperty(value = "saml.enabled", havingValue = "false")
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Order(ManagementServerProperties.ACCESS_OVERRIDE_ORDER)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {
    @Resource
    private SecurityProperties security;
    @Resource
    private Alien4CloudAccessDeniedHandler accessDeniedHandler;
    @Resource
    private Alien4CloudAuthenticationProvider authenticationProvider;

    @Autowired
    private Environment env;

    @Bean
    public Alien4CloudAuthenticationProvider authenticationProvider() {
        return new Alien4CloudAuthenticationProvider();
    }

    @Bean
    @Profile("security-demo")
    public DaoAuthenticationProvider demoAuthenticationProvider() {
        log.warn("ALIEN 4 CLOUD is Running in DEMO mode. This includes demo users and MUST NOT BE USED in PRODUCTION");
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        List<UserDetails> users = getUsers(
                new String[] { "user", "componentManager", "componentBrowser", "applicationManager", "appManager", "admin", "architect" },
                new String[] { "COMPONENTS_BROWSER", "COMPONENTS_BROWSER, COMPONENTS_MANAGER", "COMPONENTS_BROWSER",
                        "APPLICATIONS_MANAGER, COMPONENTS_BROWSER, COMPONENTS_MANAGER", "APPLICATIONS_MANAGER, COMPONENTS_BROWSER, COMPONENTS_MANAGER", "ADMIN",
                        "ARCHITECT, COMPONENTS_BROWSER" });
        InMemoryUserDetailsManager detailsManager = new InMemoryUserDetailsManager(users);
        provider.setUserDetailsService(detailsManager);
        return provider;
    }

    public List<UserDetails> getUsers(String[] usernames, String[] roles) {
        List<UserDetails> users = Lists.newArrayList();
        for (int i = 0; i < usernames.length; i++) {
            List<GrantedAuthority> authorities = Lists.newArrayList();
            for (String role : roles[i].split(",")) {
                authorities.add(new SimpleGrantedAuthority(role.trim()));
            }
            users.add(new org.springframework.security.core.userdetails.User(usernames[i], usernames[i], authorities));
        }
        return users;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(authenticationProvider);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        // configure the HttpSecurity
        AuthorizationUtil.configure(http);

        if (env.acceptsProfiles("github-auth")) {
            log.info("GitHub profile is active - enabling Spring Social features");
            http.apply(new SpringSocialConfigurer().postLoginUrl("/").alwaysUsePostLoginUrl(true));
        }
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        log.debug("Configure ignore path");
        web.ignoring().antMatchers("/api-doc/**", "/api-docs/**", "/data/**", "/bower_components/**", "/images/**", "/js-lib/**", "/scripts/**", "/styles/**",
                "/views/**", "/rest/admin/health");
    }

}
