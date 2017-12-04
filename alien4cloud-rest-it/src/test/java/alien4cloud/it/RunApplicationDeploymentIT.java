package alien4cloud.it;

import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(features = {
        //
        "classpath:alien/rest/application-deployment/"
//        "classpath:alien/rest/application-deployment/inputs/inputs_secrets_settings.feature"
//        "classpath:alien/rest/application-deployment/nodes_matching/node_substitution_update.feature"
//        "classpath:alien/rest/application-deployment/policies_matching/policy_substitution_update.feature"
        // "classpath:alien/rest/application-deployment/policies_matching/policy_matching.feature"
        // "classpath:alien/rest/application-deployment/service_matching.feature"
        // "classpath:alien/rest/application-deployment/deploy_application.feature"
        // "classpath:alien/rest/application-deployment/deploy_application_with_events.feature"
        // "classpath:alien/rest/application-deployment/deployments.feature"
        // "classpath:alien/rest/application-deployment/inputs/inputs_orchestrators_properties_settings.feature"
        // "classpath:alien/rest/application-deployment/locations/location_matching.feature"
        // "classpath:alien/rest/application-deployment/locations/location_policies_setting.feature"
        // "classpath:alien/rest/application-deployment/nodes_matching/node_substitution.feature"
        // "classpath:alien/rest/application-deployment/nodes_matching/node_substitution_update.feature"
        // "classpath:alien/rest/application-deployment/undeploy_application.feature"
        //
}, format = { "pretty", "html:target/cucumber/application_deployment", "json:target/cucumber/cucumber-application-deployment.json" })
// @Ignore
public class RunApplicationDeploymentIT {
}
