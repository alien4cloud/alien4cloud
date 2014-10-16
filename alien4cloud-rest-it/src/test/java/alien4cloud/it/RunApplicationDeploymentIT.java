package alien4cloud.it;

import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(features = {
        "classpath:alien/rest/application-deployment/"
        // "classpath:alien/rest/application-deployment/deploy_application_with_events.feature"
        // "classpath:alien/rest/application-deployment/deploy_application.feature"
        // "classpath:alien/rest/application-deployment/deploy_with_deployment_properties.feature"
        // "classpath:alien/rest/application-deployment/deployments.feature"
        // "classpath:alien/rest/application-deployment/undeploy_application.feature"
//        "classpath:alien/rest/application-deployment/compute_template_matcher.feature"
}, format = { "pretty", "html:target/cucumber/application_deployment", "json:target/cucumber/cucumber-application-deployment.json" })
public class RunApplicationDeploymentIT {
}
