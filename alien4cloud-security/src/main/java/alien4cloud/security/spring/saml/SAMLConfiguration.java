package alien4cloud.security.spring.saml;

import javax.inject.Inject;

import alien4cloud.security.spring.Alien4CloudAuthenticationProvider;
import org.springframework.boot.actuate.autoconfigure.ManagementServerProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.saml.*;
import org.springframework.security.saml.context.SAMLContextProviderImpl;
import org.springframework.security.saml.log.SAMLDefaultLogger;
import org.springframework.security.saml.metadata.MetadataDisplayFilter;
import org.springframework.security.saml.metadata.MetadataGenerator;
import org.springframework.security.saml.metadata.MetadataGeneratorFilter;
import org.springframework.security.saml.websso.*;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import alien4cloud.security.spring.Alien4CloudAccessDeniedHandler;
import alien4cloud.security.spring.FailureAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConditionalOnProperty(value = "saml.enabled", havingValue = "true")
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Order(ManagementServerProperties.ACCESS_OVERRIDE_ORDER)
public class SAMLConfiguration extends WebSecurityConfigurerAdapter {
    @Inject
    private Alien4CloudAccessDeniedHandler accessDeniedHandler;
    @Inject
    private SAMLUserDetailServiceImpl samlUserDetailsServiceImpl;
    @Inject
    private MetadataGenerator metadataGenerator;

    // SAML Authentication Provider responsible for validating of received SAML messages
    @Bean
    public SAMLAuthenticationProvider samlAuthenticationProvider() {
        SAMLAuthenticationProvider samlAuthenticationProvider = new SAMLAuthenticationProvider();
        samlAuthenticationProvider.setUserDetails(samlUserDetailsServiceImpl);
        samlAuthenticationProvider.setForcePrincipalAsString(false);
        return samlAuthenticationProvider;
    }

    // Provider of default SAML Context
    @Bean
    public SAMLContextProviderImpl contextProvider() {
        return new SAMLContextProviderImpl();
    }

    // Initialization of OpenSAML library
    @Bean
    public static SAMLBootstrap sAMLBootstrap() {
        return new SAMLBootstrap();
    }

    // Logger for SAML messages and events
    @Bean
    public SAMLDefaultLogger samlLogger() {
        return new SAMLDefaultLogger();
    }

    // SAML 2.0 WebSSO Assertion Consumer
    @Bean
    public WebSSOProfileConsumer webSSOprofileConsumer() {
        return new WebSSOProfileConsumerImpl();
    }

    // SAML 2.0 Holder-of-Key WebSSO Assertion Consumer
    @Bean
    public WebSSOProfileConsumerHoKImpl hokWebSSOprofileConsumer() {
        return new WebSSOProfileConsumerHoKImpl();
    }

    // SAML 2.0 Web SSO profile
    @Bean
    public WebSSOProfile webSSOprofile() {
        return new WebSSOProfileImpl();
    }

    // SAML 2.0 Holder-of-Key Web SSO profile
    @Bean
    public WebSSOProfileConsumerHoKImpl hokWebSSOProfile() {
        return new WebSSOProfileConsumerHoKImpl();
    }

    // SAML 2.0 ECP profile
    @Bean
    public WebSSOProfileECPImpl ecpprofile() {
        return new WebSSOProfileECPImpl();
    }

    @Bean
    public SingleLogoutProfile logoutprofile() {
        return new SingleLogoutProfileImpl();
    }

    // Handler for successful logout
    @Bean
    public SimpleUrlLogoutSuccessHandler successLogoutHandler() {
        SimpleUrlLogoutSuccessHandler successLogoutHandler = new SimpleUrlLogoutSuccessHandler();
        successLogoutHandler.setDefaultTargetUrl("/");
        return successLogoutHandler;
    }

    // Logout handler terminating local session
    @Bean
    public SecurityContextLogoutHandler logoutHandler() {
        SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
        logoutHandler.setInvalidateHttpSession(true);
        logoutHandler.setClearAuthentication(true);
        return logoutHandler;
    }

    // Filter processing incoming logout messages
    // First argument determines URL user will be redirected to after successful
    // global logout
    @Bean
    public SAMLLogoutProcessingFilter samlLogoutProcessingFilter() {
        return new SAMLLogoutProcessingFilter(successLogoutHandler(), logoutHandler());
    }

    // Overrides default logout processing filter with the one processing SAML
    // messages
    @Bean
    public SAMLLogoutFilter samlLogoutFilter() {
        return new SAMLLogoutFilter(successLogoutHandler(), new LogoutHandler[] { logoutHandler() }, new LogoutHandler[] { logoutHandler() });
    }

    // Handler deciding where to redirect user after successful login
    @Bean
    public SavedRequestAwareAuthenticationSuccessHandler successRedirectHandler() {
        SavedRequestAwareAuthenticationSuccessHandler successRedirectHandler = new SavedRequestAwareAuthenticationSuccessHandler();
        successRedirectHandler.setDefaultTargetUrl("/");
        return successRedirectHandler;
    }

    // Handler deciding where to redirect user after failed login
    @Bean
    public SimpleUrlAuthenticationFailureHandler authenticationFailureHandler() {
        SimpleUrlAuthenticationFailureHandler failureHandler = new SimpleUrlAuthenticationFailureHandler();
        failureHandler.setUseForward(true);
        failureHandler.setDefaultFailureUrl("/");
        return failureHandler;
    }

    @Bean
    public SAMLWebSSOHoKProcessingFilter samlWebSSOHoKProcessingFilter() throws Exception {
        SAMLWebSSOHoKProcessingFilter samlWebSSOHoKProcessingFilter = new SAMLWebSSOHoKProcessingFilter();
        samlWebSSOHoKProcessingFilter.setAuthenticationSuccessHandler(successRedirectHandler());
        samlWebSSOHoKProcessingFilter.setAuthenticationManager(authenticationManager());
        samlWebSSOHoKProcessingFilter.setAuthenticationFailureHandler(authenticationFailureHandler());
        return samlWebSSOHoKProcessingFilter;
    }

    // Processing filter for WebSSO profile messages
    @Bean
    public SAMLProcessingFilter samlWebSSOProcessingFilter() throws Exception {
        SAMLProcessingFilter samlWebSSOProcessingFilter = new SAMLProcessingFilter();
        samlWebSSOProcessingFilter.setAuthenticationManager(authenticationManager());
        samlWebSSOProcessingFilter.setAuthenticationSuccessHandler(successRedirectHandler());
        samlWebSSOProcessingFilter.setAuthenticationFailureHandler(authenticationFailureHandler());
        return samlWebSSOProcessingFilter;
    }

    @Bean
    public MetadataGeneratorFilter metadataGeneratorFilter() {
        return new MetadataGeneratorFilter(metadataGenerator);
    }

    @Bean
    public WebSSOProfileOptions defaultWebSSOProfileOptions() {
        WebSSOProfileOptions webSSOProfileOptions = new WebSSOProfileOptions();
        webSSOProfileOptions.setIncludeScoping(false);
        return webSSOProfileOptions;
    }

    // Entry point to initialize authentication, default values taken from
    // properties file
    @Bean
    public SAMLEntryPoint samlEntryPoint() {
        SAMLEntryPoint samlEntryPoint = new SAMLEntryPoint();
        samlEntryPoint.setDefaultProfileOptions(defaultWebSSOProfileOptions());
        return samlEntryPoint;
    }

    // The filter is waiting for connections on URL suffixed with filterSuffix
    // and presents SP metadata there
    @Bean
    public MetadataDisplayFilter metadataDisplayFilter() {
        return new MetadataDisplayFilter();
    }

    /**
     * Define the security filter chain in order to support SSO Auth by using SAML 2.0
     *
     * @return Filter chain proxy
     * @throws Exception
     */
    @Bean
    public FilterChainProxy samlFilter() throws Exception {
        List<SecurityFilterChain> chains = new ArrayList<SecurityFilterChain>();
        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/login/**"), samlEntryPoint()));
        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/logout/**"), samlLogoutFilter()));
        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/metadata/**"), metadataDisplayFilter()));
        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/SSO/**"), samlWebSSOProcessingFilter()));
        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/SSOHoK/**"), samlWebSSOHoKProcessingFilter()));
        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/SingleLogout/**"), samlLogoutProcessingFilter()));
        return new FilterChainProxy(chains);
    }

    /**
     * Returns the authentication manager currently used by Spring.
     * It represents a bean definition with the aim allow wiring from
     * other classes performing the Inversion of Control (IoC).
     *
     * @throws Exception
     */
    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    /**
     * Defines the web based security configuration.
     *
     * @param http It allows configuring web based security for specific http requests.
     * @throws Exception
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests().antMatchers("/*").permitAll();
        http.authorizeRequests().antMatchers("/static/tosca/**").hasAnyAuthority("ADMIN", "COMPONENTS_MANAGER", "COMPONENTS_BROWSER");
        http.authorizeRequests().antMatchers("/rest/alienEndPoint/**").authenticated();
        http.authorizeRequests().antMatchers("/rest/admin/**").hasAuthority("ADMIN");
        http.authorizeRequests().antMatchers("/rest/audit/**").hasAuthority("ADMIN");

        http.exceptionHandling().accessDeniedHandler(accessDeniedHandler);
        http.exceptionHandling().authenticationEntryPoint(new FailureAuthenticationEntryPoint());

        http.logout().logoutSuccessUrl("/");

        http.httpBasic().authenticationEntryPoint(samlEntryPoint());

        http.formLogin().defaultSuccessUrl("/rest/auth/status").failureUrl("/rest/auth/authenticationfailed").loginProcessingUrl("/login")
                .usernameParameter("username").passwordParameter("password").permitAll().and().logout().logoutSuccessUrl("/").deleteCookies("JSESSIONID");

        http.csrf().disable();
        http.addFilterBefore(metadataGeneratorFilter(), ChannelProcessingFilter.class).addFilterAfter(samlFilter(), BasicAuthenticationFilter.class);
    }

    @Bean
    public Alien4CloudAuthenticationProvider alienAuthenticationProvider() {
        return new Alien4CloudAuthenticationProvider();
    }

    /**
     * Sets a custom authentication provider.
     *
     * @param auth SecurityBuilder used to create an AuthenticationManager.
     * @throws Exception
     */
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(alienAuthenticationProvider());
    }
}
