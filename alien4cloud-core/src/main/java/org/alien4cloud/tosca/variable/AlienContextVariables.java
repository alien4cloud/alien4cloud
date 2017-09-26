package org.alien4cloud.tosca.variable;

import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.common.IMetaProperties;
import alien4cloud.model.common.Tag;
import alien4cloud.model.orchestrators.locations.Location;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.PropertySource;

import java.util.Map;
import java.util.function.Function;

/**
 * Hold all the predefined variables that are automatically available and not overridable.
 */
@Setter
public class AlienContextVariables extends PropertySource {

    private Application application;
    private ApplicationEnvironment applicationEnvironment;
    private Location location;

    public AlienContextVariables() {
        super("predefinedVariables");
    }

    @Override
    public Object getProperty(String name) {
        if (!name.startsWith("a4c.")) {
            return null;
        }

        switch (name) {
            case "a4c.application":
                return application;

            case "a4c.application.id":
                return ifNotNull(application, Application::getId);

            case "a4c.application.name":
                return ifNotNull(application, Application::getName);

            case "a4c.environment.type":
                return ifNotNull(applicationEnvironment, ApplicationEnvironment::getEnvironmentType);

            case "a4c.environment.name":
                return ifNotNull(applicationEnvironment, ApplicationEnvironment::getName);
        }

        // lookup for a tag
        if (name.startsWith("a4c.application.tags.")) {
            if (application != null && application.getTags() != null) {
                String tagName = StringUtils.removeStart(name, "a4c.application.tags.");

                for (Tag tag : application.getTags()) {
                    if (tag.getName().equals(tagName)) {
                        return tag.getValue();
                    }
                }
            }
        }

        // lookup for meta properties
        String metaName = StringUtils.removeStart(name, "a4c.");
        String metaValue = findMetaProperties(metaName, application);
        if (metaValue != null) {
            return metaValue;
        }
        metaValue = findMetaProperties(metaName, location);
        if (metaValue != null) {
            return metaValue;
        }

        return null;
    }

    private String findMetaProperties(String metaName, IMetaProperties metaProperties) {
        if (metaProperties != null && metaProperties.getMetaProperties() != null) {
            for (Map.Entry<String, String> meta : metaProperties.getMetaProperties().entrySet()) {
                if (meta.getKey().equals(metaName)) {
                    return meta.getValue();
                }
            }
        }
        return null;
    }

    private <T, R> R ifNotNull(T o, Function<T, R> getter) {
        if (o != null) {
            return getter.apply(o);
        } else {
            return null;
        }
    }

}
