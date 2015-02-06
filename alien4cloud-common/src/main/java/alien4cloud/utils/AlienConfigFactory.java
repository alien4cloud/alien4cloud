package alien4cloud.utils;

import javax.annotation.Resource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean
;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Non static configuration factory.
 */
public class AlienConfigFactory implements ApplicationContextAware {

    @Resource
    private ApplicationContext applicationContext;

    public YamlPropertiesFactoryBean get() {
        return AlienYamlPropertiesFactoryBeanFactory.get(applicationContext);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}