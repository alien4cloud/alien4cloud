package alien4cloud.rest.wizard;

import alien4cloud.security.spring.ldap.LdapCondition;
import alien4cloud.utils.AlienYamlPropertiesFactoryBeanFactory;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "wizard", ignoreInvalidFields = true, ignoreUnknownFields = true)
public class ApplicationWizardConfiguration {

    private Set<String> applicationOverviewMetapropertiesSet;
    private Set<String> componentOverviewMetapropertiesSet;

    // key: categorie, value: { key: metaproperty name, values : accepted values)
    private Map<String, Map<String, Set<String>>> componentFilterByMetapropertyValuesSet;

    private Map<String, Set<String>> allComponentFiltersSet = Maps.newHashMap();

    /**
     * The list of metaproperty names that should be returned for applications. No filter if empty.
     */
    @Setter
    @Getter
    private String[] applicationOverviewMetaproperties;

    /**
     * The list of metaproperty names that should be returned for components. No filter if empty.
     */
    @Setter
    @Getter
    private String[] componentOverviewMetaproperties;

    /**
     * The list categories where to put components.
     */
    @Setter
    @Getter
    private String[] componentCategories;

    /**
     * In order to filter the nodes that are are returned as application / topology modules, a map where the key is the property name,
     * the value the property value (or coma separated values). A OR operator is used for values, a AND operator is used for meta property names.
     */
    @Setter
    @Getter
    private Map<String, Map<String, String>> componentFilterByMetapropertyValues;


    @PostConstruct
    void init() {
        if (componentCategories == null) {
            // by default, we'll display all nodes, with no filter
            componentCategories = new String[] {"Nodes"};
            componentFilterByMetapropertyValues = Maps.newHashMap();
            componentFilterByMetapropertyValues.put("Nodes", Maps.newHashMap());
        }

        // init the componentFilterByMetapropertyValuesSet
        componentFilterByMetapropertyValuesSet = Maps.newHashMap();
        Set<String> catagories = Sets.newHashSet(componentCategories);
        componentFilterByMetapropertyValues.forEach((key, value) -> {
            if (catagories.contains(key)) {
                Map<String, String> categoryComponentFilterByMetapropertyValues = value;
                Map<String, Set<String>> categoryComponentFilterByMetapropertyValuesSet = Maps.newHashMap();
                componentFilterByMetapropertyValuesSet.put(key, categoryComponentFilterByMetapropertyValuesSet);
                if (categoryComponentFilterByMetapropertyValues.size() > 0) {
                    categoryComponentFilterByMetapropertyValues.forEach((k, v) -> {
                        String[] values = v.split(",");
                        categoryComponentFilterByMetapropertyValuesSet.put(k, Sets.newHashSet(values));
                        Set<String> metaprops = allComponentFiltersSet.get(k);
                        if (metaprops == null) {
                            metaprops = Sets.newHashSet();
                            allComponentFiltersSet.put(k, metaprops);
                        }
                        for (int i=0; i<values.length; i++) {
                            metaprops.add(values[i].toLowerCase());
                        }
                    });
                }
            } else {
                // TODO log warn
            }
        });

        if (applicationOverviewMetaproperties != null) {
            applicationOverviewMetapropertiesSet = Sets.newHashSet(applicationOverviewMetaproperties);
        } else {
            applicationOverviewMetapropertiesSet = Sets.newHashSet();
        }

        if (componentOverviewMetaproperties != null) {
            componentOverviewMetapropertiesSet = Sets.newHashSet(componentOverviewMetaproperties);
        } else {
            componentOverviewMetapropertiesSet = Sets.newHashSet();
        }

    }

    public Set<String> getApplicationOverviewMetapropertiesSet() {
        return applicationOverviewMetapropertiesSet;
    }

    public Set<String> getComponentOverviewMetapropertiesSet() {
        return componentOverviewMetapropertiesSet;
    }

    public Map<String, Map<String, Set<String>>> getComponentFilterByCategorySet() {
        return componentFilterByMetapropertyValuesSet;
    }

    public Map<String, Set<String>> getAllComponentFiltersSet() {
        return allComponentFiltersSet;
    }

}
