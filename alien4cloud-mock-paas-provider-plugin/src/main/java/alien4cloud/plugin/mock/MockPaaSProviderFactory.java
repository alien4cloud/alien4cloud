package alien4cloud.plugin.mock;

import java.util.Arrays;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.stereotype.Component;

import alien4cloud.model.components.PropertyConstraint;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.components.constraints.GreaterOrEqualConstraint;
import alien4cloud.model.components.constraints.PatternConstraint;
import alien4cloud.paas.IConfigurablePaaSProvider;
import alien4cloud.paas.IConfigurablePaaSProviderFactory;
import alien4cloud.paas.IDeploymentParameterizablePaaSProviderFactory;
import alien4cloud.tosca.normative.ToscaType;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Component("mock-paas-provider")
public class MockPaaSProviderFactory implements IConfigurablePaaSProviderFactory<ProviderConfig>,
        IDeploymentParameterizablePaaSProviderFactory<IConfigurablePaaSProvider<ProviderConfig>> {

    private final Map<String, PropertyDefinition> deploymentProperties = Maps.newHashMap();

    @Resource
    private BeanFactory beanFactory;

    @Override
    public ProviderConfig getDefaultConfiguration() {
        return null;
    }

    @Override
    public Map<String, PropertyDefinition> getDeploymentPropertyDefinitions() {

        // Field 1 : managerUrl as string
        PropertyDefinition managerUrl = new PropertyDefinition();
        managerUrl.setType(ToscaType.STRING.toString());
        managerUrl.setRequired(true);
        managerUrl.setDescription("PaaS manager URL");
        managerUrl.setConstraints(null);
        PatternConstraint manageUrlConstraint = new PatternConstraint();
        manageUrlConstraint.setPattern("http://.+");
        managerUrl.setConstraints(Arrays.asList((PropertyConstraint) manageUrlConstraint));

        // Field 2 : number backup with constraint
        PropertyDefinition numberBackup = new PropertyDefinition();
        numberBackup.setType(ToscaType.INTEGER.toString());
        numberBackup.setRequired(true);
        numberBackup.setDescription("Number of backup");
        numberBackup.setConstraints(null);
        GreaterOrEqualConstraint greaterOrEqualConstraint = new GreaterOrEqualConstraint();
        greaterOrEqualConstraint.setGreaterOrEqual(String.valueOf("1"));
        numberBackup.setConstraints(Lists.newArrayList((PropertyConstraint) greaterOrEqualConstraint));

        // Field 3 : email manager
        PropertyDefinition managerEmail = new PropertyDefinition();
        managerEmail.setType(ToscaType.STRING.toString());
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
    public IConfigurablePaaSProvider newInstance() {
        return beanFactory.getBean(MockPaaSProvider.class);
    }

    @Override
    public Class<ProviderConfig> getConfigurationType() {
        return ProviderConfig.class;
    }

    @Override
    public void destroy(IConfigurablePaaSProvider instance) {
        // Do nothing
    }
}