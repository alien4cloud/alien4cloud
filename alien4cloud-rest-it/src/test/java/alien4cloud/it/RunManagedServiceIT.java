package alien4cloud.it;

import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(features = {
        //
        "classpath:alien/rest/managed-service",
        // "classpath:alien/rest/managed-service/create_service.feature",
        // "classpath:alien/rest/managed-service/create_service_continued.feature",
        // "classpath:alien/rest/managed-service/delete_service.feature",
        // "classpath:alien/rest/managed-service/get_service.feature",
        // "classpath:alien/rest/managed-service/list_service.feature",
        // "classpath:alien/rest/managed-service/search_service.feature",
        // "classpath:alien/rest/managed-service/update_service.feature",
        // "classpath:alien/rest/managed-service/patch_service.feature",
        //
}, format = { "pretty", "html:target/cucumber/managed-service", "json:target/cucumber/cucumber-service.json" })
// @Ignore
public class RunManagedServiceIT {
}
