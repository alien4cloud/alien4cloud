package alien4cloud.it;

import org.junit.Ignore;
import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(features = {
        //
        "classpath:alien/rest/application-deployment/"
        // "classpath:alien/rest/application-deployment/policies/policy_matching.feature"
        // "classpath:alien/rest/application-deployment/service_matching.feature"
        // "classpath:alien/rest/application-deployment/deploy_application.feature"
        // "classpath:alien/rest/application-deployment/deploy_application_with_events.feature"
        // "classpath:alien/rest/application-deployment/deployments.feature"
        // "classpath:alien/rest/application-deployment/inputs_orchestrators_properties_settings.feature"
        // "classpath:alien/rest/application-deployment/location_matching.feature"
        // "classpath:alien/rest/application-deployment/location_policies_setting.feature"
        // "classpath:alien/rest/application-deployment/node_substitution.feature"
        // "classpath:alien/rest/application-deployment/undeploy_application.feature"
        //
}, format = { "pretty", "html:target/cucumber/application_deployment", "json:target/cucumber/cucumber-application-deployment.json" })
// @Ignore
public class RunApplicationDeploymentIT {
}
