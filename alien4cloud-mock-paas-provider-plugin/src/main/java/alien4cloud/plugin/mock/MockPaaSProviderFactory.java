package alien4cloud.plugin.mock;

import javax.annotation.Resource;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.stereotype.Component;

import alien4cloud.paas.IPaaSProvider;
import alien4cloud.paas.IPaaSProviderFactory;

@Component("mock-paas-provider")
public class MockPaaSProviderFactory implements IPaaSProviderFactory {
    @Resource
    private BeanFactory beanFactory;

    @Override
    public IPaaSProvider newInstance() {
        return beanFactory.getBean(MockPaaSProvider.class);
    }
}