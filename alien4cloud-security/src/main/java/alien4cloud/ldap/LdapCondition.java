package alien4cloud.ldap;

import org.springframework.boot.yaml.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import alien4cloud.utils.AlienYamlPropertiesFactoryBeanFactory;

/**
 * Condition to check if ldap should be enabled. It checks that the ldap.enabled property is actually true.
 */
public class LdapCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        YamlPropertiesFactoryBean propertiesFactoryBean = AlienYamlPropertiesFactoryBeanFactory.get(context.getResourceLoader());

        Object ldapEnabled = propertiesFactoryBean.getObject().get("ldap.enabled");
        if (ldapEnabled != null && ldapEnabled instanceof Boolean) {
            return ((Boolean) ldapEnabled).booleanValue();
        }
        return false;
    }
}