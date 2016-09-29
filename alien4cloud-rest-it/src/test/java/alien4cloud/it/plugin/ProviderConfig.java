package alien4cloud.it.plugin;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.model.common.Tag;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import alien4cloud.tosca.normative.ToscaType;
import alien4cloud.ui.form.annotation.FormProperties;
import alien4cloud.ui.form.annotation.FormPropertyConstraint;
import alien4cloud.ui.form.annotation.FormPropertyDefinition;

@Getter
@Setter
@NoArgsConstructor
@FormProperties({ "firstArgument", "secondArgument", "thirdArgument", "withBadConfiguraton", "tags", "properties", "javaVersion", "provideResourceIds",
        "resourceIdsCount", "shuffleStateChange" })
public class ProviderConfig {

    private String firstArgument;

    private String secondArgument;

    private String thirdArgument;

    private boolean withBadConfiguraton;

    private List<Tag> tags;

    private Map<String, PropertyDefinition> properties;

    @FormPropertyDefinition(type = ToscaType.VERSION, defaultValue = "1.6", constraints = @FormPropertyConstraint(greaterOrEqual = "1.6"))
    private String javaVersion;

    private boolean provideResourceIds;

    private int resourceIdsCount;

    private boolean shuffleStateChange;
}
