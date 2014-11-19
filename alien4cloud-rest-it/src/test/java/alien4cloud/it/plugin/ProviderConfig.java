package alien4cloud.it.plugin;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.component.model.Tag;
import alien4cloud.tosca.container.model.type.ToscaType;
import alien4cloud.tosca.model.PropertyDefinition;
import alien4cloud.ui.form.annotation.FormProperties;
import alien4cloud.ui.form.annotation.FormPropertyConstraint;
import alien4cloud.ui.form.annotation.FormPropertyDefinition;

@Getter
@Setter
@NoArgsConstructor
@FormProperties({ "firstArgument", "secondArgument", "thirdArgument", "withBadConfiguraton", "tags", "properties", "javaVersion" })
@SuppressWarnings("PMD.UnusedPrivateField")
public class ProviderConfig {

    private String firstArgument;

    private String secondArgument;

    private String thirdArgument;

    private boolean withBadConfiguraton;

    private List<Tag> tags;

    private Map<String, PropertyDefinition> properties;

    @FormPropertyDefinition(type = ToscaType.VERSION, defaultValue = "1.6", constraints = @FormPropertyConstraint(greaterOrEqual = "1.6"))
    private String javaVersion;
}
