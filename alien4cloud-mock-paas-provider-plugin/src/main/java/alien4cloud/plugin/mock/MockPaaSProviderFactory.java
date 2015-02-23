package alien4cloud.plugin.mock;

import javax.annotation.Resource;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.stereotype.Component;

import alien4cloud.paas.IConfigurablePaaSProvider;
import alien4cloud.paas.IConfigurablePaaSProviderFactory;

@Component("mock-paas-provider")
public class MockPaaSProviderFactory implements IConfigurablePaaSProviderFactory<ProviderConfig> {
    @Resource
    private BeanFactory beanFactory;

    @Override
    public ProviderConfig getDefaultConfiguration() {
        return new ProviderConfig();
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