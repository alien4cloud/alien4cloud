package alien4cloud.it;

import org.junit.Ignore;
import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(features = {
        //
        "classpath:alien/rest/security"
        // "classpath:alien/rest/security/authenticate.feature"
        // "classpath:alien/rest/security/create_update_delete_user.feature"
        // "classpath:alien/rest/security/get_account.feature"
        // "classpath:alien/rest/security/group_roles.feature"
        // "classpath:alien/rest/security/groupRoles_on_applications.feature"
        // "classpath:alien/rest/security/search_application_with_roles.feature"
        // "classpath:alien/rest/security/user_roles.feature"
        // "classpath:alien/rest/security/userRoles_on_applications.feature"
        // "classpath:alien/rest/security/userRoles_on_cloud.feature"
}, format = { "pretty", "html:target/cucumber/security", "json:target/cucumber/cucumber-security.json" })
// @Ignore
public class RunSecurityIT {
}
