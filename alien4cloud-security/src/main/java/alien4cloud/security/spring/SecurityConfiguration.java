package alien4cloud.security.spring;

import java.util.List;

import javax.annotation.Resource;

import alien4cloud.security.spring.Alien4CloudAccessDeniedHandler;
import alien4cloud.security.spring.Alien4CloudAuthenticationProvider;
import alien4cloud.security.spring.FailureAuthenticationEntryPoint;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.ManagementServerProperties;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import com.google.common.collect.Lists;

@Slf4j
@Configuration
@Order(ManagementServerProperties.ACCESS_OVERRIDE_ORDER)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Autowired
    private SecurityProperties security;

    @Resource
    private Alien4CloudAccessDeniedHandler accessDeniedHandler;
    @Resource
    private Alien4CloudAuthenticationProvider authenticationProvider;

    @Bean
    public Alien4CloudAuthenticationProvider authenticationProvider() {
        return new Alien4CloudAuthenticationProvider();
    }

    @Bean
    @Profile("security-demo")
    public DaoAuthenticationProvider demoAuthenticationProvider() {
        log.warn("ALIEN 4 CLOUD is Running in DEMO mode. This includes demo users and MUST NOT BE USED in PRODUCTION");
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        List<UserDetails> users = getUsers(new String[] { "user", "componentManager", "componentBrowser", "applicationManager", "appManager", "admin",
                "architect" }, new String[] { "COMPONENTS_BROWSER", "COMPONENTS_BROWSER, COMPONENTS_MANAGER", "COMPONENTS_BROWSER",
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
        http.exceptionHandling().accessDeniedHandler(accessDeniedHandler);

        http.authorizeRequests().antMatchers("/*").permitAll();
        http.authorizeRequests().antMatchers("/rest/auth/**", "/rest/modules").permitAll();
        http.authorizeRequests().antMatchers("/rest/quicksearch/**", "/rest/users/search/**", "/rest/users/getUsers/**").authenticated();
        http.authorizeRequests().antMatchers("/rest/users/**").hasAuthority("ADMIN");
        http.authorizeRequests().antMatchers("/rest/groups/search/**", "/rest/groups/getGroups/**").authenticated();
        http.authorizeRequests().antMatchers("/rest/groups/**").hasAuthority("ADMIN");
        http.authorizeRequests().antMatchers("/rest/topologies/**").authenticated();
        http.authorizeRequests().antMatchers("/rest/templates/**").hasAnyAuthority("ADMIN", "ARCHITECT", "APPLICATIONS_MANAGER");
        http.authorizeRequests().antMatchers("/rest/components/**").hasAnyAuthority("ADMIN", "COMPONENTS_MANAGER", "COMPONENTS_BROWSER");
        http.authorizeRequests().antMatchers("/csarrepository/**").hasAnyAuthority("ADMIN", "COMPONENTS_MANAGER", "COMPONENTS_BROWSER");
        http.authorizeRequests().antMatchers("/rest/applications/**").authenticated();
        http.authorizeRequests().antMatchers("/rest/runtime/**").authenticated();
        http.authorizeRequests().antMatchers("/rest/suggest/**").authenticated();
        http.authorizeRequests().antMatchers("/rest/suggestions/toscaelement/**").hasAnyAuthority("ADMIN", "COMPONENTS_MANAGER", "COMPONENTS_BROWSER");
        http.authorizeRequests().antMatchers(HttpMethod.POST, "/rest/csars").hasAnyAuthority("ADMIN", "COMPONENTS_MANAGER", "ARCHITECT");
        http.authorizeRequests().antMatchers("/rest/csars/**").hasAnyAuthority("ADMIN", "COMPONENTS_MANAGER");
        http.authorizeRequests().antMatchers("/rest/plugin/**").hasAuthority("ADMIN");
        http.authorizeRequests().antMatchers("/rest/tagconfigurations/search/**").authenticated();
        http.authorizeRequests().antMatchers("/rest/tagconfigurations/**").hasAuthority("ADMIN");

        http.authorizeRequests().antMatchers("/rest/formdescriptor/nodetype/**").hasAnyAuthority("ADMIN", "COMPONENTS_MANAGER", "COMPONENTS_BROWSER");
        http.authorizeRequests().antMatchers("/rest/formdescriptor/pluginConfig/**").hasAuthority("ADMIN");
        http.authorizeRequests().antMatchers("/rest/formdescriptor/tagconfiguration/**").hasAuthority("ADMIN");
        http.authorizeRequests().antMatchers("/rest/formdescriptor/cloudConfig/**").hasAuthority("ADMIN");

        http.authorizeRequests().antMatchers("/rest/properties/**").hasAnyAuthority("ADMIN", "APPLICATIONS_MANAGER");
        http.authorizeRequests().antMatchers("/rest/enums/**").authenticated();

        http.authorizeRequests().antMatchers("/rest/deployments/**").authenticated();
        http.authorizeRequests().antMatchers("/rest/clouds/search/**").authenticated();
        http.authorizeRequests().antMatchers(HttpMethod.GET, "/rest/clouds/{id}").authenticated();
        http.authorizeRequests().antMatchers("/rest/clouds/*/deploymentpropertydefinitions").authenticated();
        http.authorizeRequests().antMatchers("/rest/clouds/**").hasAuthority("ADMIN");
        http.authorizeRequests().antMatchers("/rest/cloud-images/**").hasAuthority("ADMIN");
        http.authorizeRequests().antMatchers("/rest/alienEndPoint/**").authenticated();
        http.authorizeRequests().antMatchers("/rest/passprovider").hasAuthority("ADMIN");
        http.authorizeRequests().antMatchers("/rest/admin/**").hasAuthority("ADMIN");
        http.authorizeRequests().antMatchers("/rest/audit/**").hasAuthority("ADMIN");
        http.authorizeRequests().anyRequest().denyAll();

        http.formLogin().defaultSuccessUrl("/rest/auth/status").failureUrl("/rest/auth/authenticationfailed").loginProcessingUrl("/login")
                .usernameParameter("username").passwordParameter("password").permitAll().and().logout().logoutSuccessUrl("/").deleteCookies("JSESSIONID");
        http.csrf().disable();

        // handle non authenticated request
        http.exceptionHandling().authenticationEntryPoint(new FailureAuthenticationEntryPoint());
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        log.debug("Configure ignore path");
        web.ignoring().antMatchers("/api-docs/**", "/data/**", "/bower_components/**", "/images/**", "/scripts/**", "/styles/**", "/views/**");
    }
}
