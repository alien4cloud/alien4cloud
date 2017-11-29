package alien4cloud.plugin.mock;

import java.util.Arrays;
import java.util.Map;

import javax.annotation.Resource;

import org.alien4cloud.tosca.model.definitions.PropertyConstraint;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.constraints.GreaterOrEqualConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.PatternConstraint;
import org.alien4cloud.tosca.normative.types.ToscaTypes;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import alien4cloud.model.orchestrators.ArtifactSupport;
import alien4cloud.model.orchestrators.locations.LocationSupport;
import alien4cloud.orchestrators.plugin.IOrchestratorPluginFactory;

/**
 * Factory for Mock implementation of orchestrator instance.
 */
@Component("mock-orchestrator-factory")
public class MockOrchestratorFactory implements IOrchestratorPluginFactory<MockOrchestrator, ProviderConfig> {
    public static final String OPENSTACK = "OpenStack";
    public static final String AWS = "Amazon";

    @Resource
    private BeanFactory beanFactory;
    private final Map<String, PropertyDefinition> deploymentProperties = Maps.newHashMap();

    @Override
    public MockOrchestrator newInstance() {
        return beanFactory.getBean(MockOrchestrator.class);
    }

    @Override
    public void destroy(MockOrchestrator instance) {
        // nothing specific, the plugin will be garbaged collected when all references are lost.
    }

    @Override
    public ProviderConfig getDefaultConfiguration() {
        return new ProviderConfig();
    }

    @Override
    public Class<ProviderConfig> getConfigurationType() {
        return ProviderConfig.class;
    }

    @Override
    public LocationSupport getLocationSupport() {
        return new LocationSupport(true, new String[] { OPENSTACK, AWS });
    }

    @Override
    public ArtifactSupport getArtifactSupport() {
        // support all type of implementations artifacts
        return new ArtifactSupport(new String[] { "tosca.artifacts.Deployment.Image.Container.Docker", "tosca.artifacts.Implementation.Bash",
                "org.alien4cloud.artifacts.BatchScript", "alien.artifacts.BatchScript", "tosca.artifacts.Implementation.Python",
                "org.alien4cloud.artifacts.AnsiblePlaybook" });
    }

    @Override
    public Map<String, PropertyDefinition> getDeploymentPropertyDefinitions() {

        // Field 1 : managerUrl as string
        PropertyDefinition managerUrl = new PropertyDefinition();
        managerUrl.setType(ToscaTypes.STRING.toString());
        managerUrl.setRequired(true);
        managerUrl.setDescription("PaaS manager URL");
        managerUrl.setConstraints(null);
        PatternConstraint manageUrlConstraint = new PatternConstraint();
        manageUrlConstraint.setPattern("http://.+");
        managerUrl.setConstraints(Arrays.asList((PropertyConstraint) manageUrlConstraint));

        // Field 2 : number backup with constraint
        PropertyDefinition numberBackup = new PropertyDefinition();
        numberBackup.setType(ToscaTypes.INTEGER.toString());
        numberBackup.setRequired(true);
        numberBackup.setDescription("Number of backup");
        numberBackup.setConstraints(null);
        GreaterOrEqualConstraint greaterOrEqualConstraint = new GreaterOrEqualConstraint();
        greaterOrEqualConstraint.setGreaterOrEqual(String.valueOf("1"));
        numberBackup.setConstraints(Lists.newArrayList((PropertyConstraint) greaterOrEqualConstraint));

        // Field 3 : email manager
        PropertyDefinition managerEmail = new PropertyDefinition();
        managerEmail.setType(ToscaTypes.STRING.toString());
        managerEmail.setRequired(true);
        managerEmail.setDescription("PaaS manager email");
        managerEmail.setConstraints(null);
        PatternConstraint managerEmailConstraint = new PatternConstraint();
        managerEmailConstraint.setPattern(".+@.+");
        managerEmail.setConstraints(Arrays.asList((PropertyConstraint) managerEmailConstraint));

        deploymentProperties.put("managementUrl", managerUrl);
        deploymentProperties.put("numberBackup", numberBackup);
        deploymentProperties.put("managerEmail", managerEmail);

        return deploymentProperties;
    }

    @Override
    public String getType() {
        return "Mock Orchestrator";
    }

}