package alien4cloud.it;

import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(features = {
//
"classpath:alien/rest/csars"
// "classpath:alien/rest/csars/csar_crud.feature",
// "classpath:alien/rest/csars/csar_snapshot_test.feature"
// "classpath:alien/rest/csars/upload.feature"
// "classpath:alien/rest/csars/upload_topology.feature"
// "classpath:alien/rest/csars/upload_rights.feature"
//
}, format = { "pretty", "html:target/cucumber/csars", "json:target/cucumber/cucumber-csars.json" })
public class RunCsarIT {
}